package org.exp.primeapp.controller.admin.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.exp.primeapp.models.dto.responce.admin.AdminSizeRes;
import org.exp.primeapp.service.face.admin.product.AdminSizeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import static org.exp.primeapp.utils.Const.*;

@RestController
@RequestMapping(API + V2 + ADMIN + PRODUCT + SIZE)
@RequiredArgsConstructor
public class AdminProductSizeController {

    private final AdminSizeService adminSizeService;

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<AdminSizeRes>> getSizes() {
        List<AdminSizeRes> sizeList = adminSizeService.getSizeList();
        return ResponseEntity.ok(sizeList);
    }
}
