package org.exp.primeapp.controller.user.product;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.models.dto.responce.user.ProductPageRes;
import org.exp.primeapp.models.dto.responce.user.ProductRes;
import org.exp.primeapp.models.dto.responce.user.page.PageRes;
import org.exp.primeapp.service.face.user.ProductService;
import org.exp.primeapp.utils.SessionTokenUtil;
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
    private final SessionTokenUtil sessionTokenUtil;

    @GetMapping("/{productId}")
    public ResponseEntity<?> getProduct(
            @PathVariable Long productId,
            HttpServletRequest request,
            HttpServletResponse response) {
        return sessionTokenUtil.handleSessionTokenRequest("product", request, response, () -> {
            ProductRes product = productService.getProductById(productId);
            return ResponseEntity.ok(product);
        });
    }

    @GetMapping
    public ResponseEntity<?> getProducts(
            @RequestParam(required = false) String spotlightName,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) String colorName,
            @RequestParam(required = false) String sizeName,
            @RequestParam(required = false) String brandName,
            @RequestParam(required = false) String sortBy, // "discount", "low-price", "high-price"
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request,
            HttpServletResponse response) {
        return sessionTokenUtil.handleSessionTokenRequest("product", request, response, () -> {
            Pageable pageable = PageRequest.of(page, size);
            PageRes<ProductPageRes> pageableProducts = productService.getActiveProducts(
                    spotlightName, categoryName, colorName, sizeName, brandName, sortBy, pageable);
            return ResponseEntity.ok(pageableProducts);
        });
    }
}
