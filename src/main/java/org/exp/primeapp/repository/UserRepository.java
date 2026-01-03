package org.exp.primeapp.repository;

import org.exp.primeapp.models.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

       User findOneByVerifyCode(Integer verifyCode);

       Optional<User> findByPhoneAndVerifyCode(String phone, Integer verifyCode);

       @Query(value = "SELECT * FROM app_users WHERE telegram_id = :telegramId ORDER BY id ASC LIMIT 1", nativeQuery = true)
       Optional<User> findByTelegramId(@Param("telegramId") Long telegramId);

       User findByTgUsername(String tgUsername);

       User findByPhone(String phone);

       @Query("SELECT u FROM User u WHERE u.phone = :phone")
       Optional<User> findByPhoneOptional(@Param("phone") String phone);

       List<User> findAllByStatus(org.exp.primeapp.models.enums.AccountStatus status);

       @Modifying
       @Transactional
       @Query("UPDATE User u SET u.status = :status WHERE u.telegramId = :telegramId")
       int updateStatus(@Param("status") org.exp.primeapp.models.enums.AccountStatus status,
                     @Param("telegramId") Long telegramId);

       @Transactional
       @Modifying
       @Query("UPDATE User u SET u.status = CASE WHEN u.status = 'ACTIVE' THEN 'INACTIVE' ELSE 'ACTIVE' END WHERE u.telegramId = :telegramId")
       void toggleUserActiveStatus(@Param("telegramId") Long telegramId);

       @Modifying
       @Transactional
       @Query("UPDATE User u SET u.phone = :phone WHERE u.telegramId = :telegramId")
       void updatePhoneByUserId(@Param("telegramId") Long telegramId, @Param("phone") String phone);

       @Modifying
       @Transactional
       @Query("UPDATE User u SET u.verifyCode = :code, u.verifyCodeExpiration = :expiration WHERE u.telegramId = :telegramId")
       void updateVerifyCodeAndExpiration(@Param("telegramId") Long telegramId, @Param("code") Integer oneTimeCode,
                     @Param("expiration") LocalDateTime expirationTime);

       @Modifying
       @Transactional
       @Query("UPDATE User u SET u.messageId = :messageId WHERE u.telegramId = :telegramId")
       void updateMessageId(@Param("telegramId") Long telegramId, @Param("messageId") Integer messageId);

       @Query("SELECT u FROM User u WHERE " +
                     "(:status IS NULL OR u.status = :status) AND " +
                     "(:phone IS NULL OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :phone, '%'))) AND " +
                     "(:firstName IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :firstName, '%')))")
       Page<User> findAllWithFilters(
                     @Param("status") org.exp.primeapp.models.enums.AccountStatus status,
                     @Param("phone") String phone,
                     @Param("firstName") String firstName,
                     Pageable pageable);

       @Query("SELECT u FROM User u WHERE u.verifyCodeExpiration IS NOT NULL " +
                     "AND u.verifyCodeExpiration < :now AND u.messageId IS NOT NULL")
       List<User> findUsersWithExpiredOtpCodes(@Param("now") LocalDateTime now);
}