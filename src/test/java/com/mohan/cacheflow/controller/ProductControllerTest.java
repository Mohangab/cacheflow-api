package com.mohan.cacheflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohan.cacheflow.dto.ProductListResponse;
import com.mohan.cacheflow.dto.ProductRequest;
import com.mohan.cacheflow.dto.ProductResponse;
import com.mohan.cacheflow.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {ProductController.class, GlobalExceptionHandler.class})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Test
    void getById_returnsProduct() throws Exception {
        ProductResponse body = sample(1L, "Mouse", "Electronics");
        when(productService.getById(1L)).thenReturn(body);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mouse"))
                .andExpect(jsonPath("$.category").value("Electronics"));
    }

    @Test
    void listByCategory_returnsList() throws Exception {
        when(productService.listByCategory("Electronics"))
                .thenReturn(new ProductListResponse(List.of(sample(1L, "Mouse", "Electronics"))));

        mockMvc.perform(get("/api/products/category/Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Mouse"));
    }

    @Test
    void create_validatesRequiredFields() throws Exception {
        ProductRequest bad = new ProductRequest();
        bad.setName("");
        bad.setPrice(new BigDecimal("-1"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns201() throws Exception {
        ProductRequest req = new ProductRequest();
        req.setName("Hub");
        req.setCategory("Electronics");
        req.setPrice(new BigDecimal("19.99"));
        req.setDescription("USB hub");

        when(productService.create(any(ProductRequest.class)))
                .thenReturn(sample(9L, "Hub", "Electronics"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9));
    }

    @Test
    void update_returnsBody() throws Exception {
        ProductRequest req = new ProductRequest();
        req.setName("Hub Pro");
        req.setCategory("Electronics");
        req.setPrice(new BigDecimal("29.99"));

        when(productService.update(eq(9L), any(ProductRequest.class)))
                .thenReturn(sample(9L, "Hub Pro", "Electronics"));

        mockMvc.perform(put("/api/products/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hub Pro"));
    }

    private static ProductResponse sample(Long id, String name, String category) {
        ProductResponse r = new ProductResponse();
        r.setId(id);
        r.setName(name);
        r.setCategory(category);
        r.setPrice(new BigDecimal("9.99"));
        r.setDescription("test");
        r.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        r.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return r;
    }
}
