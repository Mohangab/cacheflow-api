package com.mohan.cacheflow.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * Decorates every cache so GET hits/misses are logged — recruiters can watch the console
 * and correlate with curl timings.
 */
public class LoggingCacheManager implements CacheManager {

    private final CacheManager delegate;

    public LoggingCacheManager(CacheManager delegate) {
        this.delegate = delegate;
    }

    @Override
    @Nullable
    public Cache getCache(String name) {
        Cache cache = delegate.getCache(name);
        return cache == null ? null : new LoggingCache(cache);
    }

    @Override
    public Collection<String> getCacheNames() {
        return delegate.getCacheNames();
    }

    static final class LoggingCache implements Cache {

        private static final Logger log = LoggerFactory.getLogger(LoggingCache.class);

        private final Cache delegate;

        LoggingCache(Cache delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public Object getNativeCache() {
            return delegate.getNativeCache();
        }

        @Override
        @Nullable
        public ValueWrapper get(Object key) {
            ValueWrapper value = delegate.get(key);
            if (value != null) {
                log.info("CACHE HIT  — cache='{}' key={}", getName(), key);
            } else {
                log.info("CACHE MISS — cache='{}' key={} (loading from DB next)", getName(), key);
            }
            return value;
        }

        @Override
        @Nullable
        public <T> T get(Object key, @Nullable Class<T> type) {
            T value = delegate.get(key, type);
            if (value != null) {
                log.info("CACHE HIT  — cache='{}' key={}", getName(), key);
            } else {
                log.info("CACHE MISS — cache='{}' key={} (loading from DB next)", getName(), key);
            }
            return value;
        }

        @Override
        @Nullable
        public <T> T get(Object key, Callable<T> valueLoader) {
            // Spring's RedisCache uses get(key) + put for @Cacheable; this overload is rare.
            return delegate.get(key, valueLoader);
        }

        @Override
        public void put(Object key, @Nullable Object value) {
            log.debug("CACHE PUT  — cache='{}' key={}", getName(), key);
            delegate.put(key, value);
        }

        @Override
        @Nullable
        public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
            return delegate.putIfAbsent(key, value);
        }

        @Override
        public void evict(Object key) {
            log.info("CACHE EVICT — cache='{}' key={}", getName(), key);
            delegate.evict(key);
        }

        @Override
        public void clear() {
            log.info("CACHE CLEAR — cache='{}'", getName());
            delegate.clear();
        }
    }
}
