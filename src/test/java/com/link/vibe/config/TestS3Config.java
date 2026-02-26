package com.link.vibe.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.s3.S3Client;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestS3Config {

    @Bean
    public S3Client s3Client() {
        return mock(S3Client.class);
    }
}
