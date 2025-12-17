package org.exp.primeapp.service.impl.user;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.exp.primeapp.models.dto.responce.user.FeaturedProductRes;
import org.exp.primeapp.models.dto.responce.user.ProductRes;
import org.exp.primeapp.models.dto.responce.user.ProductSizeRes;
import org.exp.primeapp.models.dto.responce.user.page.PageRes;
import org.exp.primeapp.models.entities.Attachment;
import org.exp.primeapp.models.entities.Product;
import org.exp.primeapp.models.enums.ProductTag;
import org.exp.primeapp.repository.AttachmentRepository;
import org.exp.primeapp.repository.ProductRepository;
import org.exp.primeapp.service.face.user.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    public PageRes<ProductRes> getActiveProducts(Pageable pageable) {
        Page<ProductRes> productRes = productRepository.findAllByActive(true, pageable)
                .map(this::convertToProductRes);
        return toPageRes(productRes);
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
}
