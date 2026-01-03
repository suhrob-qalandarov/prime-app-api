package org.exp.primeapp.service.impl.admin.product;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.exp.primeapp.models.dto.request.ProductReq;
import org.exp.primeapp.models.dto.responce.admin.AdminProductDashboardRes;
import org.exp.primeapp.models.dto.responce.admin.AdminProductRes;
import org.exp.primeapp.models.dto.responce.user.ProductSizeRes;
import org.exp.primeapp.models.entities.*;
import org.exp.primeapp.models.enums.ProductStatus;
import org.exp.primeapp.models.enums.ProductTag;
import org.exp.primeapp.repository.AttachmentRepository;
import org.exp.primeapp.repository.CategoryRepository;
import org.exp.primeapp.repository.ProductIncomeRepository;
import org.exp.primeapp.repository.ProductRepository;
import org.exp.primeapp.service.face.admin.product.AdminProductService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminProductServiceImpl implements AdminProductService {

    @Value("${app.products.update-offset-minutes}")
    private long updateOffsetMinutes;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final AttachmentRepository attachmentRepository;
    private final ProductIncomeRepository productIncomeRepository;
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
                .productResList(productResList)
                .build();
    }

    private Map<String, Long> mapCountByTag(List<AdminProductRes> productResList) {
        return productResList.stream()
                .collect(Collectors.groupingBy(AdminProductRes::status, Collectors.counting()));
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

    @Transactional
    public AdminProductRes convertToAdminProductRes(Product product) {
        List<ProductSizeRes> productSizeReslist = product.getSizes().stream()
                .map(size -> ProductSizeRes.builder()
                        .size(size.getSize())
                        .amount(size.getQuantity())
                        .build())
                .toList();
        List<String> picturesUrlList = attachmentRepository.findByProductId(product.getId())
                .stream()
                .map(Attachment::getUrl)
                .toList();
        return AdminProductRes.builder()
                .id(product.getId())
                .name(product.getName())
                .brand(product.getBrand())
                .colorName(product.getColorName())
                .colorHex(product.getColorHex())
                .description(product.getDescription())
                .categoryName(product.getCategory().getName())
                .price(product.getPrice())
                .status(product.getStatus().name())
                .active(product.getStatus() == ProductStatus.ON_SALE)
                .discount(product.getDiscountPercent())
                .createdAt(product.getCreatedAt().format(formatter))
                .picturesUrls(picturesUrlList)
                .productSizeRes(productSizeReslist)
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

        Set<String> attachmentUrls = productReq.attachmentUrls();
        if (attachmentUrls == null || attachmentUrls.isEmpty()) {
            throw new RuntimeException("Attachments empty");
        }

        List<Attachment> attachments = new java.util.ArrayList<>(attachmentRepository.findAllByUrlIn(attachmentUrls));
        if (attachments.size() != attachmentUrls.size()) {
            throw new RuntimeException("Some attachments not found");
        }

        Product product = createProductFromReq(productReq, category);
        product.setDiscountPercent(0);
        Product savedProduct = productRepository.save(product);

        // Attachments ga product_id ni set qilamiz
        attachments.forEach(attachment -> attachment.setProduct(savedProduct));
        attachmentRepository.saveAll(attachments);

        return convertToAdminProductRes(savedProduct);
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
                .sizes(new HashSet<>())
                .build();
    }

    private ProductIncome createIncome(Product product, Integer amount) {
        return ProductIncome.builder()
                .stockQuantity(amount)
                .product(product)
                .build();
    }

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