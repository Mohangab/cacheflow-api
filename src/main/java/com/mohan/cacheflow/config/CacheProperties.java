package com.mohan.cacheflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunable Redis cache TTLs (seconds). Bound from {@code cacheflow.cache.*}
 * / env vars {@code CACHE_PRODUCTS_TTL} and {@code CACHE_CATEGORY_TTL}.
 */
@ConfigurationProperties(prefix = "cacheflow.cache")
public class CacheProperties {

    /** TTL for single-product entries ({@code products} cache). Default 5 minutes. */
    private long productsTtlSeconds = 300;

    /** TTL for category list entries ({@code productsByCategory}). Default 2 minutes. */
    private long productsByCategoryTtlSeconds = 120;

    public long getProductsTtlSeconds() {
        return productsTtlSeconds;
    }

    public void setProductsTtlSeconds(long productsTtlSeconds) {
        this.productsTtlSeconds = productsTtlSeconds;
    }

    public long getProductsByCategoryTtlSeconds() {
        return productsByCategoryTtlSeconds;
    }

    public void setProductsByCategoryTtlSeconds(long productsByCategoryTtlSeconds) {
        this.productsByCategoryTtlSeconds = productsByCategoryTtlSeconds;
    }
}
