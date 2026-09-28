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
                // Electronics
                new Product("Wireless Mouse", "Electronics", new BigDecimal("29.99"),
                        "Ergonomic 2.4GHz mouse with silent clicks"),
                new Product("Mechanical Keyboard", "Electronics", new BigDecimal("89.00"),
                        "Hot-swappable switches, RGB backlight"),
                new Product("USB-C Hub", "Electronics", new BigDecimal("45.50"),
                        "7-in-1 hub with HDMI and SD card reader"),
                new Product("Noise-Cancelling Headphones", "Electronics", new BigDecimal("129.00"),
                        "Over-ear Bluetooth ANC headset"),
                // Furniture
                new Product("Standing Desk Mat", "Furniture", new BigDecimal("39.99"),
                        "Anti-fatigue mat for standing desks"),
                new Product("Monitor Arm", "Furniture", new BigDecimal("79.00"),
                        "Gas-spring single monitor mount"),
                new Product("Ergonomic Chair", "Furniture", new BigDecimal("249.00"),
                        "Lumbar support, adjustable armrests"),
                // Stationery
                new Product("Notebook A5", "Stationery", new BigDecimal("8.50"),
                        "Dotted grid, 160 pages, lay-flat binding"),
                new Product("Gel Pen Set", "Stationery", new BigDecimal("12.00"),
                        "12-color smooth gel pens"),
                // Accessories
                new Product("Laptop Sleeve 14\"", "Accessories", new BigDecimal("24.99"),
                        "Padded neoprene sleeve with zipper pocket"),
                new Product("Cable Organizer Kit", "Accessories", new BigDecimal("15.00"),
                        "Velcro ties and desk clips, pack of 20"),
                // Home Office
                new Product("LED Desk Lamp", "Home Office", new BigDecimal("34.50"),
                        "Dimmable USB-C lamp with night mode"),
                new Product("Webcam Cover Pack", "Home Office", new BigDecimal("6.99"),
                        "Sliding privacy covers, 3-pack")
        );
        productRepository.saveAll(seed);
        log.info("Seeded {} sample products across {} categories into H2",
                seed.size(),
                seed.stream().map(Product::getCategory).distinct().count());
    }
}
