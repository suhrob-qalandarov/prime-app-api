package org.exp.primeapp.controller.user.order;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.models.dto.request.CreateOrderReq;
import org.exp.primeapp.models.dto.responce.order.UserOrderRes;

import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.service.face.global.session.SessionService;
import org.exp.primeapp.service.face.user.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static org.exp.primeapp.utils.Const.*;

@Slf4j
@RestController
@RequestMapping(API + V1 + ORDER)
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final SessionService sessionService;

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @PostMapping
    public ResponseEntity<UserOrderRes> createOrder(
            @RequestBody CreateOrderReq orderRequest,
            @AuthenticationPrincipal User user,
            HttpServletRequest request,
            HttpServletResponse response) {
        var session = sessionService.getOrCreateSession(request, response);
        UserOrderRes orderRes = orderService.createOrder(user, session, orderRequest);
        return new ResponseEntity<>(orderRes, HttpStatus.OK);
    }
}