package org.exp.primeapp.service.face.user;

import org.exp.primeapp.models.dto.responce.admin.AdminUserDashboardRes;
import org.exp.primeapp.models.dto.responce.admin.AdminUserDetailRes;
import org.exp.primeapp.models.dto.responce.admin.AdminUserRes;
import org.exp.primeapp.models.dto.responce.user.UserRes;
import org.exp.primeapp.models.dto.responce.user.page.PageRes;
import org.exp.primeapp.models.entities.User;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserRes getUserData(User user);

    UserRes getByTelegramId(Long userId);

    AdminUserDashboardRes getAdminAllUsers();

    UserRes getByUsername(String tgUsername);

    void toggleUserUpdate(Long userId);

    UserRes getByPhoneNumber(String phoneNumber);

    UserRes getUserDataFromToken(User user);

    UserRes getById(Long id);

    AdminUserRes getAdminUserDataFromToken(User user);

    Boolean getUserHasAdminFromToken(User user);

    AdminUserDetailRes getAdminUserDetailById(Long userId);

    PageRes<AdminUserDetailRes> getAdminUsersWithFilter(Pageable pageable, Boolean active, String phone, String firstName);
}