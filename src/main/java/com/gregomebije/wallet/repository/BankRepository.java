package com.gregomebije.wallet.repository;

import com.gregomebije.wallet.model.Bank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankRepository extends JpaRepository <Bank, Long> {
}
