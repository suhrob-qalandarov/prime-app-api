package org.exp.primeapp.repository;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.models.entities.Category;
import org.exp.primeapp.models.entities.InventoryTransaction;
import org.exp.primeapp.models.entities.Product;
import org.exp.primeapp.models.enums.CategoryStatus;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class InventoryTransactionRepositoryImpl implements InventoryTransactionRepositoryCustom {

    private final EntityManager entityManager;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public InventoryTransaction saveWithActivation(InventoryTransaction productIncome) {
        // Warehouse ni saqlash
        InventoryTransaction savedIncome = entityManager.merge(productIncome);
        entityManager.flush();

        // Product va Category ni active qilish
        activateProductAndCategory(savedIncome);

        return savedIncome;
    }

    private void activateProductAndCategory(InventoryTransaction productIncome) {
        try {
            Product product = productIncome.getProduct();
            if (product == null) {
                log.warn("Warehouse has no product, skipping activation");
                return;
            }

            // Product ni active qilish
            if (product.getStatus() != org.exp.primeapp.models.enums.ProductStatus.ON_SALE) {
                product.setStatus(org.exp.primeapp.models.enums.ProductStatus.ON_SALE);
                productRepository.save(product);
                log.info("Product {} activated due to income", product.getId());
            }

            // Category ni active qilish
            Category category = product.getCategory();
            if (category != null && category.getStatus() != CategoryStatus.ACTIVE) {
                category.setStatus(CategoryStatus.ACTIVE);
                categoryRepository.save(category);
                log.info("Category {} activated due to product income", category.getId());
            }
        } catch (Exception e) {
            log.error("Error activating product and category for Warehouse {}", productIncome.getId(), e);
        }
    }
}
