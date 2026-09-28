package com.mohan.cacheflow.config;

import com.mohan.cacheflow.entity.Product;
import com.mohan.cacheflow.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final ProductRepository productRepository;

    public DataSeeder(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            return;
        }
        List<Product> seed = List.of(
                new Product("Wireless Mouse", "Electronics", new BigDecimal("29.99"),
                        "Ergonomic 2.4GHz mouse with silent clicks"),
                new Product("Mechanical Keyboard", "Electronics", new BigDecimal("89.00"),
                        "Hot-swappable switches, RGB backlight"),
                new Product("USB-C Hub", "Electronics", new BigDecimal("45.50"),
                        "7-in-1 hub with HDMI and SD card reader"),
                new Product("Standing Desk Mat", "Furniture", new BigDecimal("39.99"),
                        "Anti-fatigue mat for standing desks"),
                new Product("Monitor Arm", "Furniture", new BigDecimal("79.00"),
                        "Gas-spring single monitor mount"),
                new Product("Notebook A5", "Stationery", new BigDecimal("8.50"),
                        "Dotted grid, 160 pages, lay-flat binding")
        );
        productRepository.saveAll(seed);
        log.info("Seeded {} sample products into H2", seed.size());
    }
}
