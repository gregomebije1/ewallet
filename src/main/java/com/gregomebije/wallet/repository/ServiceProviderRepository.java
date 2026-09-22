package com.gregomebije.wallet.repository;

import com.gregomebije.wallet.model.ServiceProvider;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceProviderRepository extends JpaRepository<ServiceProvider, Long> {

}