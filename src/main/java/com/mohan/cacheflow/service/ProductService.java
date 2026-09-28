package com.mohan.cacheflow.service;

import com.mohan.cacheflow.config.RedisCacheConfig;
import com.mohan.cacheflow.dto.ProductListResponse;
import com.mohan.cacheflow.dto.ProductRequest;
import com.mohan.cacheflow.dto.ProductResponse;
import com.mohan.cacheflow.entity.Product;
import com.mohan.cacheflow.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Cache-aside product service.
 * <p>
 * Reads go through Redis via {@code @Cacheable}. Writes evict / refresh related keys so the
 * next read reloads from H2 (source of truth). An intentional sleep on DB load makes MISS vs
 * HIT obvious in curl timings; see also {@link com.mohan.cacheflow.config.LoggingCacheManager}.
 */
@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    /** Artificial DB latency so recruiters can see MISS vs HIT in curl time. */
    private static final long SIMULATED_DB_LATENCY_MS = 400L;

    private final ProductRepository productRepository;
    private final CacheManager cacheManager;
    private final AtomicLong dbLoads = new AtomicLong();

    public ProductService(ProductRepository productRepository, CacheManager cacheManager) {
        this.productRepository = productRepository;
        this.cacheManager = cacheManager;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisCacheConfig.CACHE_PRODUCTS, key = "#id")
    public ProductResponse getById(Long id) {
        log.info("DB LOAD — product id={} from H2 (cache miss path)", id);
        dbLoads.incrementAndGet();
        simulateDbLatency();
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Product not found: " + id));
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisCacheConfig.CACHE_PRODUCTS_BY_CATEGORY, key = "#category.toLowerCase()")
    public ProductListResponse listByCategory(String category) {
        log.info("DB LOAD — products category='{}' from H2 (cache miss path)", category);
        dbLoads.incrementAndGet();
        simulateDbLatency();
        List<ProductResponse> items = productRepository
                .findByCategoryIgnoreCaseOrderByNameAsc(category).stream()
                .map(ProductResponse::from)
                .toList();
        return new ProductListResponse(items);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listAll() {
        // Full catalog listing is not cached — only hot paths (by-id / by-category) are.
        return productRepository.findAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_PRODUCTS_BY_CATEGORY, allEntries = true)
    })
    public ProductResponse create(ProductRequest request) {
        Product product = new Product(
                request.getName().trim(),
                request.getCategory().trim(),
                request.getPrice(),
                request.getDescription()
        );
        Product saved = productRepository.save(product);
        log.info("Created product id={} — evicted productsByCategory cache", saved.getId());
        return ProductResponse.from(saved);
    }

    @Transactional
    @Caching(
            put = {
                    @CachePut(cacheNames = RedisCacheConfig.CACHE_PRODUCTS, key = "#id")
            },
            evict = {
                    @CacheEvict(cacheNames = RedisCacheConfig.CACHE_PRODUCTS_BY_CATEGORY, allEntries = true)
            }
    )
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Product not found: " + id));
        product.setName(request.getName().trim());
        product.setCategory(request.getCategory().trim());
        product.setPrice(request.getPrice());
        product.setDescription(request.getDescription());
        Product saved = productRepository.save(product);
        log.info("Updated product id={} — refreshed products cache, evicted productsByCategory", id);
        return ProductResponse.from(saved);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_PRODUCTS, key = "#id"),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_PRODUCTS_BY_CATEGORY, allEntries = true)
    })
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id);
        }
        productRepository.deleteById(id);
        log.info("Deleted product id={} — evicted related Redis cache entries", id);
    }


    /**
     * Pre-load Redis from H2 for demos / cold starts.
     * Loads the full catalog once (no simulated per-row latency), then writes
     * {@code products} and {@code productsByCategory} entries via the CacheManager.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> warmCaches() {
        List<Product> all = productRepository.findAll();
        dbLoads.incrementAndGet();
        log.info("CACHE WARM — loaded {} products from H2 in one scan", all.size());

        Cache products = cacheManager.getCache(RedisCacheConfig.CACHE_PRODUCTS);
        Cache byCategory = cacheManager.getCache(RedisCacheConfig.CACHE_PRODUCTS_BY_CATEGORY);
        if (products == null || byCategory == null) {
            throw new IllegalStateException("Required caches not configured");
        }

        for (Product product : all) {
            products.put(product.getId(), ProductResponse.from(product));
        }

        Map<String, List<ProductResponse>> grouped = all.stream()
                .map(ProductResponse::from)
                .collect(Collectors.groupingBy(
                        r -> r.getCategory().toLowerCase(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        for (Map.Entry<String, List<ProductResponse>> entry : grouped.entrySet()) {
            byCategory.put(entry.getKey(), new ProductListResponse(entry.getValue()));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("warmedProducts", all.size());
        result.put("warmedCategories", grouped.size());
        result.put("categories", grouped.keySet());
        result.put("dbLoadsSinceStart", dbLoads.get());
        return result;
    }

    public long getDbLoadCount() {
        return dbLoads.get();
    }

    public void resetDbLoadCount() {
        dbLoads.set(0);
    }

    private void simulateDbLatency() {
        try {
            Thread.sleep(SIMULATED_DB_LATENCY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
