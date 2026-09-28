package com.mohan.cacheflow.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around a product list so Redis/Jackson default-typing can serialize
 * the cache value as a JSON object (raw {@code List} values are awkward with
 * {@code @class} type info).
 */
public class ProductListResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<ProductResponse> items = new ArrayList<>();

    public ProductListResponse() {
    }

    public ProductListResponse(List<ProductResponse> items) {
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }

    public List<ProductResponse> getItems() {
        return items;
    }

    public void setItems(List<ProductResponse> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }
}
