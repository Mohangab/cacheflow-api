package com.mohan.cacheflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cacheflowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CacheFlow API")
                        .description("""
                                Product catalog REST API showcasing **Redis as a Spring Cache layer** \
                                (cache-aside, TTL, eviction) on top of JPA + H2.

                                Auth intentionally omitted so Redis stays the star of the demo.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Mohan Venkatesh Kola")
                                .url("https://github.com/Mohangab"))
                        .license(new License().name("MIT")));
    }
}
