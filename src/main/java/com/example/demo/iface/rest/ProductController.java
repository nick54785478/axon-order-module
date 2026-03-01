package com.example.demo.iface.rest;

import java.util.concurrent.CompletableFuture;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.application.domain.product.command.CreateProductCommand;
import com.example.demo.application.domain.product.command.UpdateProductCommand;
import com.example.demo.application.domain.product.projection.ProductPageQueriedView;
import com.example.demo.application.service.ProductCommandService;
import com.example.demo.application.service.ProductQueryService;
import com.example.demo.application.shared.dto.ProductQueriedView;
import com.example.demo.iface.dto.req.CreateProductResource;
import com.example.demo.iface.dto.req.UpdateProductResource;
import com.example.demo.iface.dto.res.ProductCreatedResource;
import com.example.demo.iface.dto.res.ProductListQueriedResource;
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
	 * <p>
	 * 由管理人員發送，包含名稱、價格與初始庫存。
	 * </p>
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
	 * API: 更新產品資訊
	 * 
	 * @param productId 產品 ID
	 * @param resource  包含 version, name, price 的請求體
	 */
	@PutMapping("/{productId}")
	public CompletableFuture<ResponseEntity<ProductCreatedResource>> updateProduct(@PathVariable String productId,
			@RequestBody UpdateProductResource resource) {

		UpdateProductCommand command = new UpdateProductCommand(productId, resource.version(), resource.name(),
				resource.price());

		return commandService.updateProduct(command)
				.thenApply(result -> ResponseEntity.ok().body(new ProductCreatedResource("200", "產品更新成功", productId)))
				.exceptionally(ex -> {

					// 關鍵：取得真正的異常原因 (Cause)
					Throwable cause = (ex.getCause() != null) ? ex.getCause() : ex;

					log.error("[API] 更新失敗: {}", cause.getMessage());

					// 判斷是否為版本衝突
					if (cause instanceof org.axonframework.modelling.command.ConcurrencyException) {
						return ResponseEntity.status(HttpStatus.CONFLICT)
								.body(new ProductCreatedResource("409", "更新衝突：該資料已被他人修改，請重新整理", productId));
					}

					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body(new ProductCreatedResource("400", "更新失敗: " + cause.getMessage(), productId));
				});
	}

	/**
	 * API: 獲取所有產品清單
	 */
	@GetMapping("")
	public CompletableFuture<ResponseEntity<ProductListQueriedResource>> listAll() {
		return queryService.findAll()
				.thenApply(result -> ResponseEntity.ok().body(new ProductListQueriedResource("200", "查詢成功", result)));
	}

	/**
	 * API: 獲取特定產品詳情
	 */
	@GetMapping("/{productId}")
	public CompletableFuture<ResponseEntity<ProductQueriedView>> getProduct(@PathVariable String productId) {
		return queryService.findById(productId)
				.thenApply(data -> data != null ? ResponseEntity.ok(data) : ResponseEntity.notFound().build());
	}

	/**
	 * API: 分頁查詢產品清單 GET /products?name=滑鼠&page=0&size=10
	 */
	@GetMapping("/summary")
	public CompletableFuture<ResponseEntity<ProductPageQueriedView>> getProducts(
			@RequestParam(required = false) String name, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		return queryService.findPaged(name, page, size).thenApply(ResponseEntity::ok);
	}
}