package org.exp.primeapp.botauth.service.impls;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.File;
import com.pengrad.telegrambot.request.GetFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.botauth.models.ProductCreationState;
import org.exp.primeapp.botauth.service.interfaces.BotProductService;
import org.exp.primeapp.models.dto.request.ProductReq;
import org.exp.primeapp.models.entities.Attachment;
import org.exp.primeapp.models.entities.Category;
import org.exp.primeapp.models.entities.Product;
import org.exp.primeapp.models.entities.ProductSize;
import org.exp.primeapp.models.enums.CategoryStatus;
import org.exp.primeapp.models.enums.Size;
import org.exp.primeapp.repository.AttachmentRepository;
import org.exp.primeapp.repository.CategoryRepository;
import org.exp.primeapp.repository.ProductRepository;
import org.exp.primeapp.service.face.admin.product.AdminProductService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotProductServiceImpl implements BotProductService {

    private final Map<Long, ProductCreationState> productCreationStates = new ConcurrentHashMap<>();
    private final TelegramBot telegramBot;
    private final CategoryRepository categoryRepository;
    private final AttachmentRepository attachmentRepository;
    private final ProductRepository productRepository;
    private final AdminProductService adminProductService;

    @Value("${app.attachment.base.url:${app.api.url:https://api.howdy.uz}}")
    private String attachmentBaseUrl;

    @Value("${app.attachment.folder.path:uploads}")
    private String attachmentFolderPath;

    @Value("${telegram.bot.token:}")
    private String botToken;

    @Override
    public void startProductCreation(Long userId) {
        ProductCreationState state = ProductCreationState.createInitial(userId);
        productCreationStates.put(userId, state);
    }

    @Override
    public ProductCreationState getProductCreationState(Long userId) {
        return productCreationStates.get(userId);
    }

    @Override
    public void clearProductCreationState(Long userId) {
        productCreationStates.remove(userId);
    }

    @Override
    public void handleProductName(Long userId, String name) {
        ProductCreationState state = getProductCreationState(userId);
        if (state == null || state.getCurrentStep() != ProductCreationState.Step.WAITING_NAME) {
            return;
        }
        state.setName(name);
        state.setCurrentStep(ProductCreationState.Step.WAITING_DESCRIPTION);
    }

    @Override
    public void handleProductDescription(Long userId, String description) {
        ProductCreationState state = getProductCreationState(userId);
        if (state == null || state.getCurrentStep() != ProductCreationState.Step.WAITING_DESCRIPTION) {
            return;
        }
        state.setDescription(description);
        state.setCurrentStep(ProductCreationState.Step.WAITING_BRAND);
    }

    @Override
    public void handleProductBrand(Long userId, String brand) {
        ProductCreationState state = getProductCreationState(userId);
        if (state == null || state.getCurrentStep() != ProductCreationState.Step.WAITING_BRAND) {
            return;
        }
        state.setBrand(brand);
        state.setCurrentStep(ProductCreationState.Step.WAITING_IMAGES);
    }

    @Override
    @Transactional
    public void handleProductImage(Long userId, String fileId) {
        ProductCreationState state = getProductCreationState(userId);
        if (state == null || state.getCurrentStep() != ProductCreationState.Step.WAITING_IMAGES) {
            return;
        }

        if (!state.canAddMoreImages()) {
            return; // Already has 3 images
        }

        try {
            // Download file from Telegram
            File file = telegramBot.execute(new GetFile(fileId)).file();
            if (file == null) {
                log.error("File not found for fileId: {}", fileId);
                return;
            }

            String filePath = file.filePath();
            String fileUrl = "https://api.telegram.org/file/bot" + botToken + "/" + filePath;

            // Download and save file
            String savedUrl = downloadAndSaveFile(fileUrl, filePath);
            
            // Create attachment
            Attachment attachment = createAttachment(fileUrl, savedUrl, filePath);
            attachmentRepository.save(attachment);

            state.addImageFileId(fileId);
            state.addAttachmentUrl(savedUrl);

            // If we have at least 1 image, we can proceed (user can add more or continue)
        } catch (Exception e) {
            log.error("Error handling product image: {}", e.getMessage(), e);
        }
    }

    @Override
    public void handleCategorySelection(Long userId, Long categoryId) {
        ProductCreationState state = getProductCreationState(userId);
        if (state == null || state.getCurrentStep() != ProductCreationState.Step.WAITING_CATEGORY) {
            return;
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        state.setCategory(category);
        state.setCurrentStep(ProductCreationState.Step.WAITING_SIZES);
    }

    @Override
    public void handleSizeSelection(Long userId, String sizeName) {
        ProductCreationState state = getProductCreationState(userId);
        if (state == null || state.getCurrentStep() != ProductCreationState.Step.WAITING_SIZES) {
            return;
        }

        try {
            Size size = Size.valueOf(sizeName);
            if (!state.getSelectedSizes().contains(size)) {
                state.getSelectedSizes().add(size);
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid size: {}", sizeName);
        }
    }

    @Override
    public void handleSizeQuantity(Long userId, String sizeName, Integer quantity) {
        ProductCreationState state = getProductCreationState(userId);
        if (state == null) {
            return;
        }

        try {
            Size size = Size.valueOf(sizeName);
            state.getSizeQuantities().put(size, quantity);
        } catch (IllegalArgumentException e) {
            log.error("Invalid size: {}", sizeName);
        }
    }

    @Override
    @Transactional
    public void confirmAndSaveProduct(Long userId) {
        ProductCreationState state = getProductCreationState(userId);
        if (state == null) {
            return;
        }

        try {
            // Validate state
            if (state.getName() == null || state.getDescription() == null || 
                state.getBrand() == null || state.getCategory() == null ||
                state.getAttachmentUrls() == null || state.getAttachmentUrls().isEmpty() ||
                state.getSelectedSizes() == null || state.getSelectedSizes().isEmpty()) {
                throw new RuntimeException("Product data is incomplete");
            }

            // Create ProductReq
            ProductReq productReq = ProductReq.builder()
                    .name(state.getName())
                    .description(state.getDescription())
                    .brand(state.getBrand())
                    .colorName("Default") // You might want to add this to the flow
                    .colorHex("#000000") // You might want to add this to the flow
                    .price(BigDecimal.ZERO) // You might want to add this to the flow
                    .categoryId(state.getCategory().getId())
                    .attachmentUrls(new HashSet<>(state.getAttachmentUrls()))
                    .build();

            // Save product using AdminProductService
            var productRes = adminProductService.saveProduct(productReq);
            
            // Get saved product by ID
            Product savedProduct = productRepository.findById(productRes.id())
                    .orElseThrow(() -> new RuntimeException("Product not found after save"));

            // Add product sizes
            for (Size size : state.getSelectedSizes()) {
                Integer quantity = state.getSizeQuantities().getOrDefault(size, 0);
                if (quantity > 0) {
                    ProductSize productSize = ProductSize.builder()
                            .product(savedProduct)
                            .size(size)
                            .amount(quantity)
                            .costPrice(BigDecimal.ZERO)
                            .build();
                    savedProduct.getSizes().add(productSize);
                }
            }
            productRepository.save(savedProduct);

            // Check category status and update to VISIBLE if it's CREATED
            Category category = state.getCategory();
            if (category != null && category.getStatus() == CategoryStatus.CREATED) {
                category.setStatus(CategoryStatus.VISIBLE);
                categoryRepository.save(category);
                log.info("Category {} status updated from CREATED to VISIBLE", category.getId());
            }

            // Clear state
            clearProductCreationState(userId);
        } catch (Exception e) {
            log.error("Error saving product: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save product: " + e.getMessage());
        }
    }

    @Override
    public void cancelProductCreation(Long userId) {
        clearProductCreationState(userId);
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAllByOrderByOrderNumberAsc();
    }

    private String downloadAndSaveFile(String fileUrl, String originalFilePath) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadDir = Paths.get(attachmentFolderPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
            log.info("Created upload directory: {}", uploadDir.toAbsolutePath());
        }

        // Generate unique filename
        String fileExtension = getFileExtension(originalFilePath);
        String uniqueFilename = UUID.randomUUID().toString() + "_" + System.currentTimeMillis();
        if (!fileExtension.isEmpty()) {
            uniqueFilename += "." + fileExtension;
        }

        // Download file from Telegram
        try {
            URI uri = new URI(fileUrl);
            try (InputStream in = uri.toURL().openStream()) {
                Path filePath = uploadDir.resolve(uniqueFilename);
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
                log.info("File saved successfully: {}", filePath.toAbsolutePath());
            }
        } catch (URISyntaxException e) {
            log.error("Invalid URI: {}", fileUrl, e);
            throw new RuntimeException("Invalid file URL", e);
        }

        // Generate URL
        String baseUrl = attachmentBaseUrl.endsWith("/") 
                ? attachmentBaseUrl.substring(0, attachmentBaseUrl.length() - 1) 
                : attachmentBaseUrl;
        String folderPath = attachmentFolderPath.startsWith("/") 
                ? attachmentFolderPath 
                : "/" + attachmentFolderPath;
        return baseUrl + folderPath + "/" + uniqueFilename;
    }

    private Attachment createAttachment(String originalUrl, String savedUrl, String filePath) {
        String fileExtension = getFileExtension(filePath);
        String filename = savedUrl.substring(savedUrl.lastIndexOf("/") + 1);
        
        return Attachment.builder()
                .url(savedUrl)
                .filePath(attachmentFolderPath + "/" + filename)
                .filename(filename)
                .originalFilename(filePath.substring(filePath.lastIndexOf("/") + 1))
                .contentType(getContentType(fileExtension))
                .fileSize(0L) // We don't have size info from Telegram
                .fileExtension(fileExtension)
                .build();
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1).toLowerCase() : "";
    }

    private String getContentType(String extension) {
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }
}

