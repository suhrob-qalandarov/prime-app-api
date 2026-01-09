package org.exp.primeapp.botadmin.service.impls;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.File;
import com.pengrad.telegrambot.request.GetFile;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.botadmin.models.ProductCreationState;
import org.exp.primeapp.botadmin.service.interfaces.BotProductService;
import org.exp.primeapp.models.dto.request.ProductReq;
import org.exp.primeapp.models.entities.Attachment;
import org.exp.primeapp.models.entities.Category;
import org.exp.primeapp.models.entities.Product;
import org.exp.primeapp.models.entities.ProductSize;
import org.exp.primeapp.models.enums.CategoryStatus;
import org.exp.primeapp.models.enums.ProductStatus;
import org.exp.primeapp.models.enums.Size;
import org.exp.primeapp.repository.AttachmentRepository;
import org.exp.primeapp.repository.CategoryRepository;
import org.exp.primeapp.repository.ProductRepository;
import org.exp.primeapp.service.face.admin.product.AdminProductService;
import org.springframework.beans.factory.annotation.Qualifier;
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
// @Service // Temporarily disabled
public class BotProductServiceImpl implements BotProductService {

    private final Map<Long, ProductCreationState> productCreationStates = new ConcurrentHashMap<>();
    private final TelegramBot telegramBot;
    private final CategoryRepository categoryRepository;
    private final AttachmentRepository attachmentRepository;
    private final ProductRepository productRepository;
    private final AdminProductService adminProductService;

    @Value("${app.attachment.base.url}")
    private String attachmentBaseUrl;

    @Value("${app.attachment.folder.path}")
    private String attachmentFolderPath;

    @Value("${telegram.bot.admin.token:}")
    private String botToken;

    public BotProductServiceImpl(@Qualifier("adminBot") TelegramBot telegramBot,
            CategoryRepository categoryRepository,
            AttachmentRepository attachmentRepository,
            ProductRepository productRepository,
            AdminProductService adminProductService) {
        this.telegramBot = telegramBot;
        this.categoryRepository = categoryRepository;
        this.attachmentRepository = attachmentRepository;
        this.productRepository = productRepository;
        this.adminProductService = adminProductService;
    }

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
        // Format: birinchi harfni katta, qolganlarini kichik
        String formattedName = formatName(name);
        state.setName(formattedName);
        state.setCurrentStep(ProductCreationState.Step.WAITING_DESCRIPTION);
    }

    @Override
    public void handleProductDescription(Long userId, String description) {
        ProductCreationState state = getProductCreationState(userId);
        if (state == null || state.getCurrentStep() != ProductCreationState.Step.WAITING_DESCRIPTION) {
            return;
        }
        // Format: faqat birinchi harfni katta
        String formattedDescription = formatDescription(description);
        state.setDescription(formattedDescription);
        state.setCurrentStep(ProductCreationState.Step.WAITING_BRAND);
    }

    @Override
    public void handleProductBrand(Long userId, String brand) {
        ProductCreationState state = getProductCreationState(userId);
        if (state == null || state.getCurrentStep() != ProductCreationState.Step.WAITING_BRAND) {
            return;
        }
        // Format: barcha harflarni katta
        String formattedBrand = formatBrand(brand);
        state.setBrand(formattedBrand);
        state.setCurrentStep(ProductCreationState.Step.WAITING_COLOR);
    }

    @Override
    public void handleProductColor(Long userId, String colorName, String colorHex) {
        ProductCreationState state = getProductCreationState(userId);
        if (state == null || state.getCurrentStep() != ProductCreationState.Step.WAITING_COLOR) {
            return;
        }
        state.setColorName(colorName);
        state.setColorHex(colorHex);
        state.setCurrentStep(ProductCreationState.Step.WAITING_MAIN_IMAGE);
    }

    @Override
    @Transactional
    public void handleProductImage(Long userId, String fileId) {
        ProductCreationState state = getProductCreationState(userId);
        if (state == null ||
                (state.getCurrentStep() != ProductCreationState.Step.WAITING_MAIN_IMAGE &&
                        state.getCurrentStep() != ProductCreationState.Step.WAITING_ADDITIONAL_IMAGES)) {
            return;
        }

        // Clear old images when going back to image steps
        if (state.getCurrentStep() == ProductCreationState.Step.WAITING_MAIN_IMAGE) {
            // Clear main image if exists (user is replacing it)
            clearMainImage(userId);
        } else if (state.getCurrentStep() == ProductCreationState.Step.WAITING_ADDITIONAL_IMAGES) {
            // Don't clear additional images automatically - user can add multiple
            // Only clear if we're at max capacity and user wants to replace
            if (!state.canAddMoreImages()) {
                // At max capacity, clear last additional image to make room
                if (state.getAttachmentUrls() != null && state.getAttachmentUrls().size() >= 3) {
                    String lastImageUrl = state.getAttachmentUrls().get(state.getAttachmentUrls().size() - 1);
                    Attachment attachment = attachmentRepository.findByUrl(lastImageUrl);
                    if (attachment != null) {
                        attachmentRepository.save(attachment);
                        log.info("Last additional image attachment soft-deleted: {}", attachment.getId());
                    }
                    state.getAttachmentUrls().remove(state.getAttachmentUrls().size() - 1);
                    if (state.getImageFileIds() != null && !state.getImageFileIds().isEmpty()) {
                        state.getImageFileIds().remove(state.getImageFileIds().size() - 1);
                    }
                }
            }
        }

        if (!state.canAddMoreImages()) {
            return; // Already has 3 images
        }

        try {
            // Download file from Telegram
            com.pengrad.telegrambot.response.GetFileResponse getFileResponse = telegramBot.execute(new GetFile(fileId));
            if (!getFileResponse.isOk()) {
                log.error("Failed to get file from Telegram for fileId: {}, error: {}", fileId,
                        getFileResponse.description());
                return;
            }

            File file = getFileResponse.file();
            if (file == null) {
                log.error("File not found for fileId: {}", fileId);
                return;
            }

            String filePath = file.filePath();
            if (filePath == null || filePath.isEmpty()) {
                log.error("File path is null or empty for fileId: {}", fileId);
                return;
            }

            log.info("Downloading file from Telegram: fileId={}, filePath={}", fileId, filePath);
            String fileUrl = "https://api.telegram.org/file/bot" + botToken + "/" + filePath;

            // Download and save file
            String savedUrl = downloadAndSaveFile(fileUrl, filePath);
            log.info("File saved successfully: savedUrl={}", savedUrl);

            // Create attachment
            Attachment attachment = createAttachment(fileUrl, savedUrl, filePath);
            attachmentRepository.save(attachment);
            log.info("Attachment saved to database: id={}, url={}", attachment.getId(), attachment.getUrl());

            state.addImageFileId(fileId);
            state.addAttachmentUrl(savedUrl);

            // If main image is uploaded, move to additional images step
            if (state.getCurrentStep() == ProductCreationState.Step.WAITING_MAIN_IMAGE) {
                state.setCurrentStep(ProductCreationState.Step.WAITING_ADDITIONAL_IMAGES);
            }
        } catch (Exception e) {
            log.error("Error handling product image for fileId: {}", fileId, e);
        }
    }

    @Override
    public void handleSpotlightNameSelection(Long userId, String spotlightName) {
        ProductCreationState state = getProductCreationState(userId);
        if (state == null || state.getCurrentStep() != ProductCreationState.Step.WAITING_SPOTLIGHT_NAME) {
            return;
        }
        state.setSpotlightName(spotlightName);
        state.setCurrentStep(ProductCreationState.Step.WAITING_CATEGORY);
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
            // Toggle: if size is already selected, remove it; otherwise add it
            if (state.getSelectedSizes().contains(size)) {
                state.getSelectedSizes().remove(size);
                // Also remove quantity if exists
                state.getSizeQuantities().remove(size);
            } else {
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
    public void handleProductPrice(Long userId, String priceText) {
        ProductCreationState state = getProductCreationState(userId);
        if (state == null || state.getCurrentStep() != ProductCreationState.Step.WAITING_PRICE) {
            return;
        }

        try {
            BigDecimal price = new BigDecimal(priceText.trim());
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException("Price must be greater than zero");
            }
            state.setPrice(price);
            state.setCurrentStep(ProductCreationState.Step.CONFIRMATION);
        } catch (NumberFormatException e) {
            log.error("Invalid price: {}", priceText, e);
            throw new RuntimeException("Narx noto'g'ri formatda. Iltimos, raqam kiriting (masalan: 150000)");
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
                    state.getCategory() == null ||
                    state.getAttachmentUrls() == null || state.getAttachmentUrls().isEmpty() ||
                    state.getSelectedSizes() == null || state.getSelectedSizes().isEmpty() ||
                    state.getPrice() == null) {
                throw new RuntimeException("Product data is incomplete");
            }

            // Color is optional - use default values if not provided
            String colorName = (state.getColorName() != null && !state.getColorName().trim().isEmpty())
                    ? state.getColorName()
                    : "N/A";
            String colorHex = (state.getColorHex() != null && !state.getColorHex().trim().isEmpty())
                    ? state.getColorHex()
                    : "#808080";

            // Brand is optional - use empty string if not provided
            String brand = (state.getBrand() != null && !state.getBrand().trim().isEmpty())
                    ? state.getBrand()
                    : "N/A";

            // Create ProductReq
            ProductReq productReq = ProductReq.builder()
                    .name(state.getName())
                    .description(state.getDescription())
                    .brand(brand)
                    .colorName(colorName)
                    .colorHex(colorHex)
                    .price(state.getPrice())
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
                            .quantity(quantity)
                            .costPrice(BigDecimal.ZERO)
                            .build();
                    savedProduct.getSizes().add(productSize);
                }
            }

            // Set status to ON_SALE
            savedProduct.setStatus(ProductStatus.ON_SALE);
            productRepository.save(savedProduct);

            // Check category status and update to VISIBLE if it's CREATED
            Category category = state.getCategory();
            if (category != null && category.getStatus() == CategoryStatus.CREATED) {
                category.setStatus(CategoryStatus.ACTIVE);
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
    public void goToPreviousStep(Long userId) {
        ProductCreationState state = getProductCreationState(userId);
        if (state == null) {
            return;
        }

        ProductCreationState.Step currentStep = state.getCurrentStep();
        ProductCreationState.Step previousStep = getPreviousStep(currentStep);

        if (previousStep != null) {
            state.setCurrentStep(previousStep);
        }
    }

    private ProductCreationState.Step getPreviousStep(ProductCreationState.Step currentStep) {
        return switch (currentStep) {
            case WAITING_NAME -> null; // First step, no previous
            case WAITING_DESCRIPTION -> ProductCreationState.Step.WAITING_NAME;
            case WAITING_BRAND -> ProductCreationState.Step.WAITING_DESCRIPTION;
            case WAITING_COLOR -> ProductCreationState.Step.WAITING_BRAND;
            case WAITING_MAIN_IMAGE -> ProductCreationState.Step.WAITING_COLOR;
            case WAITING_ADDITIONAL_IMAGES -> ProductCreationState.Step.WAITING_MAIN_IMAGE;
            case WAITING_SPOTLIGHT_NAME -> ProductCreationState.Step.WAITING_ADDITIONAL_IMAGES;
            case WAITING_CATEGORY -> ProductCreationState.Step.WAITING_SPOTLIGHT_NAME;
            case WAITING_SIZES -> ProductCreationState.Step.WAITING_CATEGORY;
            case WAITING_QUANTITIES -> ProductCreationState.Step.WAITING_SIZES;
            case WAITING_PRICE -> ProductCreationState.Step.WAITING_QUANTITIES;
            case CONFIRMATION -> ProductCreationState.Step.WAITING_PRICE;
        };
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAllByOrderByOrderNumberAsc();
    }

    @Override
    public List<Category> getCategoriesBySpotlightName(String spotlightName) {
        // Get categories by spotlight name with CREATED or VISIBLE status
        List<org.exp.primeapp.models.enums.CategoryStatus> statuses = List.of(
                org.exp.primeapp.models.enums.CategoryStatus.CREATED,
                CategoryStatus.ACTIVE);
        return categoryRepository.findBySpotlightNameAndStatusInOrderByOrderNumberAsc(spotlightName, statuses);
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
                long bytesCopied = Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
                log.info("File saved successfully: {} ({} bytes)", filePath.toAbsolutePath(), bytesCopied);

                // Verify file was saved
                if (!Files.exists(filePath) || Files.size(filePath) == 0) {
                    throw new IOException("File was not saved correctly or is empty");
                }
            }
        } catch (URISyntaxException e) {
            log.error("Invalid URI: {}", fileUrl, e);
            throw new RuntimeException("Invalid file URL", e);
        } catch (IOException e) {
            log.error("Error downloading file from URL: {}", fileUrl, e);
            throw new IOException("Failed to download file from Telegram", e);
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

    @Override
    public void clearMainImage(Long userId) {
        ProductCreationState state = getProductCreationState(userId);
        if (state == null) {
            return;
        }

        // Remove first image (main image) if exists
        if (state.getAttachmentUrls() != null && !state.getAttachmentUrls().isEmpty()) {
            String mainImageUrl = state.getAttachmentUrls().get(0);

            // Find and soft-delete attachment
            Attachment attachment = attachmentRepository.findByUrl(mainImageUrl);
            if (attachment != null) {
                // attachment.setActive(false); // Removed as field doesn't exist
                // attachmentRepository.save(attachment);
                log.info("Main image attachment cleared (not soft-deleted as field missing): {}", attachment.getId());
            }

            // Remove from state
            state.getAttachmentUrls().remove(0);
            if (state.getImageFileIds() != null && !state.getImageFileIds().isEmpty()) {
                state.getImageFileIds().remove(0);
            }

            log.info("Main image cleared from state for user: {}", userId);
        }
    }

    @Override
    public void clearAdditionalImages(Long userId) {
        ProductCreationState state = getProductCreationState(userId);
        if (state == null) {
            return;
        }

        // Remove additional images (all except first/main image) if exists
        if (state.getAttachmentUrls() != null && state.getAttachmentUrls().size() > 1) {
            List<String> additionalImageUrls = new ArrayList<>(
                    state.getAttachmentUrls().subList(1, state.getAttachmentUrls().size()));

            // Soft-delete all additional image attachments
            for (String url : additionalImageUrls) {
                Attachment attachment = attachmentRepository.findByUrl(url);
                if (attachment != null) {
                    // attachment.setActive(false); // Removed as field doesn't exist
                    // attachmentRepository.save(attachment);
                    log.info("Additional image attachment cleared (not soft-deleted as field missing): {}",
                            attachment.getId());
                }
            }

            // Remove from state (keep only first/main image)
            if (state.getAttachmentUrls().size() > 1) {
                state.getAttachmentUrls().subList(1, state.getAttachmentUrls().size()).clear();
            }
            if (state.getImageFileIds() != null && state.getImageFileIds().size() > 1) {
                state.getImageFileIds().subList(1, state.getImageFileIds().size()).clear();
            }

            log.info("Additional images cleared from state for user: {}, removed {} images", userId,
                    additionalImageUrls.size());
        }
    }

    /**
     * Format name: birinchi harfni katta, qolganlarini kichik
     * Example: "JOHN DOE" -> "John doe"
     */
    private String formatName(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
    }

    /**
     * Format description: faqat birinchi harfni katta
     * Example: "this is a description" -> "This is a description"
     */
    private String formatDescription(String description) {
        if (description == null || description.isBlank()) {
            return description;
        }
        String trimmed = description.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1);
    }

    /**
     * Format brand: barcha harflarni katta
     * Example: "nike" -> "NIKE"
     */
    private String formatBrand(String brand) {
        if (brand == null || brand.isBlank()) {
            return brand;
        }
        return brand.trim().toUpperCase();
    }
}
