package com.part2.monew.service.newsprovider;

import com.part2.monew.entity.NewsArticle;
import java.util.List;

public interface NewsCrawlingService {

    String getSourceName();

    List<NewsArticle> searchNews(String query, int limit);

    List<NewsArticle> searchAndSaveNews(String query, int limit);

}
