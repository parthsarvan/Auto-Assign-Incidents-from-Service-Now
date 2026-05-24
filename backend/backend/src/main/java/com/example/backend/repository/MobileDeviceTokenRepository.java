package com.example.backend.repository;

import com.example.backend.entity.MobileDeviceToken;
import com.example.backend.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MobileDeviceTokenRepository extends JpaRepository<MobileDeviceToken, Long> {
    Optional<MobileDeviceToken> findByUserAndDeviceToken(User user, String deviceToken);

    List<MobileDeviceToken> findAllByUserAndActiveTrue(User user);

    long deleteByUser(User user);
}
