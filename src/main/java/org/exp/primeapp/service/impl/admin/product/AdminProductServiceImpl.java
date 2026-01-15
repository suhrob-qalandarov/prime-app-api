package org.exp.primeapp.service.impl.admin.product;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.models.dto.request.ProductReq;
import org.exp.primeapp.models.dto.responce.admin.AdminProductDashboardRes;
import org.exp.primeapp.models.dto.responce.admin.AdminProductRes;
import org.exp.primeapp.models.dto.response.admin.AdminAttachmentRes;
import org.exp.primeapp.models.dto.responce.user.ProductPageRes;
import org.exp.primeapp.models.dto.responce.user.ProductSizeRes;
import org.exp.primeapp.models.entities.*;
import org.exp.primeapp.models.enums.ProductStatus;
import org.exp.primeapp.models.enums.ProductTag;
import org.exp.primeapp.repository.AttachmentRepository;
import org.exp.primeapp.repository.CategoryRepository;
import org.exp.primeapp.repository.InventoryTransactionRepository;
import org.exp.primeapp.repository.ProductRepository;
import org.exp.primeapp.service.face.admin.product.AdminProductService;
import org.exp.primeapp.service.impl.user.ProductServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductServiceImpl implements AdminProductService {

    private final ProductServiceImpl productServiceImpl;
    @Value("${app.products.update-offset-minutes}")
    private long updateOffsetMinutes;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final AttachmentRepository attachmentRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final org.exp.primeapp.service.face.global.attachment.AttachmentService attachmentService;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @Override
    @Transactional
    public AdminProductDashboardRes getProductDashboardRes() {
        List<AdminProductRes> productResList = productRepository.findAll()
                .stream()
                .map(this::convertToAdminProductRes)
                .toList();

        long totalCount = productResList.size();
        long activeCount = productResList.stream().filter(AdminProductRes::active).count();
        long inactiveCount = productResList.stream().filter(p -> !p.active()).count();

        Map<String, Long> countMap = mapCountByTag(productResList);

        long newTagCount = getCountByTag(countMap, ProductTag.NEW.name());
        long hotTagCount = getCountByTag(countMap, ProductTag.HOT.name());
        long saleTagCount = getCountByTag(countMap, ProductTag.SALE.name());

        return AdminProductDashboardRes.builder()
                .totalCount(totalCount)
                .activeCount(activeCount)
                .inactiveCount(inactiveCount)
                .newTagCount(newTagCount)
                .hotTagCount(hotTagCount)
                .saleTagCount(saleTagCount)
                .responseDate(LocalDateTime.now().plusMinutes(updateOffsetMinutes))
                .products(productResList)
                .build();
    }

    private Map<String, Long> mapCountByTag(List<AdminProductRes> productResList) {
        return productResList.stream()
                .collect(Collectors.groupingBy(AdminProductRes::tag, Collectors.counting()));
    }

    private long getCountByTag(Map<String, Long> countMap, String tag) {
        return countMap.getOrDefault(tag, 0L);
    }

    @Override
    @Transactional
    public AdminProductRes toggleProductUpdate(Long productId) {
        productRepository.toggleProductUpdateStatus(productId);
        Product product = productRepository.findById(productId)
                .orElseThrow();
        return convertToAdminProductRes(product);
    }

    @Override
    public List<ProductPageRes> getProductListForIncome() {
        return productRepository.findAll().stream()
                .map(productServiceImpl::convertToProductPageRes)
                .toList();
    }

    @Transactional
    public AdminProductRes convertToAdminProductRes(Product product) {
        List<ProductSizeRes> productSizeReslist = product.getSizes().stream()
                .map(size -> ProductSizeRes.builder()
                        .size(size.getSize())
                        .amount(size.getQuantity())
                        .build())
                .toList();

        List<Attachment> attachments = attachmentRepository.findByProductId(product.getId());
        List<AdminAttachmentRes> attachmentResList = attachments.stream()
                .map(this::convertToAdminAttachmentRes)
                .toList();

        // Use mainImageFilename from product, or fallback to first attachment
        String mainImageFilename = product.getMainImageFilename();
        if (mainImageFilename == null && !attachments.isEmpty()) {
            mainImageFilename = attachments.getFirst().getFilename();
        }
        String mainImageUrl = mainImageFilename != null
                ? attachmentService.generateUrl(mainImageFilename)
                : null;

        return AdminProductRes.builder()
                .id(product.getId())
                .name(product.getName())
                .mainImageUrl(mainImageUrl)
                .brand(product.getBrand())
                .colorName(product.getColorName())
                .colorHex(product.getColorHex())
                .description(product.getDescription())
                .categoryName(product.getCategory().getName())
                .price(product.getPrice())
                .tag(product.getTag().name())
                .active(product.getStatus() == ProductStatus.ON_SALE)
                .discount(product.getDiscountPercent())
                .createdAt(product.getCreatedAt().format(formatter))
                .attachments(attachmentResList)
                .sizes(productSizeReslist)
                .build();
    }

    private AdminAttachmentRes convertToAdminAttachmentRes(Attachment attachment) {
        String url = attachmentService.convertToAttachmentRes(attachment).url();
        return AdminAttachmentRes.builder()
                .filename(attachment.getFilename())
                .url(url)
                .isMain(attachment.getIsMain())
                .isActive(attachment.getIsActive() != null ? attachment.getIsActive() : true)
                .orderNumber(attachment.getOrderNumber())
                .dateTime(attachment.getCreatedAt())
                .build();
    }

    @Transactional
    @Override
    public AdminProductRes getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with telegramId: " + productId));
        return convertToAdminProductRes(product);
    }

    @Transactional
    @Override
    public AdminProductRes saveProduct(ProductReq productReq) {
        Category category = categoryRepository.findById(productReq.categoryId())
                .orElseThrow(
                        () -> new RuntimeException("Category not found with categoryId: " + productReq.categoryId()));

        Set<Object> attachmentObjects = productReq.attachmentUrls();
        if (attachmentObjects == null || attachmentObjects.isEmpty()) {
            throw new RuntimeException("Attachments empty");
        }

        // Extract filenames from Mixed Types (String or Map)
        Set<String> filenames = attachmentObjects.stream()
                .map(obj -> {
                    if (obj instanceof String str) {
                        return extractFilenameFromUrl(str);
                    }
                    if (obj instanceof java.util.Map map) {
                        Object filenameObj = map.get("filename");
                        if (filenameObj != null && !filenameObj.toString().isEmpty()) {
                            return filenameObj.toString();
                        }
                        Object urlObj = map.get("url");
                        if (urlObj != null) {
                            return extractFilenameFromUrl(urlObj.toString());
                        }
                    }
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        // Find attachments by filename (not UUID!)
        List<Attachment> attachments = filenames.stream()
                .map(filename -> attachmentRepository.findByFilename(filename)
                        .orElse(null))
                .filter(att -> att != null)
                .toList();

        if (attachments.size() != filenames.size()) {
            log.error("Expected {} attachments, found {}. Filenames: {}",
                    filenames.size(), attachments.size(), filenames);
            throw new RuntimeException("Some attachments not found");
        }

        Product product = createProductFromReq(productReq, category);
        product.setDiscountPercent(0);

        // Set main image filename
        String mainImageFilename = attachments.stream()
                .filter(Attachment::getIsMain)
                .map(Attachment::getFilename)
                .findFirst()
                .orElseGet(() -> attachments.isEmpty() ? null : attachments.get(0).getFilename());
        product.setMainImageFilename(mainImageFilename);

        Product savedProduct = productRepository.save(product);

        // Attachments ga product_id ni set qilamiz
        attachments.forEach(attachment -> attachment.setProduct(savedProduct));
        attachmentRepository.saveAll(attachments);

        return convertToAdminProductRes(savedProduct);
    }

    /**
     * Extract UUID from attachment URL or filename
     * Examples:
     * "/api/v1/attachment/uuid-123.jpg" -> "uuid-123"
     * "http://localhost:8080/api/v1/attachment/uuid-456.png" -> "uuid-456"
     * "uuid-789.jpg" -> "uuid-789"
     * "uuid-abc" -> "uuid-abc" (already UUID without extension)
     */
    private String extractFilenameFromUrl(String urlOrFilename) {
        if (urlOrFilename == null || urlOrFilename.isEmpty()) {
            return urlOrFilename;
        }

        // Extract filename from URL (last segment after the last slash)
        if (urlOrFilename.contains("/")) {
            String[] parts = urlOrFilename.split("/");
            return parts[parts.length - 1];
        }

        // Already a filename
        return urlOrFilename;
    }

    private Product createProductFromReq(ProductReq req, Category category) {
        return Product.builder()
                .name(formatName(req.name()))
                .brand(formatBrand(req.brand()))
                .colorName(req.colorName())
                .colorHex(req.colorHex())
                .description(formatDescription(req.description()))
                .price(req.price())
                .status(ProductStatus.PENDING_INCOME)
                .category(category)
                .categoryName(category.getName())
                .sizes(new HashSet<>())
                .build();
    }

    /*
     * Commented out - no longer needed with InventoryTransaction
     * private InventoryTransaction createIncome(Product product, Integer amount) {
     * return InventoryTransaction.builder()
     * .stockQuantity(amount)
     * .build();
     * }
     */

    @Transactional
    @Override
    public AdminProductRes updateProduct(Long productId, ProductReq productReq) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with productId: " + productId));
        updateProductFields(product, productReq);
        Product updatedProduct = productRepository.save(product);
        return convertToAdminProductRes(updatedProduct);
    }

    private void updateProductFields(Product product, ProductReq req) {
        if (req.categoryId() != null) {
            Category category = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found with categoryId: " + req.categoryId()));
            product.setCategory(category);
            product.setCategoryName(category.getName());
        }

        /*
         * if (req.getAttachmentIds() != null && !req.getAttachmentIds().isEmpty()) {
         * Set<Attachment> attachments = new
         * HashSet<>(attachmentRepository.findAllById(req.getAttachmentIds()));
         * product.setAttachments(attachments);
         * }
         */

        if (hasText(req.name())) {
            product.setName(formatName(req.name()));
        }

        if (hasText(req.brand())) {
            product.setBrand(formatBrand(req.brand()));
        }

        if (hasText(req.colorName())) {
            product.setColorName(req.colorName());
        }

        if (hasText(req.colorHex())) {
            product.setColorHex(req.colorHex());
        }

        if (hasText(req.description())) {
            product.setDescription(formatDescription(req.description()));
        }

        if (req.price() != null) {
            product.setPrice(req.price());
        }

        /*
         * if (req.getDiscount() != null) {
         * product.setDiscount(req.getDiscount());
         * }
         * 
         * if (req.getStatus() != null) {
         * product.setStatus(req.getStatus());
         * }
         */

        // ProductSize'larni yangilaymiz
        /*
         * if (req.productSizes() != null) {
         * product.getSizes().clear();
         * req.productSizes().forEach(sizeReq -> {
         * ProductSize productSize = new ProductSize();
         * productSize.setSize(sizeReq.size());
         * productSize.setAmount(sizeReq.amount());
         * product.addSize(productSize);
         * });
         * }
         */
    }

    private boolean hasText(String str) {
        return str != null && !str.isBlank();
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