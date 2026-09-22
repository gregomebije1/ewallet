package com.gregomebije.wallet.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "user_metadata")
public class UserMetadata {

    @Id
    private String walletId;
    
    private String userId;

    // Standard constructor, getters, and setters (or use Lombok @Data / @Getter @Setter)
    public UserMetadata() {}

    public UserMetadata(String userId, String walletId) {
        this.userId = userId;
        this.walletId = walletId;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getWalletId() { return walletId; }
    public void setWalletId(String walletId) { this.walletId = walletId; }
}
