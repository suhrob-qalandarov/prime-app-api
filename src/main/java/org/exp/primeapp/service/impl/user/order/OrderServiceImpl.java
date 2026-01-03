package org.exp.primeapp.service.impl.user.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.models.dto.request.CreateOrderItemReq;
import org.exp.primeapp.models.dto.request.CreateOrderReq;
import org.exp.primeapp.models.dto.responce.order.UserOrderItemRes;
import org.exp.primeapp.models.dto.responce.order.UserOrderRes;
import org.exp.primeapp.models.dto.responce.order.UserProfileOrdersRes;
import org.exp.primeapp.models.entities.*;
import org.exp.primeapp.models.enums.OrderStatus;

import org.exp.primeapp.repository.*;
import org.exp.primeapp.service.face.user.OrderService;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductSizeRepository productSizeRepository;
    private final ProductOutcomeRepository productOutcomeRepository;
    private final CustomerRepository customerRepository;
    private final org.exp.primeapp.service.face.user.CustomerService customerService;
    private final AttachmentRepository attachmentRepository;

    @Transactional
    @Override
    public UserProfileOrdersRes getUserProfileOrdersById(Long id) {
        List<UserOrderRes> pendingOrderResList = orderRepository
                .findByOrderedByUserIdAndStatusIn(id, List.of(OrderStatus.PENDING, OrderStatus.PENDING_PAYMENT))
                .stream()
                .map(this::convertToUserOrderRes)
                .toList();

        List<UserOrderRes> confirmedOrderResList = orderRepository
                .findByOrderedByUserIdAndStatusIn(id, List.of(OrderStatus.PAID, OrderStatus.CONFIRMED)).stream()
                .map(this::convertToUserOrderRes)
                .toList();

        List<UserOrderRes> shippedOrderResList = orderRepository
                .findByOrderedByUserIdAndStatusIn(id, List.of(OrderStatus.SHIPPED, OrderStatus.DELIVERED)).stream()
                .map(this::convertToUserOrderRes)
                .toList();

        return UserProfileOrdersRes.builder()
                .pendingOrders(pendingOrderResList)
                .confirmedOrders(confirmedOrderResList)
                .shippedOrders(shippedOrderResList)
                .build();
    }

    @Transactional
    public UserOrderRes convertToUserOrderRes(Order order) {
        List<UserOrderItemRes> items = order.getItems().stream()
                .map(orderItem -> {
                    BigDecimal totalSum = orderItem.getUnitPrice()
                            .multiply(BigDecimal.valueOf(orderItem.getQuantity()));

                    // Calculate discount amount for this item
                    BigDecimal itemOriginalPrice = orderItem.getUnitPrice();
                    if (orderItem.getDiscountPercent() != null && orderItem.getDiscountPercent() > 0) {
                        BigDecimal multiplier = BigDecimal.valueOf(100 - orderItem.getDiscountPercent())
                                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                        itemOriginalPrice = orderItem.getUnitPrice().divide(multiplier, 2, RoundingMode.HALF_UP);
                    }
                    BigDecimal itemDiscountAmount = itemOriginalPrice.subtract(orderItem.getUnitPrice())
                            .multiply(BigDecimal.valueOf(orderItem.getQuantity()));

                    return UserOrderItemRes.builder()
                            .name(orderItem.getName())
                            .brand(orderItem.getBrand())
                            .colorName(orderItem.getColorName())
                            .colorHex(orderItem.getColorHex())
                            .mainImageUrl(orderItem.getImageUrl())
                            .size(orderItem.getSize())
                            .price(orderItem.getUnitPrice().setScale(2, RoundingMode.HALF_UP))
                            .discount(orderItem.getDiscountPercent())
                            .discountPrice(itemDiscountAmount.intValue())
                            .amount(orderItem.getQuantity())
                            .totalSum(totalSum.setScale(2, RoundingMode.HALF_UP))
                            .build();
                })
                .toList();

        BigDecimal totalDiscountSum = items.stream()
                .map(item -> BigDecimal.valueOf(item.discountPrice() != null ? item.discountPrice() : 0))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return UserOrderRes.builder()
                .id(order.getId())
                .status(order.getStatus().getLabel())
                .deliveryType(order.getShippingType().getLabel())
                .createdAt(order.getCreatedAt() != null ? order.getCreatedAt().format(DATE_FORMATTER) : null)
                .deliveredAt(order.getDeliveredAt() != null ? order.getDeliveredAt().format(DATE_FORMATTER) : null)
                .items(items)
                .totalSum(order.getTotalPrice().setScale(2, RoundingMode.HALF_UP))
                .totalDiscountSum(totalDiscountSum)
                .build();
    }

    @Override
    @Transactional
    public UserOrderRes createOrder(User user, Session session, CreateOrderReq orderRequest) {
        log.info("Creating order for User ID: {}", user.getId());

        // 1. Get or Create Customer
        String phoneNumber = user.getPhone();
        String fullName = user.getFirstName() + (user.getLastName() != null ? " " + user.getLastName() : "");

        if (orderRequest.customer() != null) {
            if (orderRequest.customer().phoneNumber() != null && !orderRequest.customer().phoneNumber().isBlank()) {
                phoneNumber = orderRequest.customer().phoneNumber();
            }
            if (orderRequest.customer().fullName() != null && !orderRequest.customer().fullName().isBlank()) {
                fullName = orderRequest.customer().fullName();
            }
        }

        Customer customer = customerService.getOrCreateCustomer(phoneNumber, fullName, user);

        // 2. Create Order
        Order order = new Order();
        order.setOrderedByUser(user);
        order.setOrderedBySession(session);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setShippingType(orderRequest.delivery());

        if (orderRequest.customer() != null) {
            order.setComment(orderRequest.customer().comment());
        }

        BigDecimal totalPrice = BigDecimal.ZERO;
        List<OrderItem> orderItemsList = new ArrayList<>();

        try {
            for (CreateOrderItemReq itemReq : orderRequest.items()) {
                log.debug("Processing Item. ProductID: {}, SizeID: {}, Quantity: {}",
                        itemReq.productId(), itemReq.sizeId(), itemReq.amount());

                // Fetch Product
                Product product = fetchProduct(itemReq.productId());

                // Fetch Size by ID
                ProductSize productSize = fetchAndValidateProductSize(
                        itemReq.sizeId(), product.getId(), itemReq.amount());

                // Calculate Price
                BigDecimal finalUnitPrice = calculateItemPrice(product);
                BigDecimal itemTotal = finalUnitPrice.multiply(BigDecimal.valueOf(itemReq.amount()));

                totalPrice = totalPrice.add(itemTotal);

                // Create OrderItem (Snapshot)
                OrderItem orderItem = createOrderItem(order, product, productSize, itemReq.amount(), finalUnitPrice);
                orderItemsList.add(orderItem);

                // Update Stock
                updateStockAndLogOutcome(productSize, itemReq.amount(), user, product);
            }

            order.setItems(orderItemsList);
            order.setTotalPrice(totalPrice.setScale(2, RoundingMode.HALF_UP));

            Order savedOrder = orderRepository.save(order);

            // Update customer stats
            customer.setOrderAmount(customer.getOrderAmount() + 1);
            if (customer.getIsNew()) {
                customer.setIsNew(false);
            }
            customerRepository.save(customer);

            log.info("Order successfully created. OrderId: {}", savedOrder.getId());
            return convertToUserOrderRes(savedOrder);

        } catch (ObjectOptimisticLockingFailureException e) {
            log.error("Optimistic locking failure. UserId: {}", user.getId(), e);
            throw new RuntimeException("Mahsulot zaxirasi o'zgartirilgan. Iltimos, qaytadan urinib ko'ring.");
        } catch (RuntimeException e) {
            log.error("Error creating order. UserId: {}", user.getId(), e);
            throw e;
        }
    }

    private Product fetchProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Mahsulot topilmadi"));
    }

    private ProductSize fetchAndValidateProductSize(Long productSizeId, Long productId, int quantity) {
        if (productSizeId == null) {
            throw new IllegalArgumentException("Mahsulot hajmi tanlanmagan");
        }
        ProductSize productSize = productSizeRepository.findById(productSizeId)
                .orElseThrow(() -> new RuntimeException("Mahsulot hajmi topilmadi"));

        if (!productSize.getProduct().getId().equals(productId)) {
            throw new RuntimeException("Mahsulot hajmi mos kelmadi");
        }
        if (productSize.getQuantity() < quantity) {
            throw new RuntimeException("Zaxirada yetarli emas: " + productSize.getSize());
        }
        return productSize;
    }

    private BigDecimal calculateItemPrice(Product product) {
        BigDecimal price = product.getPrice();
        Integer discount = product.getDiscountPercent();

        if (discount != null && discount > 0) {
            BigDecimal discountAmount = price.multiply(BigDecimal.valueOf(discount))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            price = price.subtract(discountAmount);
        }

        return price.setScale(2, RoundingMode.HALF_UP);
    }

    private OrderItem createOrderItem(Order order, Product product, ProductSize productSize, int quantity,
            BigDecimal unitPrice) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setProductSize(productSize);
        orderItem.setCategory(product.getCategory());

        // Snapshot fields
        orderItem.setName(product.getName());
        orderItem.setBrand(product.getBrand());
        orderItem.setColorName(product.getColorName());
        orderItem.setColorHex(product.getColorHex());
        orderItem.setSize(productSize.getSize().name()); // Assuming Size is Enum
        orderItem.setCategoryName(product.getCategory().getName());
        orderItem.setTag(product.getTag());

        // Set Image URL
        List<Attachment> attachments = attachmentRepository.findByProductId(product.getId());
        String imageUrl = "N/A";
        if (!attachments.isEmpty()) {
            imageUrl = attachments.getFirst().getUrl();
        }
        orderItem.setImageUrl(imageUrl);

        orderItem.setQuantity(quantity);
        orderItem.setUnitPrice(unitPrice);
        orderItem.setDiscountPercent(product.getDiscountPercent() != null ? product.getDiscountPercent() : 0);

        return orderItem;
    }

    private void updateStockAndLogOutcome(ProductSize productSize, int quantity, User user, Product product) {
        productSize.setQuantity(productSize.getQuantity() - quantity);
        productSizeRepository.save(productSize);

        ProductOutcome outcome = new ProductOutcome();
        outcome.setUser(user);
        outcome.setProduct(product);
        outcome.setStockQuantity(quantity);
        outcome.setProductSize(productSize);
        // outcome.setUnitPrice/TotalPrice logic if needed
        productOutcomeRepository.save(outcome);
    }
}