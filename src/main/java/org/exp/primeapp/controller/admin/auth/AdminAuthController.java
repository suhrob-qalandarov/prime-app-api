package org.exp.primeapp.controller.admin.auth;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.exp.primeapp.models.dto.request.AdminLoginReq;
import org.exp.primeapp.models.dto.responce.global.LoginRes;
import org.exp.primeapp.service.face.admin.auth.AdminAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.exp.primeapp.utils.Const.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(API + V1 + ADMIN + AUTH)
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping
    public ResponseEntity<LoginRes> verifyAdminLogin(@RequestBody AdminLoginReq loginReq, HttpServletResponse response) {
        LoginRes loginRes = adminAuthService.checkAdminLogin(loginReq, response);
        return new ResponseEntity<>(loginRes, HttpStatus.ACCEPTED);
    }
}