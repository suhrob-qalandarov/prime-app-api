package org.exp.primeapp.controller.admin.inventory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.exp.primeapp.models.dto.request.InventoryTransactionRequest;
import org.exp.primeapp.models.dto.response.InventoryTransactionPageResponse;
import org.exp.primeapp.models.dto.response.InventoryTransactionResponse;
import org.exp.primeapp.service.face.admin.inventory.InventoryTransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Admin controller for managing inventory transactions
 */
@RestController
@RequestMapping("/api/v1/admin/inventory-transactions")
@RequiredArgsConstructor
@Tag(name = "Admin Inventory Transactions", description = "Inventory transaction management APIs (IN/OUT)")
@SecurityRequirement(name = "bearerAuth")
public class AdminInventoryTransactionController {

    private final InventoryTransactionService inventoryTransactionService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create inventory transaction", description = "Create IN (income) or OUT (outcome) transaction with size-based items")
    public ResponseEntity<InventoryTransactionResponse> createTransaction(
            @Valid @RequestBody InventoryTransactionRequest request) {
        InventoryTransactionResponse response = inventoryTransactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<InventoryTransactionResponse> getTransactionById(@PathVariable Long id) {
        InventoryTransactionResponse response = inventoryTransactionService.getTransactionById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get all transactions", description = "Get all transactions with filtering, pagination and statistics")
    public ResponseEntity<InventoryTransactionPageResponse> getAllTransactions(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String productSize,
            @RequestParam(required = false) String productTag,
            @RequestParam(required = false) Long performedById,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        InventoryTransactionPageResponse response = inventoryTransactionService.getAllTransactionsWithStats(
                type, reason, productId, categoryId, productSize, productTag, performedById,
                startDate, endDate, pageable);
        return ResponseEntity.ok(response);
    }
}
