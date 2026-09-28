package com.mohan.cacheflow;

import com.mohan.cacheflow.config.CacheProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
public class CacheflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(CacheflowApplication.class, args);
    }
}
