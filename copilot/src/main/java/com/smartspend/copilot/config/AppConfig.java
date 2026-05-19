package com.smartspend.copilot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class AppConfig {
    @Bean
    public RestClient restClient(){
        return RestClient.create();
    }

    @Bean
    public ObjectMapper objectMapper(){ return new ObjectMapper(); }
}
