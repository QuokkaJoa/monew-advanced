package com.part2.monew.service.impl;

import com.part2.monew.annotation.Master;
import com.part2.monew.annotation.ReadOnly;
import com.part2.monew.dto.request.CommentRequest;
import com.part2.monew.dto.request.CreateCommentRequest;
import com.part2.monew.dto.response.CommentLikeResponse;
import com.part2.monew.dto.response.CommentResponse;
import com.part2.monew.dto.response.CursorResponse;
import com.part2.monew.entity.CommentLike;
import com.part2.monew.entity.CommentsManagement;
import com.part2.monew.entity.NewsArticle;
import com.part2.monew.entity.User;
import com.part2.monew.global.exception.article.ArticleNotFoundException;
import com.part2.monew.global.exception.comment.CommentIsActiveException;
import com.part2.monew.global.exception.comment.CommentLikeDuplication;
import com.part2.monew.global.exception.comment.CommentNotFoundException;
import com.part2.monew.global.exception.comment.CommentUnlikeDuplication;
import com.part2.monew.global.exception.user.UserNotFoundException;
import com.part2.monew.repository.CommentLikeRepository;
import com.part2.monew.repository.CommentRepository;
import com.part2.monew.repository.NewsArticleRepository;
import com.part2.monew.repository.UserRepository;
import com.part2.monew.service.CommentService;
import com.part2.monew.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final RedissonClient redisson;
    private final CacheManager cacheManager;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;
    private final NewsArticleRepository articleRepository;
    private final NotificationService notificationService;

    // CommentServiceImpl 맨 위에 추가
    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedisCacheManager redisCacheManager;

    @ReadOnly
    @Override
    public CursorResponse findCommentsByArticleId(CommentRequest req, UUID userId) {
        String cacheKey = req.getArticleId() + ":limit:" + req.getLimit();
        Cache cache = redisCacheManager.getCache("getComments");

        // 1. 초기 캐시 확인 (기존과 동일)
        CursorResponse cached = cache.get(cacheKey, CursorResponse.class);
        if (cached != null) {
            log.info("[Cache Hit] Key: {}", cacheKey);
            return cached;
        }

        // 2. 락과 토픽(Pub/Sub 채널) 이름 정의
        final String lockKey = "lock:comments:" + cacheKey;
        final String topicName = "channel:comments:" + cacheKey;

        RLock lock = redisson.getLock(lockKey);
        RTopic topic = redisson.getTopic(topicName); // <-- Pub/Sub을 위한 토픽 객체 추가

        boolean locked = false;
        try {
            locked = lock.tryLock(5, 10, TimeUnit.SECONDS);

            // 3. 락 획득 성공 시 로직
            if (locked) {
                log.info("[Lock Acquired] Key: {}", lockKey);

                // Double-Checked Locking (기존과 동일)
                cached = cache.get(cacheKey, CursorResponse.class);
                if (cached != null) {
                    log.info("[Double-Checked Lock Cache Hit] Key: {}", cacheKey);
                    return cached;
                }

                // DB 조회 및 캐시 저장 (기존과 동일)
                log.warn("[DB Access] Cache miss. Querying database for Key: {}", cacheKey);
                List<CommentsManagement> list = commentRepository.findCommentsByArticleId(
                    req.getArticleId(), req.getAfter(), req.getLimit(), userId);
                Long total = commentRepository.totalCount(req.getArticleId());
                CursorResponse fresh = CursorResponse.of(
                    list.stream().map(CommentResponse::of).collect(Collectors.toList()), total);
                cache.put(cacheKey, fresh);

                // [변경점 1] 작업 완료 후, 대기자들에게 메시지 발행
                topic.publish("CACHE_UPDATED");

                return fresh;
            }
            // 4. 락 획득 실패 시 로직 (가장 큰 변경점)
            else {
                log.warn("[Lock Failed] Waiting via Pub/Sub for Key: {}", lockKey);

                CountDownLatch latch = new CountDownLatch(1);
                // "캐시 업데이트 완료" 메시지가 오면 latch.countDown()을 실행할 리스너 등록
                var listenerId = topic.addListener(String.class, (channel, msg) -> latch.countDown());

                try {
                    // 최대 5초간 메시지가 오기를 효율적으로 대기
                    boolean awaitSuccessful = latch.await(5, TimeUnit.SECONDS);
                    // 대기 후에는 캐시를 다시 한번 조회하여 반환
                    if(awaitSuccessful) {
                        log.info("[Pub/Sub] Notified. Re-fetching cache for Key: {}", cacheKey);
                        return cache.get(cacheKey, CursorResponse.class);
                    } else {
                        // 타임아웃 발생 시
                        log.error("[Pub/Sub] Wait timed out for Key: {}", cacheKey);
                        // 이 경우 null을 반환하거나 예외를 던질 수 있습니다.
                        return null;
                    }
                } finally {
                    // 대기가 끝나면 항상 리스너를 정리
                    topic.removeListener(listenerId);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    @Override
    @Master
    @Transactional
    public CommentResponse create(CreateCommentRequest requeset) {
        printLogo("Comment Create");

        User user = userRepository.findById(requeset.getUserId())
                .orElseThrow(UserNotFoundException::new);


        NewsArticle article = articleRepository.findById(requeset.getArticleId())
                .orElseThrow(ArticleNotFoundException::new);

        CommentsManagement comment = CommentsManagement.create(user, article, requeset.getContent(), 0);

        CommentsManagement saveComment = commentRepository.saveAndFlush(comment);

        // 뉴스 기사 댓글 수 증가
        article.incrementCommentCount();
        articleRepository.save(article);

        return CommentResponse.of(saveComment);

    }

    @Override
    @Master
    @Transactional
    public CommentResponse update(UUID id, String content) {
        printLogo("Comment update");


        CommentsManagement commentsManagement = commentRepository.findById(id)
                .orElseThrow(CommentNotFoundException::new);

        commentsManagement.update(content);

        return CommentResponse.of(commentsManagement);
    }

    @Override
    @Master
    @Transactional
    public CommentLikeResponse likeComment(UUID id, UUID userId) {
        printLogo("Comment likeComment");


        Optional<CommentLike> existingLikeOpt = commentLikeRepository.findByCommentsManagement_IdAndUser_Id(id, userId);

        existingLikeOpt.ifPresent(cl -> {
            throw new CommentLikeDuplication();
        });


        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        CommentsManagement commentsManagement = commentRepository.findById(id)
                .orElseThrow(CommentNotFoundException::new);


        CommentLike commentLike = CommentLike.create(user, commentsManagement);

        CommentLike saveComment = commentLikeRepository.saveAndFlush(commentLike);

        int totalLike = commentTotalLike(commentsManagement);

        commentsManagement.updateTotalCount(totalLike);

        User commentOwner = commentsManagement.getUser();
        if (!commentOwner.getId().equals(user.getId())) {
            String content = (user.getNickname()+"님이 나의 댓글을 좋아합니다.");
            notificationService.createNotification(
                    commentOwner,
                    content,
                    "COMMENT",
                    commentsManagement.getId()
            );
        }

        return CommentLikeResponse.of(commentsManagement, saveComment);
    }

    @Override
    @Master
    @Transactional
    public void unlikeComment(UUID id, UUID userId) {
        printLogo("Comment unlikeComment");


        CommentLike commentLike = commentLikeRepository.findByCommentsManagement_IdAndUser_Id(id, userId)
                .orElseThrow(CommentUnlikeDuplication::new);

        CommentsManagement commentsManagement = commentRepository.findById(id)
                .orElseThrow(CommentNotFoundException::new);


        commentLikeRepository.deleteById(commentLike.getId());

        int totalLike = commentTotalLike(commentsManagement);

        commentsManagement.updateTotalCount(totalLike);
    }

    @Override
    @Master
    @Transactional
    public void deleteComment(UUID id) {
        printLogo("Comment deleteComment");


        CommentsManagement commentsManagement = commentRepository.findById(id)
                .orElseThrow(CommentNotFoundException::new);

        // 뉴스 기사 댓글 수 감소
        NewsArticle article = commentsManagement.getNewsArticle();
        article.decrementCommentCount();
        articleRepository.save(article);

        commentsManagement.delete();

    }

    @Override
    @Master
    @Transactional
    public void hardDeleteComment(UUID id) {
        printLogo("Comment hardDeleteComment");


        CommentsManagement commentsManagement = commentRepository.findById(id)
                .orElseThrow(CommentNotFoundException::new);

        if(commentsManagement.isActive()){
            throw new CommentIsActiveException();
        }

        commentRepository.deleteById(id);
    }

    private int commentTotalLike(CommentsManagement commentsManagement) {
        return commentLikeRepository.findAllByCommentsManagement(commentsManagement).size();
    }


    private void printLogo(String callName){
        try {
            log.info("[DB URL Check {}] : {} ", callName ,dataSource.getConnection().getMetaData().getURL());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
