package org.exp.primeapp.service.impl.admin.inventory;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.models.dto.request.IncomeRequest;
import org.exp.primeapp.models.entities.Category;
import org.exp.primeapp.models.entities.Product;
import org.exp.primeapp.models.entities.ProductIncome;
import org.exp.primeapp.models.entities.ProductSize;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.repository.CategoryRepository;
import org.exp.primeapp.repository.ProductIncomeRepository;
import org.exp.primeapp.repository.ProductRepository;
import org.exp.primeapp.repository.ProductSizeRepository;
import org.exp.primeapp.service.face.admin.inventory.ProductIncomeService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductIncomeServiceImpl implements ProductIncomeService {

    private final ProductIncomeRepository productIncomeRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductSizeRepository productSizeRepository;

    @Override
    @Transactional
    public ProductIncome save(ProductIncome productIncome) {
        // ProductIncome ni saqlash
        ProductIncome savedIncome = productIncomeRepository.save(productIncome);

        // Product va Category ni active qilish
        activateProductAndCategory(savedIncome);

        return savedIncome;
    }

    @Override
    @Transactional
    public ProductIncome createIncome(IncomeRequest incomeRequest) {
        // Product ni topish
        Product product = productRepository.findById(incomeRequest.productId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + incomeRequest.productId()));

        // ProductSize ni topish yoki yaratish
        ProductSize productSize = productSizeRepository.findByProductAndSize(product, incomeRequest.size())
                .orElseGet(() -> {
                    ProductSize newSize = ProductSize.builder()
                            .product(product)
                            .size(incomeRequest.size())
                            .amount(0)
                            .costPrice(BigDecimal.ZERO)
                            .build();
                    return productSizeRepository.save(newSize);
                });

        // Narxlarni hisoblash
        BigDecimal oneStockPrice = incomeRequest.oneStockPrice();
        BigDecimal totalIncomeStockPrice = incomeRequest.totalIncomeStockPrice();

        // Agar oneStockPrice null bo'lmasa, totalIncomeStockPrice ni hisobla
        if (oneStockPrice != null && totalIncomeStockPrice == null) {
            totalIncomeStockPrice = oneStockPrice.multiply(BigDecimal.valueOf(incomeRequest.incomeStock()))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        // Agar totalIncomeStockPrice null bo'lmasa va oneStockPrice null bo'lsa, oneStockPrice ni hisobla
        else if (oneStockPrice == null && totalIncomeStockPrice != null) {
            oneStockPrice = totalIncomeStockPrice.divide(BigDecimal.valueOf(incomeRequest.incomeStock()), 2, RoundingMode.HALF_UP);
        }
        // Agar ikkalasi ham null bo'lsa, default qiymatlar
        else if (oneStockPrice == null && totalIncomeStockPrice == null) {
            oneStockPrice = BigDecimal.ZERO;
            totalIncomeStockPrice = BigDecimal.ZERO;
        }

        // isCalculated ni hisoblash: sellingPrice va (oneStockPrice yoki totalIncomeStockPrice) bo'lsa true
        boolean isCalculated = incomeRequest.sellingPrice() != null 
                && (oneStockPrice != null && !oneStockPrice.equals(BigDecimal.ZERO) 
                    || totalIncomeStockPrice != null && !totalIncomeStockPrice.equals(BigDecimal.ZERO));

        // ProductIncome yaratish
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        ProductIncome productIncome = ProductIncome.builder()
                .product(product)
                .stockQuantity(incomeRequest.incomeStock())
                .unitPrice(oneStockPrice)
                .totalPrice(totalIncomeStockPrice)
                .sellingPrice(incomeRequest.sellingPrice())
                .userAdmin(currentUser)
                .isCalculated(isCalculated)
                .build();

        ProductIncome savedIncome = productIncomeRepository.save(productIncome);

        // ProductSize amount ni yangilash
        productSize.setAmount(productSize.getAmount() + incomeRequest.incomeStock());
        productSize.setCostPrice(oneStockPrice);
        productSizeRepository.save(productSize);

        // Agar sellingPrice null bo'lmasa va canSetPriceToProduct=true bo'lsa, product price ni yangilash
        if (incomeRequest.sellingPrice() != null && Boolean.TRUE.equals(incomeRequest.canSetPriceToProduct())) {
            product.setPrice(incomeRequest.sellingPrice());
            productRepository.save(product);
            log.info("Product {} price updated to {}", product.getId(), incomeRequest.sellingPrice());
        }

        // Product va Category ni active qilish
        activateProductAndCategory(savedIncome);

        return savedIncome;
    }

    @Transactional
    public void activateProductAndCategory(ProductIncome productIncome) {
        try {
            Product product = productIncome.getProduct();
            if (product == null) {
                log.warn("ProductIncome has no product, skipping activation");
                return;
            }

            // Product ni active qilish
            if (!product.getActive()) {
                product.setActive(true);
                productRepository.save(product);
                log.info("Product {} activated due to income", product.getId());
            }

            // Category ni active qilish
            Category category = product.getCategory();
            if (category != null && !category.getActive()) {
                category.setActive(true);
                categoryRepository.save(category);
                log.info("Category {} activated due to product income", category.getId());
            }
        } catch (Exception e) {
            log.error("Error activating product and category for ProductIncome {}", productIncome.getId(), e);
        }
    }
}

