package com.example.demo.iface.rest;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.application.domain.product.command.CreateProductCommand;
import com.example.demo.application.service.ProductCommandService;
import com.example.demo.application.service.ProductQueryService;
import com.example.demo.application.shared.dto.ProductQueriedView;
import com.example.demo.iface.dto.req.CreateProductResource;
import com.example.demo.iface.dto.res.ProductCreatedResource;
import com.example.demo.infra.mapper.ProductMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ProductController - 產品管理 API
 */
@Slf4j
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductCommandService commandService;
    private final ProductQueryService queryService;
    private final ProductMapper productMapper;

    /**
     * API: 建立新產品
     * <p>由管理人員發送，包含名稱、價格與初始庫存。</p>
     */
    @PostMapping("")
    public CompletableFuture<ResponseEntity<ProductCreatedResource>> createProduct(
            @RequestBody CreateProductResource resource) {
        CreateProductCommand command = productMapper.toCommand(resource);
        String productId = command.productId();
        return commandService.createProduct(command)
                .thenApply(result -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(new ProductCreatedResource("200", "產品建立成功", productId)))
                .exceptionally(ex -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ProductCreatedResource("400", "建立失敗: " + ex.getCause().getMessage(), null)));
    }

    /**
     * API: 獲取所有產品清單
     */
    @GetMapping("")
    public CompletableFuture<ResponseEntity<List<ProductQueriedView>>> listAll() {
        return queryService.findAll()
                .thenApply(ResponseEntity::ok);
    }

    /**
     * API: 獲取特定產品詳情
     */
    @GetMapping("/{productId}")
    public CompletableFuture<ResponseEntity<ProductQueriedView>> getProduct(@PathVariable String productId) {
        return queryService.findById(productId)
                .thenApply(data -> data != null ? ResponseEntity.ok(data) : ResponseEntity.notFound().build());
    }
}