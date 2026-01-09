package org.exp.primeapp.botadmin.service.impls;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.botadmin.service.interfaces.BotUserService;
import org.exp.primeapp.models.entities.Role;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.repository.RoleRepository;
import org.exp.primeapp.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
// @Service // Temporarily disabled
@RequiredArgsConstructor
public class BotUserServiceImpl implements BotUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ConcurrentHashMap<Long, Boolean> userSearchStates = new ConcurrentHashMap<>();

    @Override
    public long[] getUserCounts() {
        long totalCount = userRepository.count();

        long adminCount = userRepository.findAll().stream()
                .filter(user -> hasRole(user, "ROLE_ADMIN"))
                .count();

        long superAdminCount = userRepository.findAll().stream()
                .filter(user -> hasRole(user, "ROLE_SUPER_ADMIN"))
                .count();

        return new long[] { totalCount, adminCount, superAdminCount };
    }

    @Override
    public User findUserByPhone(String phoneNumber) {
        return userRepository.findByPhone(phoneNumber);
    }

    @Override
    @Transactional
    public void addRoleToUser(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User topilmadi: " + userId));

        // Role allaqachon mavjudligini tekshirish
        if (hasRole(user, roleName)) {
            log.warn("User {} allaqachon {} role'ga ega", userId, roleName);
            return;
        }

        // Role'ni topish
        List<Role> roles = roleRepository.findALlByNameIn(List.of(roleName));
        if (roles.isEmpty()) {
            throw new RuntimeException("Role topilmadi: " + roleName);
        }

        Role role = roles.get(0);

        // User'ning role'larini olish va yangi role qo'shish
        List<Role> userRoles = user.getRoles();
        if (userRoles == null) {
            userRoles = new ArrayList<>();
        }
        userRoles.add(role);
        user.setRoles(userRoles);

        userRepository.save(user);
        log.info("User {} ga {} role qo'shildi", userId, roleName);
    }

    @Override
    public boolean hasRole(User user, String roleName) {
        if (user == null || user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream()
                .anyMatch(role -> role.getName() != null && role.getName().equals(roleName));
    }

    @Override
    public void setUserSearchState(Long userId, boolean state) {
        if (state) {
            userSearchStates.put(userId, true);
        } else {
            userSearchStates.remove(userId);
        }
    }

    @Override
    public boolean getUserSearchState(Long userId) {
        return userSearchStates.getOrDefault(userId, false);
    }
}
