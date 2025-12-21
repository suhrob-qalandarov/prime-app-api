package org.exp.primeapp.botauth.models;

import lombok.Builder;
import lombok.Data;
import org.exp.primeapp.models.entities.Category;
import org.exp.primeapp.models.enums.Size;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ProductCreationState {
    
    public enum Step {
        WAITING_NAME,
        WAITING_DESCRIPTION,
        WAITING_BRAND,
        WAITING_IMAGES,
        WAITING_SPOTLIGHT_NAME,
        WAITING_CATEGORY,
        WAITING_SIZES,
        WAITING_QUANTITIES,
        CONFIRMATION
    }
    
    private Step currentStep;
    private String name;
    private String description;
    private String brand;
    private List<String> imageFileIds; // Telegram file_id lar
    private List<String> attachmentUrls; // Saqlangan attachment URL lar
    private String spotlightName; // Selected spotlight name
    private Category category;
    private List<Size> selectedSizes;
    private Map<Size, Integer> sizeQuantities; // Size -> quantity
    private Long userId;
    
    public static ProductCreationState createInitial(Long userId) {
        return ProductCreationState.builder()
                .currentStep(Step.WAITING_NAME)
                .imageFileIds(new ArrayList<>())
                .attachmentUrls(new ArrayList<>())
                .selectedSizes(new ArrayList<>())
                .sizeQuantities(new HashMap<>())
                .userId(userId)
                .build();
    }
    
    public void addImageFileId(String fileId) {
        if (imageFileIds == null) {
            imageFileIds = new ArrayList<>();
        }
        imageFileIds.add(fileId);
    }
    
    public void addAttachmentUrl(String url) {
        if (attachmentUrls == null) {
            attachmentUrls = new ArrayList<>();
        }
        attachmentUrls.add(url);
    }
    
    public boolean hasMinimumImages() {
        return attachmentUrls != null && attachmentUrls.size() >= 1;
    }
    
    public boolean canAddMoreImages() {
        return attachmentUrls == null || attachmentUrls.size() < 3;
    }
}


