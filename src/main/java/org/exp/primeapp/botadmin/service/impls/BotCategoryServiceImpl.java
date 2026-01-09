package org.exp.primeapp.botadmin.service.impls;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.botadmin.models.CategoryCreationState;
import org.exp.primeapp.botadmin.service.interfaces.BotCategoryService;
import org.exp.primeapp.models.dto.request.CategoryReq;
import org.exp.primeapp.service.face.user.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
// @Service // Temporarily disabled
@RequiredArgsConstructor
public class BotCategoryServiceImpl implements BotCategoryService {

    private final Map<Long, CategoryCreationState> categoryCreationStates = new ConcurrentHashMap<>();
    private final CategoryService categoryService;

    @Override
    public void startCategoryCreation(Long userId) {
        CategoryCreationState state = CategoryCreationState.createInitial(userId);
        categoryCreationStates.put(userId, state);
    }

    @Override
    public CategoryCreationState getCategoryCreationState(Long userId) {
        return categoryCreationStates.get(userId);
    }

    @Override
    public void clearCategoryCreationState(Long userId) {
        categoryCreationStates.remove(userId);
    }

    @Override
    public void handleCategoryName(Long userId, String name) {
        CategoryCreationState state = getCategoryCreationState(userId);
        if (state == null || state.getCurrentStep() != CategoryCreationState.Step.WAITING_NAME) {
            return;
        }
        state.setName(name);
        state.setCurrentStep(CategoryCreationState.Step.WAITING_SPOTLIGHT_NAME);
    }

    @Override
    public void handleSpotlightName(Long userId, String spotlightName) {
        CategoryCreationState state = getCategoryCreationState(userId);
        if (state == null || state.getCurrentStep() != CategoryCreationState.Step.WAITING_SPOTLIGHT_NAME) {
            return;
        }
        state.setSpotlightName(spotlightName);
        state.setCurrentStep(CategoryCreationState.Step.CONFIRMATION);
    }

    @Override
    @Transactional
    public void confirmAndSaveCategory(Long userId) {
        CategoryCreationState state = getCategoryCreationState(userId);
        if (state == null) {
            return;
        }

        try {
            // Validate state
            if (state.getName() == null || state.getName().trim().isEmpty()) {
                throw new RuntimeException("Category name is required");
            }

            // Create CategoryReq
            CategoryReq categoryReq = CategoryReq.builder()
                    .name(state.getName())
                    .spotlightName(state.getSpotlightName())
                    .build();

            // Save category using CategoryService
            categoryService.saveCategory(categoryReq);

            // Clear state
            clearCategoryCreationState(userId);
        } catch (Exception e) {
            log.error("Error saving category: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save category: " + e.getMessage());
        }
    }

    @Override
    public void cancelCategoryCreation(Long userId) {
        clearCategoryCreationState(userId);
    }
}
