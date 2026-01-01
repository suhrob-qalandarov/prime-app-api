package org.exp.primeapp.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.exp.primeapp.models.dto.responce.user.UserRes;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.service.face.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.exp.primeapp.utils.Const.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(API + V1 + PROFILE)
public class ProfileController {

    private final UserService userService;

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @GetMapping
    public ResponseEntity<UserRes> getUserById(@AuthenticationPrincipal User user) {
        UserRes userRes = userService.getById(user.getId());
        return ResponseEntity.ok(userRes);
    }
}
