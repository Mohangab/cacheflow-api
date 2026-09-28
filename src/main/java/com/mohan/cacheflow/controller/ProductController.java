package com.mohan.cacheflow.controller;

import com.mohan.cacheflow.dto.ProductRequest;
import com.mohan.cacheflow.dto.ProductResponse;
import com.mohan.cacheflow.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Product catalog CRUD — GET by id / category are Redis-cached")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "List all products (not cached — source of truth scan)")
    public List<ProductResponse> listAll() {
        return productService.listAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by id (Redis cache: products)")
    public ProductResponse getById(@PathVariable Long id) {
        return productService.getById(id);
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "List products by category (Redis cache: productsByCategory)")
    public List<ProductResponse> listByCategory(@PathVariable String category) {
        return productService.listByCategory(category).getItems();
    }

    @PostMapping
    @Operation(summary = "Create product (evicts productsByCategory)")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product (refreshes products cache, evicts category lists)")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete product (evicts product + category caches)")
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }
}
