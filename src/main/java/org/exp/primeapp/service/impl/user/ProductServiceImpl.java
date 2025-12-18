package org.exp.primeapp.service.impl.user;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.exp.primeapp.models.dto.responce.user.FeaturedProductRes;
import org.exp.primeapp.models.dto.responce.user.ProductPageRes;
import org.exp.primeapp.models.dto.responce.user.ProductRes;
import org.exp.primeapp.models.dto.responce.user.ProductSizeRes;
import org.exp.primeapp.models.dto.responce.user.page.PageRes;
import org.exp.primeapp.models.entities.Attachment;
import org.exp.primeapp.models.entities.Product;
import org.exp.primeapp.models.entities.ProductSize;
import org.exp.primeapp.models.enums.ProductTag;
import org.exp.primeapp.models.enums.Size;
import org.exp.primeapp.repository.AttachmentRepository;
import org.exp.primeapp.repository.ProductRepository;
import org.exp.primeapp.service.face.user.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final AttachmentRepository attachmentRepository;

    @Override
    public List<ProductRes> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::convertToProductRes)
                .collect(toList());
    }

    @Transactional
    @Override
    public FeaturedProductRes getFeaturedRandomProducts() {
        return new FeaturedProductRes(
                productRepository.findRandom4ActiveProductsStatusSale().stream()
                        .map(this::convertToProductRes)
                        .collect(toList()),

                productRepository.findRandom4ActiveProductsStatusNew().stream()
                        .map(this::convertToProductRes)
                        .collect(toList()),

                productRepository.findRandom4ActiveProductsStatusHot().stream()
                        .map(this::convertToProductRes)
                        .collect(toList())
        );
    }

    @Override
    public PageRes<ProductPageRes> getActiveProducts(Pageable pageable) {
        Page<ProductPageRes> productRes = productRepository.findAllByActive(true, pageable)
                .map(this::convertToProductPageRes);
        return toPageRes(productRes);
    }

    @Override
    public PageRes<ProductPageRes> getActiveProducts(
            String spotlightName,
            String categoryName,
            String colorName,
            String sizeName,
            String sortBy,
            Pageable pageable) {
        
        // Agar barcha filterlar bo'sh bo'lsa, oddiy query ishlatish
        boolean hasFilters = (spotlightName != null && !spotlightName.isBlank()) ||
                            (categoryName != null && !categoryName.isBlank()) ||
                            (colorName != null && !colorName.isBlank()) ||
                            (sizeName != null && !sizeName.isBlank());
        
        // Sort qo'shish - faqat sortBy parametridan, Pageable ichidagi sort e'tiborsiz qoldiriladi
        Sort sort = buildSort(sortBy);
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort);
        
        if (!hasFilters) {
            // Filterlar yo'q - oddiy query (tezroq va ishonchli)
            Page<ProductPageRes> productRes = productRepository.findAllByActive(true, sortedPageable)
                    .map(this::convertToProductPageRes);
            return toPageRes(productRes);
        }
        
        // Filterlar bor - Specification ishlatish
        Specification<Product> spec = buildProductSpecification(
                spotlightName, categoryName, colorName, sizeName);
        
        Page<ProductPageRes> productRes = productRepository.findAll(spec, sortedPageable)
                .map(this::convertToProductPageRes);
        return toPageRes(productRes);
    }

    private Specification<Product> buildProductSpecification(
            String spotlightName,
            String categoryName,
            String colorName,
            String sizeName) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Active filter
            predicates.add(cb.equal(root.get("active"), true));
            
            // Spotlight name filter
            if (spotlightName != null && !spotlightName.isBlank()) {
                predicates.add(cb.equal(root.get("category").get("spotlightName"), spotlightName));
            }
            
            // Category name filter
            if (categoryName != null && !categoryName.isBlank()) {
                predicates.add(cb.equal(root.get("category").get("name"), categoryName));
            }
            
            // Color name filter
            if (colorName != null && !colorName.isBlank()) {
                predicates.add(cb.equal(root.get("colorName"), colorName));
            }
            
            // Size filter - requires join with ProductSize
            if (sizeName != null && !sizeName.isBlank()) {
                try {
                    Size size = Size.valueOf(sizeName);
                    Join<Product, ProductSize> sizesJoin = root.join("sizes");
                    predicates.add(cb.equal(sizesJoin.get("size"), size));
                    query.distinct(true); // Prevent duplicates
                } catch (IllegalArgumentException e) {
                    // Invalid size enum, ignore
                }
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort buildSort(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return Sort.unsorted(); // Default sort yo'q
        }
        
        switch (sortBy.toLowerCase()) {
            case "discount":
                return Sort.by(Sort.Direction.DESC, "discount");
            case "low-price":
                return Sort.by(Sort.Direction.ASC, "price");
            case "high-price":
                return Sort.by(Sort.Direction.DESC, "price");
            default:
                return Sort.unsorted(); // Noto'g'ri sortBy bo'lsa ham sort yo'q
        }
    }

    @Override
    public PageRes<ProductRes> getProductsByCategoryId(Long categoryId, Pageable pageable) {
        Page<ProductRes> productRes = productRepository.findAllByCategory_IdAndActive(categoryId, true, pageable)
                .map(this::convertToProductRes);

        return toPageRes(productRes);
    }

    @Override
    public List<ProductRes> getInactiveProducts() {
        return productRepository.findAllByActive(false)
                .stream()
                .map(this::convertToProductRes)
                .collect(toList());
    }

    @Override
    public List<ProductRes> getActiveProductsByCategoryId(Long categoryId) {
        return productRepository.findByActiveAndCategoryId(categoryId, true)
                .stream()
                .map(this::convertToProductRes)
                .collect(toList());
    }

    @Override
    public List<ProductRes> getInactiveProductsByCategoryId(Long categoryId) {
        return productRepository.findByActiveAndCategoryId(categoryId, false)
                .stream()
                .map(this::convertToProductRes)
                .collect(toList());
    }

    @Override
    public ProductRes getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Product not found with productId: " + productId));
        return convertToProductRes(product);
    }

    public <T> PageRes<T> toPageRes(Page<T> page) {
        return PageRes.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    public ProductRes convertToProductRes(Product product) {
        // Query orqali attachments ni topamiz
        List<String> attachmentUrls = attachmentRepository.findByProductId(product.getId())
                .stream()
                .map(Attachment::getUrl)
                .collect(toList());

        List<ProductSizeRes> productSizes = product.getSizes()
                .stream()
                .map(size -> new ProductSizeRes(size.getSize(), size.getAmount()))
                .collect(toList());

        // Discount'ni hisoblab discountPrice'ni set qilish
        // Faqat discount > 0 va tag = SALE bo'lsagina discount hisoblanadi
        BigDecimal price = product.getPrice();
        Integer discount = product.getDiscount() != null ? product.getDiscount() : 0;
        BigDecimal discountPrice = price;
        
        if (discount > 0 && discount <= 100 && product.getTag() == ProductTag.SALE) {
            // discountPrice = price - (price * discount / 100)
            BigDecimal discountAmount = price.multiply(BigDecimal.valueOf(discount))
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            discountPrice = price.subtract(discountAmount);
        }

        return new ProductRes(
                product.getId(),
                product.getName(),
                product.getBrand(),
                product.getColorName(),
                product.getColorHex(),
                product.getTag().name(),
                product.getCategory().getName(),
                product.getDescription(),
                price,
                discountPrice,
                discount,
                attachmentUrls,
                productSizes
        );
    }

    public ProductPageRes convertToProductPageRes(Product product) {
        // Query orqali attachments ni topamiz - faqat birinchi rasm
        String mainImage = attachmentRepository.findByProductId(product.getId())
                .stream()
                .findFirst()
                .map(Attachment::getUrl)
                .orElse(null);

        // Discount'ni hisoblab discountPrice'ni set qilish
        // Faqat discount > 0 va tag = SALE bo'lsagina discount hisoblanadi
        BigDecimal price = product.getPrice();
        Integer discount = product.getDiscount() != null ? product.getDiscount() : 0;
        BigDecimal discountPrice = price;
        
        if (discount > 0 && discount <= 100 && product.getTag() == ProductTag.SALE) {
            // discountPrice = price - (price * discount / 100)
            BigDecimal discountAmount = price.multiply(BigDecimal.valueOf(discount))
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            discountPrice = price.subtract(discountAmount);
        }

        return new ProductPageRes(
                product.getId(),
                product.getName(),
                product.getBrand(),
                product.getColorHex(),
                product.getTag().name(),
                price,
                discountPrice,
                discount,
                mainImage
        );
    }
}
