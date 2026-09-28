package com.mohan.cacheflow.service;

import com.mohan.cacheflow.config.RedisCacheConfig;
import com.mohan.cacheflow.dto.ProductRequest;
import com.mohan.cacheflow.dto.ProductResponse;
import com.mohan.cacheflow.entity.Product;
import com.mohan.cacheflow.repository.ProductRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.ServerSocket;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: real Spring Cache + embedded Redis.
 * Proves second getById does not hit the DB (dbLoads stays at 1).
 */
@SpringBootTest
class ProductServiceCacheTest {

    private static final int REDIS_PORT;
    private static final RedisServer REDIS_SERVER;

    static {
        try {
            REDIS_PORT = findFreePort();
            REDIS_SERVER = RedisServer.newRedisServer()
                    .port(REDIS_PORT)
                    .bind("127.0.0.1")
                    .build();
            REDIS_SERVER.start();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @AfterAll
    static void stopRedis() throws IOException {
        if (REDIS_SERVER != null) {
            REDIS_SERVER.stop();
        }
    }

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", () -> "127.0.0.1");
        registry.add("spring.data.redis.port", () -> REDIS_PORT);
    }

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCachesAndStats() {
        var products = cacheManager.getCache(RedisCacheConfig.CACHE_PRODUCTS);
        var byCat = cacheManager.getCache(RedisCacheConfig.CACHE_PRODUCTS_BY_CATEGORY);
        if (products != null) {
            products.clear();
        }
        if (byCat != null) {
            byCat.clear();
        }
        productService.resetDbLoadCount();
    }

    @Test
    void getById_secondCallIsCacheHit() {
        Product saved = productRepository.save(
                new Product("Cache Test Widget", "Test", new BigDecimal("1.00"), "demo"));

        long before = productService.getDbLoadCount();

        ProductResponse first = productService.getById(saved.getId());
        ProductResponse second = productService.getById(saved.getId());

        assertThat(first.getName()).isEqualTo("Cache Test Widget");
        assertThat(second.getId()).isEqualTo(first.getId());
        // Method body ran once → one DB load; second call served from Redis.
        assertThat(productService.getDbLoadCount()).isEqualTo(before + 1);
    }

    @Test
    void listByCategory_secondCallIsCacheHit() {
        productRepository.save(new Product("Cat A1", "Gadgets", new BigDecimal("2.00"), "a"));
        productRepository.save(new Product("Cat A2", "Gadgets", new BigDecimal("3.00"), "b"));

        long before = productService.getDbLoadCount();
        assertThat(productService.listByCategory("Gadgets").getItems()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(productService.listByCategory("Gadgets").getItems()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(productService.getDbLoadCount()).isEqualTo(before + 1);
    }

    @Test
    void update_evictsCategoryCache_andRefreshesProductCache() {
        Product saved = productRepository.save(
                new Product("Old Name", "Office", new BigDecimal("5.00"), "x"));

        productService.getById(saved.getId());
        productService.listByCategory("Office"); // warm category cache
        long loadsAfterWarm = productService.getDbLoadCount();

        ProductRequest req = new ProductRequest();
        req.setName("New Name");
        req.setCategory("Office");
        req.setPrice(new BigDecimal("6.00"));
        req.setDescription("updated");
        productService.update(saved.getId(), req);

        // Product cache was @CachePut — next getById should NOT bump dbLoads.
        ProductResponse cached = productService.getById(saved.getId());
        assertThat(cached.getName()).isEqualTo("New Name");
        assertThat(productService.getDbLoadCount()).isEqualTo(loadsAfterWarm);

        // Category cache was evicted — next list should miss and load DB.
        productService.listByCategory("Office");
        assertThat(productService.getDbLoadCount()).isEqualTo(loadsAfterWarm + 1);
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
