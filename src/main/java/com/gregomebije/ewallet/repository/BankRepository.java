package com.gregomebije.ewallet.repository;

import com.gregomebije.ewallet.model.Bank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankRepository extends JpaRepository <Bank, Long> {
}
