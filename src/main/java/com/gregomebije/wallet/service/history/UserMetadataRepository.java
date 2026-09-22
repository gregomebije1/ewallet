package com.gregomebije.wallet.service.history;

import com.gregomebije.wallet.model.UserMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMetadataRepository extends JpaRepository<UserMetadata, String> {
    UserMetadata findByWalletId(String walletId);
}
