package org.exp.primeapp.controller.user.product;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.models.dto.responce.user.ProductPageRes;
import org.exp.primeapp.models.dto.responce.user.ProductRes;
import org.exp.primeapp.models.dto.responce.user.page.PageRes;
import org.exp.primeapp.service.face.user.ProductService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.exp.primeapp.utils.Const.*;

@Slf4j
@RestController
@RequestMapping(API + V1 + PRODUCT)
@RequiredArgsConstructor
public class ProductController {

        private final ProductService productService;

        @GetMapping("/{productId}")
        public ResponseEntity<ProductRes> getProduct(@PathVariable Long productId) {
                ProductRes product = productService.getProductById(productId);
                return ResponseEntity.ok(product);
        }

        @GetMapping
        public ResponseEntity<PageRes<ProductPageRes>> getProducts(
                        @RequestParam(required = false) String spotlightName,
                        @RequestParam(required = false) String categoryName,
                        @RequestParam(required = false) String colorName,
                        @RequestParam(required = false) String sizeName,
                        @RequestParam(required = false) String brandName,
                        @Parameter(description = "Product tag filter", schema = @Schema(type = "string", allowableValues = {
                                        "NEW", "HOT",
                                        "SALE" }, example = "SALE")) @RequestParam(required = false) String tag,
                        @Parameter(description = "Sort by option", schema = @Schema(type = "string", allowableValues = {
                                        "discount", "low-price",
                                        "high-price" }, example = "low-price")) @RequestParam(required = false) String sortBy,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                Pageable pageable = PageRequest.of(page, size);
                PageRes<ProductPageRes> pageableProducts = productService.getActiveProducts(
                                spotlightName, categoryName, colorName, sizeName, brandName, tag, sortBy, pageable);
                return ResponseEntity.ok(pageableProducts);
        }
}
