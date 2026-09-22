package com.gregomebije.wallet.repository;

import com.gregomebije.wallet.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository <User, Long> {
}
