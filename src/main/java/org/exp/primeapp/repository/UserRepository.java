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

    User findByTelegramId(Long telegramId);

    User findByTgUsername(String tgUsername);

    User findByPhone(String phone);

    @Query("SELECT u FROM User u WHERE u.phone = :phone")
    Optional<User> findByPhoneOptional(@Param("phone") String phone);

    List<User> findAllByActive(boolean b);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.active = :active WHERE u.telegramId = :telegramId")
    int updateActive(@Param("active")boolean active, @Param("telegramId") Long telegramId);

    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.active = CASE WHEN u.active = true THEN false ELSE true END WHERE u.telegramId = :telegramId")
    void toggleUserActiveStatus(@Param("telegramId") Long telegramId);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.phone = :phone WHERE u.telegramId = :telegramId")
    void updatePhoneByUserId(@Param("telegramId") Long telegramId, @Param("phone") String phone);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.verifyCode = :code, u.verifyCodeExpiration = :expiration WHERE u.telegramId = :telegramId")
    void updateVerifyCodeAndExpiration(@Param("telegramId") Long telegramId, @Param("code") Integer oneTimeCode, @Param("expiration") LocalDateTime expirationTime);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.messageId = :messageId WHERE u.telegramId = :telegramId")
    void updateMessageId(@Param("telegramId") Long telegramId, @Param("messageId") Integer messageId);

    @Query("SELECT u FROM User u WHERE " +
           "(:active IS NULL OR u.active = :active) AND " +
           "(:phone IS NULL OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :phone, '%'))) AND " +
           "(:firstName IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :firstName, '%')))")
    Page<User> findAllWithFilters(
            @Param("active") Boolean active,
            @Param("phone") String phone,
            @Param("firstName") String firstName,
            Pageable pageable
    );

    @Query("SELECT u FROM User u WHERE u.verifyCodeExpiration IS NOT NULL " +
           "AND u.verifyCodeExpiration < :now AND u.messageId IS NOT NULL")
    List<User> findUsersWithExpiredOtpCodes(@Param("now") LocalDateTime now);
}