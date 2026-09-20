package com.gregomebije.ewallet.repository;

import com.gregomebije.ewallet.model.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankAccountRepository extends JpaRepository <BankAccount, Long> {
}
