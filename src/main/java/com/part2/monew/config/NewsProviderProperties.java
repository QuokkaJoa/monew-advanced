package com.part2.monew.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "monew.news-providers")
@Getter
@Setter
public class NewsProviderProperties {

    private Map<String, ProviderConfig> providers = new HashMap<>();

    @Getter
    @Setter
    public static class ProviderConfig {

        private String type; // api, rss
        private String name;
        private boolean enabled = true;
        private String apiUrl;
        private String clientId;
        private String clientSecret;
        private int defaultDisplay = 10;
        private String defaultSort = "date";
        private String feedUrl;

    }
}
