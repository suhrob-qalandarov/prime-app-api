package org.exp.primeapp.service.impl.admin.inventory;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.models.dto.request.IncomeRequest;
import org.exp.primeapp.models.dto.response.admin.IncomeStatisticsResponse;
import org.exp.primeapp.models.entities.Category;
import org.exp.primeapp.models.entities.Product;
import org.exp.primeapp.models.entities.ProductIncome;
import org.exp.primeapp.models.entities.ProductSize;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.models.enums.IncomeFilterType;
import org.exp.primeapp.repository.CategoryRepository;
import org.exp.primeapp.repository.ProductIncomeRepository;
import org.exp.primeapp.repository.ProductRepository;
import org.exp.primeapp.repository.ProductSizeRepository;
import org.exp.primeapp.service.face.admin.inventory.ProductIncomeService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
                            .quantity(0)
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
        // Agar totalIncomeStockPrice null bo'lmasa va oneStockPrice null bo'lsa,
        // oneStockPrice ni hisobla
        else if (oneStockPrice == null && totalIncomeStockPrice != null) {
            oneStockPrice = totalIncomeStockPrice.divide(BigDecimal.valueOf(incomeRequest.incomeStock()), 2,
                    RoundingMode.HALF_UP);
        }
        // Agar ikkalasi ham null bo'lsa, default qiymatlar
        else if (oneStockPrice == null && totalIncomeStockPrice == null) {
            oneStockPrice = BigDecimal.ZERO;
            totalIncomeStockPrice = BigDecimal.ZERO;
        }

        // isCalculated ni hisoblash: sellingPrice va (oneStockPrice yoki
        // totalIncomeStockPrice) bo'lsa true
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

        // ProductSize quantity ni yangilash
        productSize.setQuantity(productSize.getQuantity() + incomeRequest.incomeStock());
        productSize.setCostPrice(oneStockPrice);
        productSizeRepository.save(productSize);

        // Agar sellingPrice null bo'lmasa va canSetPriceToProduct=true bo'lsa, product
        // price ni yangilash
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

            // Product ni active qilish (ON_SALE ga o'tkazish)
            if (product.getStatus() != org.exp.primeapp.models.enums.ProductStatus.ON_SALE) {
                product.setStatus(org.exp.primeapp.models.enums.ProductStatus.ON_SALE);
                productRepository.save(product);
                log.info("Product {} activated/set to ON_SALE due to income", product.getId());
            }

            // Category ni active qilish (VISIBLE ga o'tkazish)
            Category category = product.getCategory();
            if (category != null && category.getStatus() != org.exp.primeapp.models.enums.CategoryStatus.VISIBLE) {
                category.setStatus(org.exp.primeapp.models.enums.CategoryStatus.VISIBLE);
                categoryRepository.save(category);
                log.info("Category {} activated/set to VISIBLE due to product income", category.getId());
            }
        } catch (Exception e) {
            log.error("Error activating product and category for ProductIncome {}", productIncome.getId(), e);
        }
    }

    @Override
    public IncomeStatisticsResponse getIncomeStatistics(IncomeFilterType filterType) {
        LocalDateTime startDate;
        LocalDateTime endDate;
        String periodStart;
        String periodEnd;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        LocalDateTime now = LocalDateTime.now();

        switch (filterType) {
            case TODAY:
                startDate = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
                endDate = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
                periodStart = startDate.format(formatter);
                periodEnd = endDate.format(formatter);
                break;
            case WEEKLY:
                startDate = now.minusDays(7).with(LocalTime.MIN);
                endDate = now.with(LocalTime.MAX);
                periodStart = startDate.format(formatter);
                periodEnd = endDate.format(formatter);
                break;
            case MONTHLY:
                startDate = now.minusDays(30).with(LocalTime.MIN);
                endDate = now.with(LocalTime.MAX);
                periodStart = startDate.format(formatter);
                periodEnd = endDate.format(formatter);
                break;
            default:
                startDate = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
                endDate = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
                periodStart = startDate.format(formatter);
                periodEnd = endDate.format(formatter);
        }

        // Income larni olish
        List<ProductIncome> incomes = productIncomeRepository.findIncomesByDateRange(startDate, endDate);

        // Statistikalarni hisoblash
        long totalCount = incomes.size();
        int totalStockQuantity = incomes.stream()
                .mapToInt(ProductIncome::getStockQuantity)
                .sum();

        BigDecimal totalIncomeAmount = incomes.stream()
                .map(ProductIncome::getTotalPrice)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalUnitPrice = incomes.stream()
                .map(ProductIncome::getUnitPrice)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageUnitPrice = totalCount > 0 && totalUnitPrice.compareTo(BigDecimal.ZERO) > 0
                ? totalUnitPrice.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return IncomeStatisticsResponse.builder()
                .totalCount(totalCount)
                .totalStockQuantity(totalStockQuantity)
                .totalIncomeAmount(totalIncomeAmount)
                .totalUnitPrice(totalUnitPrice)
                .averageUnitPrice(averageUnitPrice)
                .incomes(incomes)
                .filterType(filterType.name())
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .build();
    }
}
