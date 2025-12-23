package org.exp.primeapp.controller.user.cart;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.models.dto.request.CartItemReq;
import org.exp.primeapp.models.dto.responce.user.ProductCartRes;
import org.exp.primeapp.service.face.user.ProductService;
import org.exp.primeapp.utils.SessionTokenUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.exp.primeapp.utils.Const.*;

@Slf4j
@RestController
@RequestMapping(API + V1 + "/cart")
@RequiredArgsConstructor
public class CartController {

    private final ProductService productService;
    private final SessionTokenUtil sessionTokenUtil;

    @PostMapping
    public ResponseEntity<?> getCartProducts(
            @RequestBody List<CartItemReq> cartItems,
            HttpServletRequest request,
            HttpServletResponse response) {
        return sessionTokenUtil.handleSessionTokenRequest("cart", request, response, () -> {
            List<ProductCartRes> cartProducts = productService.getCartProducts(cartItems);
            return ResponseEntity.ok(cartProducts);
        });
    }
}

