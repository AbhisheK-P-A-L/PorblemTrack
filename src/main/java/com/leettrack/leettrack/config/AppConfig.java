package com.leettrack.leettrack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    /**
     * Shared RestTemplate for server-side API calls to LeetCode GraphQL and
     * Codeforces REST. All external API calls go through here — never called
     * from frontend JS.
     *
     * FUTURE WORK: wrap this with Spring Cache + Redis to cache LeetCode/Codeforces
     * responses for ~5 minutes. Today, every search hits upstream APIs directly.
     * Cache key = "search:" + keyword. This is a O(1) addition when Redis is available.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
