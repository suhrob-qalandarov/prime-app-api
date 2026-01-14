package org.exp.primeapp.service.face.admin.inventory;

import org.exp.primeapp.models.dto.request.InventoryTransactionRequest;
import org.exp.primeapp.models.dto.response.InventoryTransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface InventoryTransactionService {

    /**
     * Create inventory transaction (unified for both IN and OUT)
     * Returns response DTO with product snapshot information
     */
    InventoryTransactionResponse createTransaction(InventoryTransactionRequest request);

    /**
     * Get transaction by ID
     * Returns response DTO with full transaction details
     */
    InventoryTransactionResponse getTransactionById(Long id);

    /**
     * Get all transactions with filtering
     * All filtering logic is handled in the service layer
     */
    Page<InventoryTransactionResponse> getAllTransactions(
            String type,
            String reason,
            Long productId,
            Long customerId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable);
}
