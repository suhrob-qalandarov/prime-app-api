package org.exp.primeapp.controller.admin.category;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.exp.primeapp.models.dto.request.CategoryReq;
import org.exp.primeapp.models.dto.responce.admin.AdminCategoryDashboardRes;
import org.exp.primeapp.models.dto.responce.admin.AdminCategoryRes;
import org.exp.primeapp.models.dto.responce.user.CategoryRes;
import org.exp.primeapp.models.enums.CategoryStatus;
import org.exp.primeapp.service.face.user.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static org.exp.primeapp.utils.Const.*;

@RestController
@RequestMapping(API + V2 + ADMIN + CATEGORY)
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{categoryId}")
    public ResponseEntity<AdminCategoryRes> getCategoryById(@PathVariable Long categoryId) {
        AdminCategoryRes adminCategoryRes  = categoryService.getAdminCategoryResById(categoryId);
        return new ResponseEntity<>(adminCategoryRes, HttpStatus.OK);
    }

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard")
    public ResponseEntity<AdminCategoryDashboardRes> getCategoryDashboard() {
        AdminCategoryDashboardRes categoryDashboardRes = categoryService.getCategoryDashboardRes();
        return new ResponseEntity<>(categoryDashboardRes, HttpStatus.ACCEPTED);
    }

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/for-product")
    public ResponseEntity<List<CategoryRes>> getCategoriesForProduct() {
        List<CategoryRes> categories = categoryService.getCategoriesResByStatuses(
                List.of(CategoryStatus.ACTIVE, CategoryStatus.CREATED)
        );
        return new ResponseEntity<>(categories, HttpStatus.OK);
    }

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<AdminCategoryRes> createCategory(@RequestBody CategoryReq categoryReq) {
        AdminCategoryRes adminCategoryRes = categoryService.saveCategory(categoryReq);
        return new ResponseEntity<>(adminCategoryRes, HttpStatus.CREATED);
    }

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{categoryId}")
    public ResponseEntity<AdminCategoryRes> updateCategory(
            @PathVariable Long categoryId,
            @RequestBody CategoryReq categoryReq
    ) {
        AdminCategoryRes categoryRes = categoryService.updateCategoryById(categoryId,categoryReq);
        return new ResponseEntity<>(categoryRes, HttpStatus.ACCEPTED);
    }

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/toggle/{categoryId}")
    public ResponseEntity<AdminCategoryRes> toggleCategoryWithType(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "false") Boolean withProducts
    ) {
        AdminCategoryRes toggledCategoryRes = categoryService.toggleCategoryStatus(categoryId, withProducts);
        return new ResponseEntity<>(toggledCategoryRes, HttpStatus.ACCEPTED);
    }

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/order")
    public ResponseEntity<List<AdminCategoryRes>> updateCategoriesOrder(
            @RequestBody Map<Long, Long> categoryOrderMap
    ) {
        List<AdminCategoryRes> updatedCategories = categoryService.updateCategoryOrder(categoryOrderMap);
        return new ResponseEntity<>(updatedCategories, HttpStatus.ACCEPTED);
    }
}
