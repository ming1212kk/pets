package com.pet.social.repository;

import com.pet.social.entity.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Long> {
    Optional<UserAccountEntity> findByPhone(String phone);

    Optional<UserAccountEntity> findByWechatKey(String wechatKey);
}
