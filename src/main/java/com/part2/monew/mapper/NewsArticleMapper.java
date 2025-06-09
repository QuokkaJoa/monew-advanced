package com.part2.monew.mapper;

import com.part2.monew.dto.response.NewsArticleResponseDto;
import com.part2.monew.entity.NewsArticle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NewsArticleMapper {

    @Mapping(target = "viewedByMe", source = "viewedByMeValue")
    @Mapping(target = "source", source = "newsArticle.sourceIn")
    @Mapping(target = "publishDate", source = "newsArticle.publishedDate")
    @Mapping(target = "commentCount", source = "actualCommentCount")
    NewsArticleResponseDto toDto(NewsArticle newsArticle, Boolean viewedByMeValue, Long actualCommentCount);

    @Mapping(target = "viewedByMe", source = "viewedByMeValue")
    @Mapping(target = "source", source = "newsArticle.sourceIn")
    @Mapping(target = "publishDate", source = "newsArticle.publishedDate")
    NewsArticleResponseDto toDto(NewsArticle newsArticle, Boolean viewedByMeValue);

} 