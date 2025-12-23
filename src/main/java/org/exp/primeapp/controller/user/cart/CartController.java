package org.exp.primeapp.controller.user.cart;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
    @Operation(
            summary = "Get cart products with stock validation",
            description = "Returns list of products with details, stock availability, and total prices for cart items. " +
                    "Each item includes product information, selected size, available quantity, requested quantity, " +
                    "and calculated total price (with discount if applicable).",
            security = @SecurityRequirement(name = "Authorization")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved cart products",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ProductCartRes.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "418",
                    description = "I'm a teapot - Session token count limit reached"
            )
    })
    public ResponseEntity<?> getCartProducts(
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "List of cart items with productId, productSize, and productQuantity",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = CartItemReq.class))
                    )
            )
            List<CartItemReq> cartItems,
            HttpServletRequest request,
            HttpServletResponse response) {
        return sessionTokenUtil.handleSessionTokenRequest("cart", request, response, () -> {
            List<ProductCartRes> cartProducts = productService.getCartProducts(cartItems);
            return ResponseEntity.ok(cartProducts);
        });
    }
}

