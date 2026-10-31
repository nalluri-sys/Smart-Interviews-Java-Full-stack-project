package com.smartinterviewcoach.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${app.supabase.url}")
    private String supabaseUrl;

    @Value("${app.supabase.service-role-key}")
    private String serviceRoleKey;

    @Bean("supabaseWebClient")
    public WebClient supabaseWebClient(WebClient.Builder builder) {
        return builder
            .baseUrl(supabaseUrl)
            .defaultHeader("apikey", serviceRoleKey)
            .defaultHeader("Authorization", "Bearer " + serviceRoleKey)
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("Prefer", "return=representation")
            .build();
    }

    @Bean("geminiWebClient")
    public WebClient geminiWebClient(WebClient.Builder builder) {
        return builder
            .baseUrl("https://generativelanguage.googleapis.com")
            .defaultHeader("Content-Type", "application/json")
            .build();
    }
}
