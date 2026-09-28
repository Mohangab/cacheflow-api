package com.mohan.cacheflow.dto;

import com.mohan.cacheflow.entity.Product;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public class ProductResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String category;
    private BigDecimal price;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;

    public ProductResponse() {
    }

    public static ProductResponse from(Product product) {
        ProductResponse r = new ProductResponse();
        r.id = product.getId();
        r.name = product.getName();
        r.category = product.getCategory();
        r.price = product.getPrice();
        r.description = product.getDescription();
        r.createdAt = product.getCreatedAt();
        r.updatedAt = product.getUpdatedAt();
        return r;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
