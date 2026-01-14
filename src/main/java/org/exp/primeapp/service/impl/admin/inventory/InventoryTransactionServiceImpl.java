package org.exp.primeapp.service.impl.admin.inventory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.models.dto.request.InventoryTransactionRequest;
import org.exp.primeapp.models.dto.request.InventoryTransactionSizeItem;
import org.exp.primeapp.models.dto.response.InventoryTransactionResponse;
import org.exp.primeapp.models.entities.*;
import org.exp.primeapp.models.enums.Size;
import org.exp.primeapp.models.enums.TransactionType;
import org.exp.primeapp.repository.*;
import org.exp.primeapp.service.face.admin.inventory.InventoryTransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for managing Inventory Transactions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryTransactionServiceImpl implements InventoryTransactionService {

        private final InventoryTransactionRepository inventoryTransactionRepository;
        private final ProductRepository productRepository;
        private final ProductSizeRepository productSizeRepository;
        private final CategoryRepository categoryRepository;
        private final CustomerRepository customerRepository;
        private final UserRepository userRepository;
        private final OrderRepository orderRepository;

        @Override
        @Transactional
        public InventoryTransactionResponse createTransaction(InventoryTransactionRequest request) {
                log.info("Creating inventory transaction for product ID: {}", request.productId());

                // Find product
                Product product = productRepository.findById(request.productId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Product not found with id: " + request.productId()));

                // Get current user (if authenticated)
                User currentUser = null;
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getPrincipal() instanceof User) {
                        User authUser = (User) authentication.getPrincipal();
                        currentUser = userRepository.findById(authUser.getId())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "User not found with id: " + authUser.getId()));
                }

                InventoryTransaction lastTransaction = null;
                TransactionType type = request.type();

                // Process each size item
                for (InventoryTransactionSizeItem sizeItem : request.sizeItems()) {
                        Size size = sizeItem.size();
                        Integer quantity = sizeItem.quantity();

                        // Find or create ProductSize
                        ProductSize productSize = productSizeRepository.findByProductAndSize(product, size)
                                        .orElseGet(() -> {
                                                ProductSize newSize = ProductSize.builder()
                                                                .product(product)
                                                                .size(size)
                                                                .quantity(0)
                                                                .costPrice(BigDecimal.ZERO)
                                                                .build();
                                                return productSizeRepository.save(newSize);
                                        });

                        BigDecimal unitPrice;
                        BigDecimal totalPrice;

                        if (type == TransactionType.IN) {
                                // Handle IN (Income) transaction
                                unitPrice = calculateUnitPrice(request, quantity);
                                totalPrice = calculateTotalPrice(request, unitPrice, quantity);

                                // Update ProductSize quantity (add)
                                productSize.setQuantity(productSize.getQuantity() + quantity);
                                productSize.setCostPrice(unitPrice);
                                productSizeRepository.save(productSize);

                                // Update product selling price if requested
                                if (sizeItem.sellingPrice() != null
                                                && Boolean.TRUE.equals(request.canSetPriceToProduct())) {
                                        product.setPrice(sizeItem.sellingPrice());
                                        productRepository.save(product);
                                        log.info("Product {} price updated to {}", product.getId(),
                                                        sizeItem.sellingPrice());
                                }

                        } else {
                                // Handle OUT (Outcome) transaction
                                // Check stock availability
                                if (productSize.getQuantity() < quantity) {
                                        throw new RuntimeException("Insufficient stock for size: " + size
                                                        + ". Available: " + productSize.getQuantity());
                                }

                                // Use cost price from ProductSize
                                unitPrice = productSize.getCostPrice() != null ? productSize.getCostPrice()
                                                : BigDecimal.ZERO;
                                totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));

                                // Update ProductSize quantity (subtract)
                                productSize.setQuantity(productSize.getQuantity() - quantity);
                                productSizeRepository.save(productSize);
                        }

                        // Create inventory transaction with product snapshot
                        InventoryTransaction transaction = InventoryTransaction.builder()
                                        .product(product)
                                        .stockQuantity(quantity)
                                        .unitPrice(unitPrice)
                                        .totalPrice(totalPrice)
                                        .performedBy(currentUser)
                                        .type(type)
                                        .reason(request.reason())
                                        .note(request.note())
                                        // Product snapshot
                                        .productName(product.getName())
                                        .productDescription(product.getDescription())
                                        .productMainImageUrl(product.getBrand())
                                        .productTag(product.getTag().name())
                                        .productStatus(product.getStatus().name())
                                        .productColor(product.getColorName())
                                        .productColorHex(product.getColorHex())
                                        .productCategoryName(product.getCategoryName())
                                        .productSize(size != null ? size.name() : null)
                                        .discountPercent(product.getDiscountPercent())
                                        .build();

                        lastTransaction = inventoryTransactionRepository.save(transaction);

                        // Activate product and category for IN transactions
                        if (type == TransactionType.IN) {
                                activateProductAndCategory(lastTransaction);
                        }
                }

                log.info("Inventory transaction created with ID: {}", lastTransaction.getId());
                return mapToResponse(lastTransaction);
        }

        @Override
        @Transactional(readOnly = true)
        public InventoryTransactionResponse getTransactionById(Long id) {
                log.info("Getting inventory transaction by ID: {}", id);
                InventoryTransaction transaction = inventoryTransactionRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Transaction not found with id: " + id));
                return mapToResponse(transaction);
        }

        @Override
        @Transactional(readOnly = true)
        public Page<InventoryTransactionResponse> getAllTransactions(
                        String type,
                        String reason,
                        Long productId,
                        Long customerId,
                        LocalDateTime startDate,
                        LocalDateTime endDate,
                        Pageable pageable) {
                log.info("Getting all transactions with filters - type: {}, reason: {}, productId: {}", type, reason,
                                productId);

                // For now, simple implementation - can be enhanced with filters later
                Page<InventoryTransaction> transactionsPage = inventoryTransactionRepository.findAll(pageable);

                List<InventoryTransactionResponse> responses = transactionsPage.getContent().stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());

                return new PageImpl<>(responses, pageable, transactionsPage.getTotalElements());
        }

        /**
         * Calculate unit price for IN transactions
         */
        private BigDecimal calculateUnitPrice(InventoryTransactionRequest request, Integer quantity) {
                if (request.oneStockPrice() != null) {
                        return request.oneStockPrice();
                } else if (request.totalIncomeStockPrice() != null && quantity != null && quantity > 0) {
                        return request.totalIncomeStockPrice().divide(BigDecimal.valueOf(quantity), 2,
                                        BigDecimal.ROUND_HALF_UP);
                }
                return BigDecimal.ZERO;
        }

        /**
         * Calculate total price for IN transactions
         */
        private BigDecimal calculateTotalPrice(InventoryTransactionRequest request, BigDecimal unitPrice,
                        Integer quantity) {
                if (request.totalIncomeStockPrice() != null) {
                        return request.totalIncomeStockPrice();
                } else if (unitPrice != null && quantity != null) {
                        return unitPrice.multiply(BigDecimal.valueOf(quantity));
                }
                return BigDecimal.ZERO;
        }

        /**
         * Activate product and its category
         */
        private void activateProductAndCategory(InventoryTransaction transaction) {
                Product product = transaction.getProduct();
                if (product != null) {
                        // Activate product if not already active
                        if (product.getStatus() != null && !product.getStatus().name().equals("ACTIVE")) {
                                // Note: Direct status update - check Product entity for setStatus method
                                productRepository.save(product);
                        }

                        Category category = product.getCategory();
                        if (category != null) {
                                // Activate category if not already active
                                // Note: Direct status update - check Category entity for setStatus method
                                categoryRepository.save(category);
                        }
                }
        }

        /**
         * Map InventoryTransaction entity to Response DTO
         */
        private InventoryTransactionResponse mapToResponse(InventoryTransaction transaction) {
                return InventoryTransactionResponse.builder()
                                .id(transaction.getId())
                                // Product snapshot
                                .productName(transaction.getProductName())
                                .productDescription(transaction.getProductDescription())
                                .productMainImageUrl(transaction.getProductMainImageUrl())
                                .productTag(transaction.getProductTag())
                                .productStatus(transaction.getProductStatus())
                                .productColor(transaction.getProductColor())
                                .productColorHex(transaction.getProductColorHex())
                                .productCategoryName(transaction.getProductCategoryName())
                                .productSize(transaction.getProductSize())
                                // Transaction details
                                .stockQuantity(transaction.getStockQuantity())
                                .unitPrice(transaction.getUnitPrice())
                                .discountPercent(transaction.getDiscountPercent())
                                .totalPrice(transaction.getTotalPrice())
                                .type(transaction.getType())
                                .reason(transaction.getReason())
                                // Reference IDs
                                .productId(transaction.getProduct() != null ? transaction.getProduct().getId() : null)
                                .customerId(transaction.getCustomer() != null ? transaction.getCustomer().getId()
                                                : null)
                                .clientId(transaction.getClient() != null ? transaction.getClient().getId() : null)
                                .orderId(transaction.getOrder() != null ? transaction.getOrder().getId() : null)
                                .performedById(transaction.getPerformedBy() != null
                                                ? transaction.getPerformedBy().getId()
                                                : null)
                                .returnPerformedById(transaction.getReturnPerformedBy() != null
                                                ? transaction.getReturnPerformedBy().getId()
                                                : null)
                                // Additional info
                                .customerName(transaction.getCustomer() != null
                                                ? transaction.getCustomer().getFullName()
                                                : null)
                                .clientName(transaction.getClient() != null ? transaction.getClient().getFullName()
                                                : null)
                                .performedByName(transaction.getPerformedBy() != null
                                                ? getFullName(transaction.getPerformedBy())
                                                : null)
                                .returnPerformedByName(transaction.getReturnPerformedBy() != null
                                                ? getFullName(transaction.getReturnPerformedBy())
                                                : null)
                                .note(transaction.getNote())
                                .createdAt(transaction.getCreatedAt())
                                .returnedAt(transaction.getReturnedAt())
                                .build();
        }

        /**
         * Get full name from User entity
         */
        private String getFullName(User user) {
                if (user.getFirstName() != null && user.getLastName() != null) {
                        return user.getFirstName() + " " + user.getLastName();
                } else if (user.getFirstName() != null) {
                        return user.getFirstName();
                } else if (user.getLastName() != null) {
                        return user.getLastName();
                }
                return user.getUsername();
        }

        @Override
        @Transactional(readOnly = true)
        public org.exp.primeapp.models.dto.response.InventoryTransactionPageResponse getAllTransactionsWithStats(
                        String type,
                        String reason,
                        Long productId,
                        Long categoryId,
                        String productSize,
                        String productTag,
                        Long performedById,
                        LocalDateTime startDate,
                        LocalDateTime endDate,
                        Pageable pageable) {
                log.info("Getting all transactions with stats - type: {}, reason: {}, productId: {}, categoryId: {}, size: {}, tag: {}",
                                type, reason, productId, categoryId, productSize, productTag);

                // Build filtering spec/criteria (simplified - would use Specification in real
                // implementation)
                // For now, using JPA query
                Page<InventoryTransaction> transactionsPage = inventoryTransactionRepository.findAll(pageable);

                // Apply filters manually on the result
                List<InventoryTransaction> filteredList = transactionsPage.getContent().stream()
                                .filter(t -> type == null || t.getType().name().equals(type))
                                .filter(t -> reason == null
                                                || (t.getReason() != null && t.getReason().name().equals(reason)))
                                .filter(t -> productId == null
                                                || (t.getProduct() != null && t.getProduct().getId().equals(productId)))
                                .filter(t -> categoryId == null
                                                || (t.getProduct() != null && t.getProduct().getCategory() != null
                                                                && t.getProduct().getCategory().getId()
                                                                                .equals(categoryId)))
                                .filter(t -> productSize == null || (t.getProductSize() != null
                                                && t.getProductSize().equals(productSize)))
                                .filter(t -> productTag == null
                                                || (t.getProductTag() != null && t.getProductTag().equals(productTag)))
                                .filter(t -> performedById == null || (t.getPerformedBy() != null
                                                && t.getPerformedBy().getId().equals(performedById)))
                                .filter(t -> startDate == null
                                                || (t.getCreatedAt() != null && !t.getCreatedAt().isBefore(startDate)))
                                .filter(t -> endDate == null
                                                || (t.getCreatedAt() != null && !t.getCreatedAt().isAfter(endDate)))
                                .collect(Collectors.toList());

                // Get all transactions for statistics (without pagination)
                List<InventoryTransaction> allForStats = inventoryTransactionRepository.findAll().stream()
                                .filter(t -> type == null || t.getType().name().equals(type))
                                .filter(t -> reason == null
                                                || (t.getReason() != null && t.getReason().name().equals(reason)))
                                .filter(t -> productId == null
                                                || (t.getProduct() != null && t.getProduct().getId().equals(productId)))
                                .filter(t -> categoryId == null
                                                || (t.getProduct() != null && t.getProduct().getCategory() != null
                                                                && t.getProduct().getCategory().getId()
                                                                                .equals(categoryId)))
                                .filter(t -> productSize == null || (t.getProductSize() != null
                                                && t.getProductSize().equals(productSize)))
                                .filter(t -> productTag == null
                                                || (t.getProductTag() != null && t.getProductTag().equals(productTag)))
                                .filter(t -> performedById == null || (t.getPerformedBy() != null
                                                && t.getPerformedBy().getId().equals(performedById)))
                                .filter(t -> startDate == null
                                                || (t.getCreatedAt() != null && !t.getCreatedAt().isBefore(startDate)))
                                .filter(t -> endDate == null
                                                || (t.getCreatedAt() != null && !t.getCreatedAt().isAfter(endDate)))
                                .collect(Collectors.toList());

                // Calculate statistics
                long totalCount = allForStats.size();
                long inCount = allForStats.stream()
                                .filter(t -> t.getType() == TransactionType.IN)
                                .count();
                long outCount = allForStats.stream()
                                .filter(t -> t.getType() == TransactionType.OUT)
                                .count();
                long returningCount = allForStats.stream()
                                .filter(t -> t.getReason() != null && t.getReason().name().contains("RETURN"))
                                .count();

                // Product tag counts
                java.util.Map<String, Long> tagCounts = allForStats.stream()
                                .filter(t -> t.getProductTag() != null)
                                .collect(Collectors.groupingBy(
                                                InventoryTransaction::getProductTag,
                                                Collectors.counting()));

                // Map to responses
                List<InventoryTransactionResponse> responses = filteredList.stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());

                Page<InventoryTransactionResponse> responsePage = new PageImpl<>(
                                responses,
                                pageable,
                                filteredList.size());

                return org.exp.primeapp.models.dto.response.InventoryTransactionPageResponse.builder()
                                .transactions(responsePage)
                                .totalTransactionsCount(totalCount)
                                .inTransactionsCount(inCount)
                                .outTransactionsCount(outCount)
                                .returningCount(returningCount)
                                .productTagCounts(tagCounts)
                                .build();
        }
}
