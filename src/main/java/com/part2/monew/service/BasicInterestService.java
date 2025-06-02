package com.part2.monew.service;

import com.part2.monew.dto.request.InterestRegisterRequestDto;
import com.part2.monew.dto.response.InterestDto;
import com.part2.monew.entity.Interest;
import com.part2.monew.entity.InterestKeyword;
import com.part2.monew.entity.Keyword;
import com.part2.monew.global.exception.BusinessException;
import com.part2.monew.global.exception.ErrorCode;
import com.part2.monew.global.exception.SimilarInterestExistsException;
import com.part2.monew.mapper.InterestMapper;
import com.part2.monew.repository.InterestRepository;
import com.part2.monew.repository.KeywordRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BasicInterestService implements InterestService{
  private final InterestRepository interestRepository;
  private final KeywordRepository keywordRepository;
  private final InterestMapper interestMapper;
  private final JaroWinklerSimilarity jaroWinklerSimilarity = new JaroWinklerSimilarity();

  @Transactional
  @Override
  public InterestDto registerInterest(InterestRegisterRequestDto requestDto, UUID requestUserId) {
    String newInterestName = requestDto.name();
    if (interestRepository.existsByName(requestDto.name())) {
      throw new BusinessException(ErrorCode.SIMILAR_INTEREST_EXISTS,
          String.format("이미 존재하는 관심사 이름입니다: %s", newInterestName));
    }

    List<String> existingInterestNames = interestRepository.findAllNames();
    for (String existingName : existingInterestNames) {
      double similarityScore = jaroWinklerSimilarity.apply(newInterestName.toLowerCase(),
          existingName.toLowerCase());
      double similarityPercent = similarityScore * 100.0;

      if (similarityPercent >= 80.0) {
        throw new SimilarInterestExistsException(
            String.format("유사한 이름의 관심사 '%'가(이) 이미 존재합니다 (유사도: %.2f%%). 다른 이름을 사용해주세요", existingName,
                similarityPercent));
      }
    }

    Interest interest = interestMapper.fromRegisterRequestDto(requestDto);
    interest.setSubscriberCount(0);

    List<InterestKeyword> newInterestKeywords = new ArrayList<>();
    for (String keywordName : requestDto.keywords()) {
      Keyword keywordEntity = keywordRepository.findByName(keywordName)
          .orElseGet(() -> {
            Keyword newKeyword = new Keyword();
            newKeyword.setName(keywordName);
            return keywordRepository.save(newKeyword);
          });

      InterestKeyword interestKeyword = new InterestKeyword();
      interestKeyword.setInterest(interest);
      interestKeyword.setKeyword(keywordEntity);

      newInterestKeywords.add(interestKeyword);
    }
    interest.setInterestKeywords(newInterestKeywords);

    Interest savedInterest = interestRepository.save(interest);
    boolean subscribedByMe = false;
    return interestMapper.toDto(savedInterest, subscribedByMe);
  }
}
