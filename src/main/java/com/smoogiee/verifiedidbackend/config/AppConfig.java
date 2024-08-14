package com.smoogiee.verifiedidbackend.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class used to initialize application beans
 */
@Configuration
public class AppConfig {
    /**
     * Initializes AzureProperties bean
     *
     * @return An AzureProperties bean
     */
    @Bean
    public AzureProperties azureProperties() {
        return new AzureProperties();
    }

    /**
     * Initializes VerifiedIdProperties bean
     *
     * @return A VerifiedIdProperties bean
     */
    @Bean
    public VerifiedIdProperties verifiedIdProperties() {
        return new VerifiedIdProperties();
    }

    /**
     * Initializes a fasterXML Jackson ObjectMapper bean
     *
     * @return A ObjectMapper bean
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return objectMapper;
    }
}
