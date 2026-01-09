package org.exp.primeapp.service.impl.user;

import lombok.RequiredArgsConstructor;
import org.exp.primeapp.models.dto.request.CategoryReq;
import org.exp.primeapp.models.dto.responce.admin.AdminCategoryDashboardRes;
import org.exp.primeapp.models.dto.responce.user.CategoryRes;
import org.exp.primeapp.models.dto.responce.admin.AdminCategoryRes;
import org.exp.primeapp.models.entities.Category;
import org.exp.primeapp.models.enums.CategoryStatus;
import org.exp.primeapp.repository.CategoryRepository;
import org.exp.primeapp.repository.ProductRepository;
import org.exp.primeapp.service.face.user.CategoryService;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    @Value("${app.categories.update-offset-minutes}")
    private long updateOffsetMinutes;

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy");

    @Override
    public List<CategoryRes> getResCategoriesBySpotlightName(String spotlightName) {
        return categoryRepository
                .findBySpotlightNameAndStatusOrderByOrderNumberAsc(spotlightName, CategoryStatus.ACTIVE)
                .stream()
                .map(category -> new CategoryRes(
                        category.getId(),
                        category.getName(),
                        category.getSpotlightName()))
                .toList();
    }

    @Override
    public List<CategoryRes> getCategoriesResByStatuses(List<CategoryStatus> statuses) {
        return categoryRepository.findByStatusInOrderByOrderNumberAsc(statuses).stream()
                .map(this::convertToCategoryRes)
                .toList();
    }

    public List<CategoryRes> getResCategories() {
        return categoryRepository.findByStatusOrderByOrderNumberAsc(CategoryStatus.ACTIVE)
                .stream()
                .map(category -> new CategoryRes(
                        category.getId(),
                        category.getName(),
                        category.getSpotlightName()))
                .toList();
    }

    @Override
    public AdminCategoryRes getAdminCategoryResById(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with telegramId: " + categoryId));
        return convertToAdminCategoryRes(category);
    }

    public AdminCategoryDashboardRes getCategoryDashboardRes() {
        List<AdminCategoryRes> categoryResList = categoryRepository.findAllByOrderByOrderNumberAsc().stream()
                .map(this::convertToAdminCategoryRes)
                .toList();

        long totalCount = categoryResList.size();
        long createdCount = categoryResList.stream().filter(c -> CategoryStatus.CREATED.name().equals(c.status())).count();
        long activeCount = categoryResList.stream().filter(c -> CategoryStatus.ACTIVE.name().equals(c.status())).count();
        long inactiveCount = categoryResList.stream().filter(c -> CategoryStatus.INACTIVE.name().equals(c.status())).count();

        return AdminCategoryDashboardRes.builder()
                .totalCount(totalCount)
                .createdCount(createdCount)
                .activeCount(activeCount)
                .inactiveCount(inactiveCount)
                .responseDate(LocalDateTime.now().plusMinutes(updateOffsetMinutes))
                .categories(categoryResList)
                .build();
    }

    @Override
    public AdminCategoryRes saveCategory(@NonNull CategoryReq categoryReq) {
        // Calculate orderNumber - get max orderNumber and add 1, or start from 1 if no categories exist
        Long orderNumber = 1L;
        List<Category> allCategories = categoryRepository.findAllByOrderByOrderNumberAsc();
        if (!allCategories.isEmpty()) {
            Long maxOrderNumber = allCategories.stream()
                    .map(Category::getOrderNumber)
                    .max(Long::compareTo)
                    .orElse(0L);
            orderNumber = maxOrderNumber + 1;
        }

        Category saved = categoryRepository.save(
                Category.builder()
                        .name(categoryReq.name())
                        .spotlightName(
                                categoryReq.spotlightName() != null && !categoryReq.spotlightName().isBlank()
                                        ? categoryReq.spotlightName()
                                        : null)
                        .orderNumber(orderNumber)
                        .build());
        System.out.println("Category saved successfully");
        return convertToAdminCategoryRes(saved);
    }

    @Transactional
    @Override
    public AdminCategoryRes updateCategoryById(Long categoryId, @NonNull CategoryReq categoryReq) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(RuntimeException::new);
        category.setName(categoryReq.name());
        category.setSpotlightName(categoryReq.spotlightName());
        Category saved = categoryRepository.save(category);
        System.out.println("Category updated successfully");
        return convertToAdminCategoryRes(saved);
    }

    @Override
    public AdminCategoryRes toggleCategoryStatus(Long categoryId, Boolean withProducts) {
        if (withProducts) {
            categoryRepository.toggleCategoryStatusWithProducts_Category(categoryId);
            categoryRepository.toggleCategoryStatusWithProducts_Products(categoryId);
        } else {
            categoryRepository.toggleCategoryStatusOnly(categoryId);
        }
        return convertToAdminCategoryRes(categoryRepository.findById(categoryId).orElseThrow(RuntimeException::new));
    }

    @Transactional
    @Override
    public List<AdminCategoryRes> updateCategoryOrder(@NonNull Map<Long, Long> categoryOrderMap) {
        List<Category> categories = categoryRepository.findAllById(categoryOrderMap.keySet());

        for (Category category : categories) {
            Long newOrder = categoryOrderMap.get(category.getId());
            category.setOrderNumber(newOrder);
        }

        return categoryRepository.saveAll(categories).stream().map(this::convertToAdminCategoryRes).toList();
    }

    public CategoryRes convertToCategoryRes(@NonNull Category category) {
        return new CategoryRes(
                category.getId(),
                category.getName(),
                category.getSpotlightName());
    }

    public AdminCategoryRes convertToAdminCategoryRes(@NonNull Category category) {
        long countByCategory = productRepository.countByCategoryId(category.getId());
        return new AdminCategoryRes(
                category.getId(),
                category.getName(),
                category.getSpotlightName(),
                category.getOrderNumber(),
                category.getStatus().name(),
                countByCategory,
                category.getCreatedAt() != null ? category.getCreatedAt().format(formatter) : null);
    }
}
