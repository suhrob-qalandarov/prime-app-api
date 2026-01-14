package org.exp.primeapp.controller.admin.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.exp.primeapp.models.dto.request.ProductReq;
import org.exp.primeapp.models.dto.responce.admin.AdminProductDashboardRes;
import org.exp.primeapp.models.dto.responce.admin.AdminProductRes;
import org.exp.primeapp.models.dto.responce.user.ProductPageRes;
import org.exp.primeapp.service.face.admin.product.AdminProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.exp.primeapp.utils.Const.*;

@RestController
@RequestMapping(API + V2 + ADMIN + PRODUCT)
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/{productId}")
    public ResponseEntity<AdminProductRes> getProduct(@PathVariable Long productId) {
        AdminProductRes adminProductRes = adminProductService.getProductById(productId);
        return new ResponseEntity<>(adminProductRes, HttpStatus.OK);
    }

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/dashboard")
    public ResponseEntity<AdminProductDashboardRes> getDashboardProducts() {
        AdminProductDashboardRes adminDashboardProductsRes = adminProductService.getProductDashboardRes();
        return new ResponseEntity<>(adminDashboardProductsRes, HttpStatus.OK);
    }

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/income")
    public ResponseEntity<List<ProductPageRes>> getIncomeProducts() {
        List<ProductPageRes> productList = adminProductService.getProductListForIncome();
        return new ResponseEntity<>(productList, HttpStatus.OK);
    }

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<AdminProductRes> addProduct(@Valid @RequestBody ProductReq productReq) {
        AdminProductRes adminProductRes = adminProductService.saveProduct(productReq);
        return new ResponseEntity<>(adminProductRes, HttpStatus.CREATED);
    }

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/{productId}")
    public ResponseEntity<AdminProductRes> updateProduct(@PathVariable Long productId, @RequestBody ProductReq productReq) {
        AdminProductRes adminProductRes = adminProductService.updateProduct(productId, productReq);
        return new ResponseEntity<>(adminProductRes, HttpStatus.ACCEPTED);
    }

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @PatchMapping("/toggle/{productId}")
    public ResponseEntity<AdminProductRes> activateProduct(@PathVariable Long productId) {
        AdminProductRes adminProductRes = adminProductService.toggleProductUpdate(productId);
        return new ResponseEntity<>(adminProductRes, HttpStatus.ACCEPTED);
    }
}