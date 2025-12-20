package org.exp.primeapp.controller.admin.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.exp.primeapp.models.dto.request.AdminLoginReq;
import org.exp.primeapp.models.dto.responce.admin.AdminUserRes;
import org.exp.primeapp.models.dto.responce.global.LoginRes;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.service.face.admin.auth.AdminAuthService;
import org.exp.primeapp.service.face.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static org.exp.primeapp.utils.Const.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(API + V1 + ADMIN + AUTH)
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final UserService userService;

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @PostMapping
    public ResponseEntity<LoginRes> verifyAdminLogin(
            @RequestBody AdminLoginReq loginReq, 
            HttpServletResponse response,
            HttpServletRequest request) {
        LoginRes loginRes = adminAuthService.checkAdminLogin(loginReq, response, request);
        return new ResponseEntity<>(loginRes, HttpStatus.ACCEPTED);
    }

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @GetMapping("/me")
    public ResponseEntity<AdminUserRes> getAdminData(@AuthenticationPrincipal User user) {
        AdminUserRes adminUserRes = userService.getAdminUserDataFromToken(user);
        if (adminUserRes == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(adminUserRes);
    }
}