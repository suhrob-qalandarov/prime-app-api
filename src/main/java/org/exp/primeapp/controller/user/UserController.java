package org.exp.primeapp.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.exp.primeapp.models.dto.responce.user.UserRes;
import org.exp.primeapp.service.face.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.exp.primeapp.utils.Const.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(API + V1 + USER)
public class UserController {

    private final UserService userService;

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @GetMapping("/{userId}")
    public ResponseEntity<UserRes> getUserById(@PathVariable Long userId) {
        UserRes user = userService.getById(userId);
        return ResponseEntity.ok(user);
    }
}
