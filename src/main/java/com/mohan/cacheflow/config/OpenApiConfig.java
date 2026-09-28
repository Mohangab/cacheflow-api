package com.mohan.cacheflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

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

                                **Quick demo:** `GET /api/products/1` twice and compare timings / logs \
                                (`CACHE MISS` then `CACHE HIT`). Use `POST /api/demo/cache/warm` to \
                                preload Redis, or `DELETE /api/demo/cache` to clear it.

                                Auth intentionally omitted so Redis stays the star of the demo.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Mohan Venkatesh Kola")
                                .url("https://github.com/Mohangab/cacheflow-api"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local (Maven / Docker Compose)")))
                .tags(List.of(
                        new Tag().name("Products").description("Catalog CRUD — by-id and by-category reads are cached"),
                        new Tag().name("Demo").description("Warm / clear caches and inspect Redis key samples")));
    }
}
