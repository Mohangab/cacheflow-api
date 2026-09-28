package com.mohan.cacheflow.controller;

import com.mohan.cacheflow.config.RedisCacheConfig;
import com.mohan.cacheflow.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

@RestController
@RequestMapping("/api/demo")
@Tag(name = "Demo", description = "Cache visibility helpers for recruiters / demos")
public class DemoController {

    private final ProductService productService;
    private final CacheManager cacheManager;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisConnectionFactory redisConnectionFactory;

    public DemoController(ProductService productService,
                          CacheManager cacheManager,
                          StringRedisTemplate stringRedisTemplate,
                          RedisConnectionFactory redisConnectionFactory) {
        this.productService = productService;
        this.cacheManager = cacheManager;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @GetMapping("/cache-stats")
    @Operation(summary = "Show DB-load counter + Redis key sample (proves cache is working)")
    public Map<String, Object> cacheStats() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dbLoadsSinceStart", productService.getDbLoadCount());
        body.put("note", "dbLoads increments only on CACHE MISS (method body ran). "
                + "Watch application logs for 'CACHE HIT' / 'CACHE MISS' lines.");
        body.put("cacheNames", cacheManager.getCacheNames());

        Set<String> keys = stringRedisTemplate.keys("*");
        body.put("redisKeyCount", keys == null ? 0 : keys.size());
        body.put("redisKeySample", keys == null ? Set.of() : keys.stream().limit(20).sorted().toList());

        try {
            Properties info = redisConnectionFactory.getConnection().serverCommands().info("server");
            if (info != null) {
                body.put("redisVersion", info.getProperty("redis_version"));
            }
        } catch (Exception ignored) {
            body.put("redisVersion", "unavailable");
        }
        return body;
    }

    @PostMapping("/cache-stats/reset")
    @Operation(summary = "Reset the in-process DB-load counter")
    public Map<String, Object> resetStats() {
        productService.resetDbLoadCount();
        return Map.of("dbLoadsSinceStart", 0, "status", "reset");
    }

    @DeleteMapping("/cache/{cacheName}")
    @Operation(summary = "Clear a named cache (products | productsByCategory)")
    public Map<String, Object> clearCache(@PathVariable String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return Map.of("cleared", false, "reason", "unknown cache: " + cacheName,
                    "known", cacheManager.getCacheNames());
        }
        cache.clear();
        return Map.of("cleared", true, "cache", cacheName);
    }

    @DeleteMapping("/cache")
    @Operation(summary = "Clear both product caches")
    public Map<String, Object> clearAll() {
        Cache products = cacheManager.getCache(RedisCacheConfig.CACHE_PRODUCTS);
        Cache byCat = cacheManager.getCache(RedisCacheConfig.CACHE_PRODUCTS_BY_CATEGORY);
        if (products != null) products.clear();
        if (byCat != null) byCat.clear();
        productService.resetDbLoadCount();
        return Map.of("cleared", true, "caches", cacheManager.getCacheNames());
    }
}
