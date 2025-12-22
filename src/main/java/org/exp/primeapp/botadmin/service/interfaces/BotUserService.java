package org.exp.primeapp.botadmin.service.interfaces;

import org.exp.primeapp.models.entities.User;

public interface BotUserService {
    
    /**
     * User count statistikasini olish
     * @return [totalCount, adminCount, superAdminCount]
     */
    long[] getUserCounts();
    
    /**
     * Phone number bo'yicha user topish
     */
    User findUserByPhone(String phoneNumber);
    
    /**
     * Userga role qo'shish
     */
    void addRoleToUser(Long userId, String roleName);
    
    /**
     * User'ning role'larini tekshirish
     */
    boolean hasRole(User user, String roleName);
    
    /**
     * User search state'ni set qilish
     */
    void setUserSearchState(Long userId, boolean state);
    
    /**
     * User search state'ni olish
     */
    boolean getUserSearchState(Long userId);
}

