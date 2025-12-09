package org.exp.primeapp.service.face.admin.auth;

import jakarta.servlet.http.HttpServletResponse;
import org.exp.primeapp.models.dto.request.AdminLoginReq;
import org.exp.primeapp.models.dto.responce.global.LoginRes;
import org.springframework.stereotype.Service;

@Service
public interface AdminAuthService {
    LoginRes checkAdminLogin(AdminLoginReq loginReq, HttpServletResponse response);
}
