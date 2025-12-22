package org.exp.primeapp.botadmin.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryCreationState {
    
    public enum Step {
        WAITING_NAME,
        WAITING_SPOTLIGHT_NAME,
        CONFIRMATION
    }
    
    private Step currentStep;
    private String name;
    private String spotlightName;
    private Long userId;
    
    public static CategoryCreationState createInitial(Long userId) {
        return CategoryCreationState.builder()
                .currentStep(Step.WAITING_NAME)
                .userId(userId)
                .build();
    }
}

