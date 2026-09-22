package com.gregomebije.wallet.repository;

import com.gregomebije.wallet.model.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankAccountRepository extends JpaRepository <BankAccount, Long> {
}
