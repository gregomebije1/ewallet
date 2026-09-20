package com.gregomebije.ewallet.repository;

import com.gregomebije.ewallet.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository <User, Long> {
}
