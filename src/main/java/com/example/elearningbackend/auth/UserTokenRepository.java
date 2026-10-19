package com.example.elearningbackend.auth;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    Optional<UserToken> findByTokenHashAndType(String tokenHash, String type);

    boolean existsByUserIdAndTypeAndUsedAtIsNullAndCreatedAtAfter(
        Long userId, String type, LocalDateTime after);

    @Modifying
    @Query("update UserToken t set t.usedAt = :now "
         + "where t.user.id = :userId and t.type = :type and t.usedAt is null")
    void invalidateUnused(@Param("userId") Long userId,
                          @Param("type") String type,
                          @Param("now") LocalDateTime now);
}