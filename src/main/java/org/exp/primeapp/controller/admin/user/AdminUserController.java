package org.exp.primeapp.controller.admin.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.exp.primeapp.models.dto.responce.admin.AdminUserDashboardRes;
import org.exp.primeapp.models.dto.responce.admin.AdminUserDetailRes;
import org.exp.primeapp.models.dto.responce.user.page.PageRes;
import org.exp.primeapp.service.face.user.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.exp.primeapp.utils.Const.*;

@RestController
@RequestMapping(API + V1 + ADMIN + USER)
@RequiredArgsConstructor
public class AdminUserController {
    private final UserService userService;

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @GetMapping("/dashboard")
    public ResponseEntity<AdminUserDashboardRes> getAllUsers() {
        AdminUserDashboardRes users = userService.getAdminAllUsers();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @GetMapping("/{userId}")
    public ResponseEntity<AdminUserDetailRes> getUser(@PathVariable Long userId) {
        AdminUserDetailRes user = userService.getAdminUserDetailById(userId);
        return ResponseEntity.ok(user);
    }

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @GetMapping
    public ResponseEntity<PageRes<AdminUserDetailRes>> getUsers(
            Pageable pageable,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String firstName
    ) {
        PageRes<AdminUserDetailRes> users = userService.getAdminUsersWithFilter(pageable, active, phone, firstName);
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @PatchMapping("/toggle/{userId}")
    public ResponseEntity<?> toggleUser(@PathVariable Long userId) {
        userService.toggleUserUpdate(userId);
        return ResponseEntity.ok().build();
    }
}
