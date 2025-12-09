package org.exp.primeapp.service.face.global.auth;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.exp.primeapp.models.dto.responce.global.LoginRes;

public interface AuthService {
    LoginRes verifyWithCodeAndSendUserData(Integer code, HttpServletResponse response, HttpServletRequest request);

    void logout(HttpServletResponse response);
}
