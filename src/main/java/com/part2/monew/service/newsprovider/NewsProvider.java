package com.part2.monew.service.newsprovider;

import com.part2.monew.config.NewsProviderProperties.ProviderConfig;
import java.util.List;

public interface NewsProvider {


    String getProviderKey();


    List<com.part2.monew.dto.request.NewsArticleDto> fetchNews(ProviderConfig config, List<String> keywords);
}
