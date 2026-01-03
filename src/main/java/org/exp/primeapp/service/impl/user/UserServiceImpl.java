package org.exp.primeapp.service.impl.user;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.exp.primeapp.models.dto.responce.admin.AdminUserDashboardRes;
import org.exp.primeapp.models.dto.responce.admin.AdminUserDetailRes;
import org.exp.primeapp.models.dto.responce.admin.AdminUserRes;
import org.exp.primeapp.models.dto.responce.order.UserProfileOrdersRes;
import org.exp.primeapp.models.dto.responce.user.SessionRes;
import org.exp.primeapp.models.dto.responce.user.UserRes;
import org.exp.primeapp.models.dto.responce.user.page.PageRes;
import org.exp.primeapp.models.entities.Role;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.repository.UserRepository;
import org.exp.primeapp.service.face.user.OrderService;
import org.exp.primeapp.service.face.user.UserService;
import org.exp.primeapp.utils.UserUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

        private final UserRepository userRepository;
        private final OrderService orderService;
        private final UserUtil userUtil;

        @Override
        public UserRes getUserData(User user) {
                return getById(user.getId());
        }

        @Transactional
        @Override
        public UserRes getByTelegramId(Long userId) {
                User user = userRepository.findById(userId).orElseThrow();
                return convertToUserRes(user);
        }

        @Transactional
        @Override
        public AdminUserDashboardRes getAdminAllUsers() {
                List<AdminUserRes> adminUserResList = userRepository.findAll().stream().map(this::convertToAdminUserRes)
                                .toList();
                List<AdminUserRes> adminActiveUserResList = userRepository
                                .findAllByStatus(org.exp.primeapp.models.enums.AccountStatus.ACTIVE).stream()
                                .map(this::convertToAdminUserRes).toList();
                List<AdminUserRes> adminInactiveUserResList = userRepository
                                .findAllByStatus(org.exp.primeapp.models.enums.AccountStatus.INACTIVE).stream()
                                .map(this::convertToAdminUserRes).toList();

                long count = adminUserResList.size();
                long activeCount = adminActiveUserResList.size();
                long inactiveCount = adminInactiveUserResList.size();

                return new AdminUserDashboardRes(count, activeCount, inactiveCount, adminUserResList,
                                adminActiveUserResList, adminInactiveUserResList);
        }

        @Transactional
        @Override
        public UserRes getByUsername(String tgUsername) {
                User user = userRepository.findByTgUsername(tgUsername);
                return convertToUserRes(user);
        }

        @Override
        public void toggleUserUpdate(Long userId) {

        }

        @Transactional
        @Override
        public UserRes getByPhoneNumber(String phoneNumber) {
                User user = userRepository.findByPhone(phoneNumber);
                return convertToUserRes(user);
        }

        @Transactional
        @Override
        public UserRes getUserDataFromToken(User user) {
                return convertToUserRes(user);
        }

        @Override
        public AdminUserRes getAdminUserDataFromToken(User user) {
                boolean isAdmin = user.getRoles().stream()
                                .anyMatch(role -> role.getName().equals("ROLE_ADMIN")
                                                || role.getName().equals("ROLE_VISITOR"));
                if (!isAdmin) {
                        return null;
                }
                return convertToAdminUserResForMe(user);
        }

        @Override
        public Boolean getUserHasAdminFromToken(User user) {
                User dbUser = userRepository.findById(user.getId())
                                .orElseThrow();
                return dbUser.getRoles().stream()
                                .anyMatch(role -> role.getName().equals("ROLE_ADMIN")
                                                || role.getName().equals("ROLE_VISITOR"));
        }

        @Override
        public UserRes getById(Long id) {
                Optional<User> optionalUser = userRepository.findById(id);
                return optionalUser.map(this::convertToUserRes).orElse(null);
        }

        @Transactional
        public UserRes convertToUserRes(User user) {
                UserProfileOrdersRes profileOrdersById = orderService.getUserProfileOrdersById(user.getId());

                List<SessionRes> sessions = user.getSessions() != null ? user.getSessions().stream()
                                .map(s -> SessionRes.builder()
                                                .sessionId(s.getSessionId())
                                                .ip(s.getIp())
                                                .browserInfo(s.getBrowserInfo())
                                                .isActive(s.getIsActive())
                                                .isDeleted(s.getIsDeleted())
                                                .isAuthenticated(s.getIsAuthenticated())
                                                .isMainSession(s.getIsMainSession())
                                                .lastAccessedAt(s.getLastAccessedAt())
                                                .migratedAt(s.getMigratedAt())
                                                .build())
                                .toList() : List.of();

                return UserRes.builder()
                                .id(user.getId())
                                .firstName(userUtil.truncateName(user.getFirstName()))
                                .lastName(userUtil.truncateName(user.getLastName()))
                                .phone(user.getPhone())
                                .username(user.getTgUsername())
                                .orders(profileOrdersById)
                                .sessions(sessions)
                                .isAdmin(user.getRoles().stream()
                                                .anyMatch(role -> role.getName().equals("ROLE_ADMIN")
                                                                || role.getName().equals("ROLE_VISITOR")))
                                .isVisitor(user.getRoles().stream()
                                                .anyMatch(role -> role.getName().equals("ROLE_VISITOR")))
                                .isSuperAdmin(user.getRoles().stream()
                                                .anyMatch(role -> role.getName().equals("ROLE_SUPER_ADMIN")))
                                .build();
        }

        private AdminUserRes convertToAdminUserRes(User user) {
                return AdminUserRes.builder()
                                .id(user.getId())
                                .telegramId(user.getTelegramId())
                                .firstName(user.getFirstName())
                                .lastName(user.getLastName())
                                .username(user.getTgUsername())
                                .phone(user.getPhone())
                                .sessions(null) // Dashboard uchun sessions kerak emas
                                .active(user.getStatus() == org.exp.primeapp.models.enums.AccountStatus.ACTIVE)
                                .isAdmin(user.getRoles().stream()
                                                .anyMatch(role -> role.getName().equals("ROLE_ADMIN")
                                                                || role.getName().equals("ROLE_VISITOR")))
                                .isVisitor(user.getRoles().stream()
                                                .anyMatch(role -> role.getName().equals("ROLE_VISITOR")))
                                .isSuperAdmin(user.getRoles().stream()
                                                .anyMatch(role -> role.getName().equals("ROLE_SUPER_ADMIN")))
                                .createdAt(user.getCreatedAt())
                                .build();
        }

        private AdminUserRes convertToAdminUserResForMe(User user) {
                // Convert sessions to SessionRes
                List<SessionRes> sessions = user.getSessions() != null ? user.getSessions().stream()
                                .map(s -> SessionRes.builder()
                                                .sessionId(s.getSessionId())
                                                .ip(s.getIp())
                                                .browserInfo(s.getBrowserInfo())
                                                .isActive(s.getIsActive())
                                                .isDeleted(s.getIsDeleted())
                                                .isAuthenticated(s.getIsAuthenticated())
                                                .isMainSession(s.getIsMainSession())
                                                .lastAccessedAt(s.getLastAccessedAt())
                                                .migratedAt(s.getMigratedAt())
                                                .build())
                                .toList() : List.of();

                return AdminUserRes.builder()
                                .id(user.getId())
                                .telegramId(user.getTelegramId())
                                .firstName(userUtil.truncateName(user.getFirstName()))
                                .lastName(userUtil.truncateName(user.getLastName()))
                                .username(user.getTgUsername())
                                .phone(user.getPhone())
                                .sessions(sessions)
                                .active(user.getStatus() == org.exp.primeapp.models.enums.AccountStatus.ACTIVE)
                                .isAdmin(user.getRoles().stream()
                                                .anyMatch(role -> role.getName().equals("ROLE_ADMIN")
                                                                || role.getName().equals("ROLE_VISITOR")))
                                .isVisitor(user.getRoles().stream()
                                                .anyMatch(role -> role.getName().equals("ROLE_VISITOR")))
                                .isSuperAdmin(user.getRoles().stream()
                                                .anyMatch(role -> role.getName().equals("ROLE_SUPER_ADMIN")))
                                .createdAt(user.getCreatedAt())
                                .build();
        }

        @Override
        public AdminUserDetailRes getAdminUserDetailById(Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
                return convertToAdminUserDetailRes(user);
        }

        @Override
        public PageRes<AdminUserDetailRes> getAdminUsersWithFilter(Pageable pageable, Boolean active, String phone,
                        String firstName) {
                org.exp.primeapp.models.enums.AccountStatus stats = null;
                if (active != null)
                        stats = active ? org.exp.primeapp.models.enums.AccountStatus.ACTIVE
                                        : org.exp.primeapp.models.enums.AccountStatus.INACTIVE;

                Page<User> userPage = userRepository.findAllWithFilters(stats, phone, firstName, pageable);
                Page<AdminUserDetailRes> detailResPage = userPage.map(this::convertToAdminUserDetailRes);
                return toPageRes(detailResPage);
        }

        private AdminUserDetailRes convertToAdminUserDetailRes(User user) {
                return AdminUserDetailRes.builder()
                                .id(user.getId())
                                .telegramId(user.getTelegramId())
                                .firstName(user.getFirstName())
                                .lastName(user.getLastName())
                                .tgUsername(user.getTgUsername())
                                .phone(user.getPhone())
                                .roles(user.getRoles().stream().map(Role::getName).toList())
                                .active(user.getStatus() == org.exp.primeapp.models.enums.AccountStatus.ACTIVE)
                                .messageId(user.getMessageId())
                                .verifyCode(user.getVerifyCode())
                                .build();
        }

        private <T> PageRes<T> toPageRes(Page<T> page) {
                return PageRes.<T>builder()
                                .content(page.getContent())
                                .page(page.getNumber())
                                .size(page.getSize())
                                .totalElements(page.getTotalElements())
                                .totalPages(page.getTotalPages())
                                .last(page.isLast())
                                .build();
        }
}