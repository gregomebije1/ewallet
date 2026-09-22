package com.gregomebije.wallet.service.history;

import java.util.*;

import org.springframework.stereotype.Service;

import com.gregomebije.wallet.model.Models.Activity;
import com.gregomebije.wallet.model.UserMetadata;

@Service
public class ActivityHistoryService {
    private final ActivityLedgerClient ledger;
    private final UserMetadataRepository metadata;
    public ActivityHistoryService(ActivityLedgerClient ledger, UserMetadataRepository metadata) {
        this.ledger = ledger; this.metadata = metadata;
    }
    public List<Activity> getActivity(String walletId) {
        UserMetadata m = metadata.findByWalletId(walletId);
        return ledger.getTransactions(walletId).stream()
                .map(t -> new Activity(m.getUserId(), t.type(), t.amount())).toList();
    }
}