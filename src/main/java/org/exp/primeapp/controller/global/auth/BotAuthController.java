package org.exp.primeapp.controller.global.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.models.dto.responce.global.LoginRes;
import org.exp.primeapp.models.dto.responce.user.UserRes;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.service.face.global.auth.AuthService;
import org.exp.primeapp.service.face.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static org.exp.primeapp.utils.Const.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(API + V2 + AUTH)
public class BotAuthController {

    private final AuthService authService;
    private final UserService userService;

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @GetMapping("/me")
    public ResponseEntity<UserRes> getUserData(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserRes userRes = userService.getUserDataFromToken(user);
        return ResponseEntity.ok(userRes);
    }

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @GetMapping("/admin")
    public ResponseEntity<Boolean> checkUserAdminRole(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false);
        }
        Boolean isAdmin = userService.getUserHasAdminFromToken(user);
        return new ResponseEntity<>(isAdmin, HttpStatus.ACCEPTED);
    }

    @PostMapping("/code/{code}")
    public ResponseEntity<LoginRes> verifyUserWithCode(
            @PathVariable Integer code,
            HttpServletResponse response,
            HttpServletRequest request) {
        LoginRes loginRes = authService.verifyWithCodeAndSendUserData(code, response, request);
        return new ResponseEntity<>(loginRes, HttpStatus.ACCEPTED);
    }

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @PostMapping("/logout")
    public ResponseEntity<LoginRes> logout(HttpServletResponse response) {
        authService.logout(response);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
}
