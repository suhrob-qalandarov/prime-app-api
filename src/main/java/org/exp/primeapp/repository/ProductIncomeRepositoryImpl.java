package org.exp.primeapp.repository;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.models.entities.Category;
import org.exp.primeapp.models.entities.Product;
import org.exp.primeapp.models.entities.ProductIncome;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ProductIncomeRepositoryImpl implements ProductIncomeRepositoryCustom {

    private final EntityManager entityManager;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public ProductIncome saveWithActivation(ProductIncome productIncome) {
        // ProductIncome ni saqlash
        ProductIncome savedIncome = entityManager.merge(productIncome);
        entityManager.flush();

        // Product va Category ni active qilish
        activateProductAndCategory(savedIncome);

        return savedIncome;
    }

    private void activateProductAndCategory(ProductIncome productIncome) {
        try {
            Product product = productIncome.getProduct();
            if (product == null) {
                log.warn("ProductIncome has no product, skipping activation");
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
            if (category != null && category.getStatus() != org.exp.primeapp.models.enums.CategoryStatus.VISIBLE) {
                category.setStatus(org.exp.primeapp.models.enums.CategoryStatus.VISIBLE);
                categoryRepository.save(category);
                log.info("Category {} activated due to product income", category.getId());
            }
        } catch (Exception e) {
            log.error("Error activating product and category for ProductIncome {}", productIncome.getId(), e);
        }
    }
}
