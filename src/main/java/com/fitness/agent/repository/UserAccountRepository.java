package com.fitness.agent.repository;

import com.fitness.agent.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 登录账号仓库。
 */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByPhone(String phone);

    boolean existsByPhone(String phone);
}
