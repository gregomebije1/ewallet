

# An Enterprise E-Wallet Interface Engine
This e-wallet engine that can allow users to perform common everyday transactions including fund transfers, cash withdrawals, 
airtime purchases, data subscriptions and utility or cable TV bill payments.
This is a customer-facing orchestrator layer. This engine processes client applications, validates user identity bounds, 
enforces compliance tiers, and manages distributed transactions across external vendor networks. 
It maintains no local balance database, instead leasing decoupled API interfaces from a distributed double-entry ledger engine and a Settlement Engine.

## Core System Requirements

### 1. Functional Requirements
* **Dynamic KYC Tier Compliance:** Evaluates incoming user actions against velocity and value volume boundaries based on identity tier mappings (e.g., Tier 1 cap at \$100/day while verified Tier 2 profiles enjoy uncapped standard operational levels).
* **Multi-Vendor Saga Orchestrator:** Manages multi-stage transactional state lifecycles across external SaaS APIs, executing automated compensating rollbacks if a vendor service fails.
* **Consolidated Activity Mapping:** Reconstructs user histories by pulling raw ledger records from a distributed double-entry ledger engine and pairing them with local user context metadata.
* **Notification Topology Streams:** Emits structured events directly into an Apache Kafka message bus upon ledger transaction affirmation to dispatch real-time SMS, email, and Webhook web-push receipts.

### 2. Non-Functional Distributed Requirements
* **Saga Pattern Resilience & Dead-Letter Storage:** Long-running cross-network workflows must persist state via an event sourcing backbone (e.g., Apache Kafka). Unresolvable multi-vendor transaction state drops must automatically isolate into dedicated Dead-Letter Queues (DLQ) for human audit.
* **Distributed Idempotency Layer:** Implements a distributed cache locking pattern (Redis cluster utilizing a Redlock algorithm) to intercept incoming user actions. Duplicate requests with identical keys are rejected with a `409 Conflict` within a 15-second window.
* **Partial Failures & Circuit Breaking:** Outbound API proxies 
   to external distributed double-entry engine and payment providers  
   must implement automated circuit breakers (e.g., Resilience4j) configured with a 50% failure rate trip-wire. If a vendor times out, requests must gracefully fail-fast or route to offline retry queues to preserve user experience.
* **Observability Context Propagation:** Every user-initiated request must generate a unified W3C Trace Context (`traceparent`). This token must be forced into the headers of all downstream API calls to Ledger engine and payment-gateway to enable end-to-end distributed tracking across corporate boundaries.
*   **Data Masking & PII Protection:** Enforces row-level AES-256 encryption at rest for customer metadata records, maintaining compliance alignment across PCI-DSS boundaries.


## Architecture 
### 1. Orchestrated Saga Pattern
Distributed consistency is maintained asynchronously across microservices using a centralized orchestrator rather than distributed locking.
*   **Coordination:** A central coordinator manages workflow states between the **Wallet Service**, **Ledger Service**, and **Merchant Settlement Service**.
*   **Compensation:** If a downstream service fails, the orchestrator triggers reverse operations sequentially to undo completed steps.


## Testing Strategy

### 1. Saga Failure Mode Simulation
*   Uses Temporal's native testing framework (`TestWorkflowEnvironment`) to inject synthetic network delays or explicitly throw a `RuntimeFailure` within the *Merchant Settlement Service*.
*   **Verification:** Asserts that all reverse compensating steps trigger in strict reverse order, leaving system funds in their exact pre-transaction state.

### 2. High-Concurrency Race Condition Testing
*   **Execution:** Leverages Java's `CountDownLatch` and `ExecutorService` to fire **50 parallel threads** attempting to deduct `$10.00` simultaneously from a single account initialized with a `$100.00` balance.
*   **Success Criteria:** 
    *   Exactly **10 threads** complete successfully (reducing the absolute balance to `$0.00`).
    *   **40 threads** fail safely, throwing either an `InsufficientFundsException` or an optimistic locking collision exception.
    *   No data drift or negative balances occur.


## Essential Core API Endpoints

### 1. Provision New Wallet Account Space
* **Endpoint:** `POST /api/v1/wallets/accounts`
* **Request Payload:**
```json
{
  "user_id": "usr_8821",
  "compliance_tier": "TIER_1",
  "currency": "USD"
}
```
* **Response (201 Created):**
```json
{
  "status": "success",
  "data": {
    "wallet_id": "wlt_01J8W2Z1X9Y8W7V6",
    "ledger_account_id": "acc_01J8W2Z1X9Y8W7V6U5T4S3R2Q1",
    "user_id": "usr_9876543210",
    "currency": "USD",
    "status": "ACTIVE",
    "created_at": "2026-09-19T12:55:00Z"
  }
}
```


### 2. Multi-Channel Wallet Funding Engine
#### `POST /api/v1/wallets/funding/intent`
Initiates a payment injection lifecycle via external providers (e.g., Payment gateway engine, Stripe, Adyen, Flutterwave).

* **Request Payload:**
```json
{
  "wallet_id": "wlt_01J8W2Z1X9Y8W7V6",
  "amount": 5000,
  "currency": "USD",
  "payment_method": "CREDIT_CARD",
  "provider": "STRIPE"
}
```
* **Response (200 OK):**
```json
{
  "status": "success",
  "data": {
    "funding_id": "fnd_01J8W3A1X9Y8W7V6",
    "wallet_id": "wlt_01J8W2Z1X9Y8W7V6",
    "amount": 5000,
    "currency": "USD",
    "status": "PENDING_PROVIDER",
    "client_secret": "pi_3MtwbL_secret_xyz",
    "redirect_url": "https://checkout.stripe.com/pay/c_xyz"
  }
}
```
#### `POST /api/v1/wallets/funding/webhook/{provider}`
Asynchronous ingress endpoint receiving validated notifications from payment providers. Upon successful capture, this endpoint calls `POST /api/v1/ledger/transactions` on the Ledger Service to move funds from the `SYSTEM_DEPOSIT_CLEARING` account to the user's ledger account.

* **Headers:** `X-Provider-Signature: t=1672531199,v1=g3b2...`
* **Response (200 OK):**
```json
{
  "status": "acknowledged",
  "ledger_transaction_id": "tx_01J8W3B5M9P7Q4R1"
}
```

### 3. Initiate External Transfer Payout Request
* **Endpoint:** `POST /api/v1/transfers/outbound`
* **Headers:** `X-Idempotency-Key: c9b22e11-128a-4402-b883-29a38f32dd71`
* **Payload:**
```json
{
  "sender_id": "usr_8821",
  "amount": 5000,
  "currency": "USD",
  "bank_routing_code": "058",
  "destination_account": "0123456789"
}
```


### 4. Peer-to-Peer (P2P) Transfers
#### `POST /api/v1/transactions/p2p`
Orchestrates an instant domestic internal transfer by submitting a multi-line transaction payload directly to the distributed ledger core.

* **Headers:** `X-Idempotency-Key: 8e5f2b84-482a-4f51-b0e6-0563b7df0ad1`
* **Request Payload:**
```json
{
  "sender_wallet_id": "wlt_01J8W2Z1X9Y8W7V6",
  "receiver_wallet_id": "wlt_01J8W5K3M2N1P0Q9",
  "amount": 2500,
  "currency": "USD",
  "narration": "Dinner split"
}
```
* **Response (202 Accepted):**
```json
{
  "status": "success",
  "transaction_id": "tx_01J8W3C8K9N7M4P1",
  "summary": {
    "sender_wallet_id": "wlt_01J8W2Z1X9Y8W7V6",
    "receiver_wallet_id": "wlt_01J8W5K3M2N1P0Q9",
    "amount": 2500,
    "fee_charged": 0
  }
}
```



### 5. Value-Added Services (VAS) & Bill Payments
#### `POST /api/v1/vas/purchase`
Executes digital utility purchases. This service operates via a Two-Phase Commit architecture:
1. Calls the Ledger Service to put a hold or freeze on the user's available balance.
2. Dispatches a call to the third-party utility network (e.g., Telecom API).
3. If successful, captures the hold in the ledger. If failed, releases the hold automatically.

* **Request Payload:**
```json
{
  "wallet_id": "wlt_01J8W2Z1X9Y8W7V6",
  "vas_type": "AIRTIME",
  "provider": "MTN_NG",
  "amount": 1000,
  "destination_identifier": "+2348030000001"
}
```

# Useful commands
./mvnw clean compile
./mvnw spring-boot:run
./mvnw test
mvn clean test-compile

=====
🔎 Architectural Analysis & Data MappingAccording to the design blueprint, this system acts as a customer-facing orchestrator layer. Because it leases decoupled account interfaces from a external Ledger Engine, this database focuses on tracking user context metadata, compliance velocity boundaries, cryptographic verification parameters, and long-running Saga state transitions.This production-grade PostgreSQL DDL schema incorporates row-level security primitives, AES-256 data masking for PII protection, and strict transactional state constraints.Complete Production E-Wallet Orchestrator DDL Scriptsql-- ============================================================================
-- 1. BASE LAYER EXTENSIONS & ENUMS
-- ============================================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto"; -- Provides native row-level AES-256 primitives

-- Functional operational bounds definitions
CREATE TYPE compliance_tier AS ENUM ('TIER_1', 'TIER_2', 'TIER_3');
CREATE TYPE account_status  AS ENUM ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED');
CREATE TYPE saga_status      AS ENUM ('STARTED', 'LEDGER_HOLD_SUCCESS', 'VENDOR_CALL_SUCCESS', 'COMPLETED', 'FAILED', 'COMPENSATING');
CREATE TYPE vas_type         AS ENUM ('AIRTIME', 'DATA_SUBSCRIPTION', 'UTILITY_BILL', 'CABLE_TV');
CREATE TYPE funding_status   AS ENUM ('INITIATED', 'PENDING_PROVIDER', 'SUCCESS', 'FAILED');

-- ============================================================================
-- 2. CLIENT IDENTITY BOUNDS & COMPLIANCE LAYER
-- ============================================================================

-- Defines daily and single-transaction limit bounds per KYC tier
CREATE TABLE compliance_limits (
    tier compliance_tier PRIMARY KEY,
    single_transaction_limit BIGINT NOT NULL CHECK (single_transaction_limit > 0),
    daily_velocity_limit BIGINT NOT NULL CHECK (daily_velocity_limit > 0)
);

-- Seed production-grade standard verification boundary caps
INSERT INTO compliance_limits (tier, single_transaction_limit, daily_velocity_limit) VALUES 
('TIER_1', 10000, 50000),    -- $100 single cap, $500 daily velocity limit (stored as cents)
('TIER_2', 250000, 1000000), -- $2,500 single cap, $10,000 daily velocity limit
('TIER_3', 9223372036854775807, 9223372036854775807); -- Uncapped operations

-- User Profiles: Employs strict PCI-DSS / PII isolation fields
CREATE TABLE user_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_reference VARCHAR(64) UNIQUE NOT NULL, -- e.g., 'usr_8821'
    compliance_tier compliance_tier NOT NULL DEFAULT 'TIER_1',
    status account_status NOT NULL DEFAULT 'PENDING_VERIFICATION',
    
    -- PII PROTECTION MASK: ROW-LEVEL AES-256 encryption at rest utilizing crypt-keys
    -- Stores phone/email variables securely as encrypted byte arrays
    encrypted_phone BYTEA NOT NULL,
    encrypted_email BYTEA NOT NULL,
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 3. CORE WALLET ACCOUNTS SPACE LAYER
-- ============================================================================

CREATE TABLE wallets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    wallet_id_string VARCHAR(32) UNIQUE NOT NULL, -- e.g., 'wlt_01J8W2Z1X9Y8W7V6'
    ledger_account_id VARCHAR(64) UNIQUE NOT NULL, -- Core external ledger entity link
    user_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE RESTRICT,
    currency VARCHAR(3) NOT NULL, -- ISO 4217 code
    status account_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 4. DISTRIBUTED IDEMPOTENCY LAYER
-- ============================================================================

-- Serves as an inner-database fallback safety grid to prevent duplicate user submissions
CREATE TABLE distributed_idempotency_registry (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    request_path VARCHAR(255) NOT NULL,
    hashed_payload BYTEA NOT NULL, -- SHA-256 checksum of incoming JSON variables
    cached_response JSONB NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Automatic index for cleaning out stale keys
CREATE INDEX idx_idempotency_expiry ON distributed_idempotency_registry (expires_at);

-- ============================================================================
-- 5. MULTI-VENDOR SAGA ORCHESTRATOR & VALUE-ADDED SERVICES (VAS)
-- ============================================================================

-- Tracks distributed transactional state lifecycles across external SaaS networks
CREATE TABLE saga_orchestration_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    trace_id VARCHAR(64) NOT NULL, -- Unified W3C Trace Context (traceparent context propagation)
    wallet_id UUID NOT NULL REFERENCES wallets(id) ON DELETE RESTRICT,
    transaction_type VARCHAR(64) NOT NULL, -- 'VAS_AIRTIME', 'OUTBOUND_PAYOUT', 'FUNDING_INJECTION'
    amount BIGINT NOT NULL CHECK (amount > 0),
    status saga_status NOT NULL DEFAULT 'STARTED',
    
    -- State history blobs tracking individual downstream completions
    current_step INT NOT NULL DEFAULT 1,
    ledger_hold_reference VARCHAR(128),
    external_vendor_reference VARCHAR(128),
    error_message TEXT,
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Value-Added Services (VAS) Metadata Store
CREATE TABLE vas_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    saga_id UUID NOT NULL REFERENCES saga_orchestration_logs(id) ON DELETE RESTRICT,
    type vas_type NOT NULL,
    provider_code VARCHAR(64) NOT NULL, -- e.g., 'MTN_NG'
    destination_identifier VARCHAR(255) NOT NULL, -- Encrypted or clear text routing endpoint
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 6. MULTI-CHANNEL WALLET FUNDING ENGINE
-- ============================================================================

CREATE TABLE wallet_funding_intents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    funding_reference VARCHAR(64) UNIQUE NOT NULL, -- e.g., 'fnd_01J8W3A1X9Y8W7V6'
    wallet_id UUID NOT NULL REFERENCES wallets(id) ON DELETE RESTRICT,
    amount BIGINT NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    status funding_status NOT NULL DEFAULT 'INITIATED',
    provider VARCHAR(64) NOT NULL, -- 'STRIPE', 'ADYEN', 'FLUTTERWAVE'
    provider_intent_id VARCHAR(255) UNIQUE, -- Stores external payload ids (e.g., Stripe pi_xyz)
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 7. NOTIFICATION TOPOLOGY STREAMS (THE OUTBOX PATTERN)
-- ============================================================================

CREATE TABLE ewallet_outbox_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    aggregate_type VARCHAR(64) NOT NULL, -- 'WALLET_CREATED', 'TRANSACTION_SETTLED', 'SAGA_COMPENSATED'
    aggregate_id VARCHAR(64) NOT NULL,
    event_payload JSONB NOT NULL, -- Structured JSON to pipe out to Kafka brokers
    is_dispatched BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ewallet_outbox_unprocessed 
ON ewallet_outbox_events (created_at ASC) WHERE is_dispatched = FALSE;

-- ============================================================================
-- 8. PROCEDURAL CONSTRAINTS, TRIGGERS, AND SECURITY FUNCTIONS
-- ============================================================================

/**
 * COMPLIANCE CONSTRAINT: Velocity Guardrail Trigger
 * Evaluates the incoming transaction size against the user's KYC tier cap.
 * Rejects operations immediately if the transaction breaches limit metrics.
 */
CREATE OR REPLACE FUNCTION enforce_kyc_velocity_bounds()
RETURNS TRIGGER AS $$
DECLARE
    v_tier compliance_tier;
    v_single_limit BIGINT;
    v_daily_limit BIGINT;
    v_current_daily_spend BIGINT;
    v_target_user_id UUID;
BEGIN
    -- Extract account metadata details
    SELECT user_id INTO v_target_user_id FROM wallets WHERE id = NEW.wallet_id;
    SELECT compliance_tier INTO v_tier FROM user_profiles WHERE id = v_target_user_id;
    
    -- Extract limits for the associated tier
    SELECT single_transaction_limit, daily_velocity_limit 
    INTO v_single_limit, v_daily_limit 
    FROM compliance_limits 
    WHERE tier = v_tier;

    -- Verification 1: Check single transaction bounds
    IF NEW.amount > v_single_limit THEN
        RAISE EXCEPTION 'Compliance Violation: Transaction amount % exceeds Single Limit of % for % profiles.', 
            NEW.amount, v_single_limit, v_tier;
    END IF;

    -- Verification 2: Calculate daily aggregated volume velocity
    SELECT COALESCE(SUM(amount), 0)
    INTO v_current_daily_spend
    FROM saga_orchestration_logs
    WHERE wallet_id = NEW.wallet_id
      AND status IN ('COMPLETED', 'STARTED', 'LEDGER_HOLD_SUCCESS')
      AND created_at >= CURRENT_DATE;

    IF (v_current_daily_spend + NEW.amount) > v_daily_limit THEN
        RAISE EXCEPTION 'Compliance Violation: Aggregated Daily Volume % exceeds Velocity Limit of % for % profiles.', 
            (v_current_daily_spend + NEW.amount), v_daily_limit, v_tier;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_kyc_bounds_before_saga
    BEFORE INSERT ON saga_orchestration_logs
    FOR EACH ROW
    EXECUTE FUNCTION enforce_kyc_velocity_bounds();
Use code with caution.💻 Production Implementation Showcase1. Inserting a PII Protected User Profile (AES-256 Encryption at Rest)This sample demonstrates how row-level security handles phone and email data masking using a cryptographic cipher key password:sqlINSERT INTO user_profiles (user_reference, compliance_tier, status, encrypted_phone, encrypted_email) 
VALUES (
    'usr_8821', 
    'TIER_1', 
    'ACTIVE',
    -- Encrypt with native AES-256 using a corporate encryption key string
    encrypt('07412345678'::bytea, 'pci_dss_encryption_key_passphrase', 'aes'),
    encrypt('greg@gregomebije.com'::bytea, 'pci_dss_encryption_key_passphrase', 'aes')
);
Use code with caution.2. Dynamic KYC Rule Enforcement Validation TestLet's verify that a Tier 1 customer cannot attempt a transaction that exceeds their daily limits (Tier 1 single_transaction_limit is capped at 10,000 minor units / $100.00):sql-- Fetch the target IDs generated by your system
SELECT id FROM user_profiles WHERE user_reference = 'usr_8821'; -- Returns a UUID
-- Assume Wallet UUID is generated and logged in wallets space...

-- Attempt to create a Saga Orchestration Log for $150.00 (15000 minor units)
INSERT INTO saga_orchestration_logs (trace_id, wallet_id, transaction_type, amount, status)
VALUES (
    '00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01', -- W3C Trace Context Parent Token
    'wallet_uuid_here',
    'VAS_AIRTIME',
    15000, -- $150.00
    'STARTED'
);

-- RESULT ENGINE BOUNCE:
-- ERROR: Compliance Violation: Transaction amount 15000 exceeds Single Limit of 10000 for TIER_1 profiles.
-- CONTEXT: PL/pgSQL function enforce_kyc_velocity_bounds() line 17 at RAISE
Use code with caution.Verification: Distributed Alignment MatrixAPI EndpointDatabase Storage TargetCore Active Safety Measures EnforcedPOST /wallets/accountsuser_profiles, walletsRow-Level AES-256 PII protection prevents plain-text identity leaks.POST /wallets/funding/intentwallet_funding_intentsStrict unique constraints on provider_intent_id to block dual-webhook injections.POST /transfers/outbounddistributed_idempotency_registryIntercepts keys to guarantee safe execution across third-party networks.POST /vas/purchasesaga_orchestration_logs, ewallet_outbox_eventsUses the Transactional Outbox pattern to guarantee that events reach Kafka for processing.

=====
package com.gregomebije.gateway.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletConcurrencyRaceConditionTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private PaymentService paymentService; // The Orchestrator Layer service under test

    @Test
    @DisplayName("High-Concurrency Race Condition: 50 Threads Deducting $10 from $100 Balance")
    void verifyHighConcurrencyRaceConditionSafety() throws InterruptedException {
        // --- 1. ARRANGE & SETUP ---
        final int totalThreads = 50;
        final int expectedSuccesses = 10;
        final int expectedFailures = 40;
        
        final BigDecimal initialBalance = new BigDecimal("100.00");
        final BigDecimal deductionAmount = new BigDecimal("10.00");

        // Atomic counters to safely track thread results across asynchronous contexts
        AtomicInteger successfulTransactions = new AtomicInteger(0);
        AtomicInteger failedTransactions = new AtomicInteger(0);

        // Core concurrency synchronization latches
        CountDownLatch startLineLatch = new CountDownLatch(1);      // Force all 50 threads to fire simultaneously
        CountDownLatch finishLineLatch = new CountDownLatch(totalThreads); // Wait for all 50 threads to finish execution

        // Thread pool manager to execute parallel requests
        ExecutorService executorService = Executors.newFixedThreadPool(totalThreads);

        // Mock Behavior Setup:
        // Simulate a real ledger lease database state. The first 10 calls succeed, subsequent calls throw
        // an InsufficientFundsException or an OptimisticLockingFailureException.
        when(idempotencyService.acquire(anyString())).thenReturn(true);
        
        // Use Mockito's thenAnswer to track state mutation across threads dynamically
        final AtomicInteger remainingBalanceUnits = new AtomicInteger(100); // Track cents/units ($100)
        
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            // Atomically decrement by 10 units ($10)
            int previousBalance = remainingBalanceUnits.get();
            while (previousBalance >= 10) {
                if (remainingBalanceUnits.compareAndSet(previousBalance, previousBalance - 10)) {
                    return invocation.getArgument(0); // Success path
                }
                previousBalance = remainingBalanceUnits.get(); // Retry CAS on optimistic collision
            }
            throw new InsufficientFundsException("Insufficient funds in wallet cache or ledger lease pool.");
        });

        // --- 2. ACT (SIMULATE SIMULTANEOUS CONCURRENT SPIKE) ---
        for (int i = 0; i < totalThreads; i++) {
            final String uniqueIdempotencyKey = "idem-key-" + i;
            final String uniquePaymentReference = "pay-ref-" + i;

            executorService.submit(() -> {
                try {
                    // All threads halt here until the master latch opens
                    startLineLatch.await(); 

                    PaymentRequest request = new PaymentRequest(
                            uniquePaymentReference, 
                            deductionAmount, 
                            "USD"
                    );

                    // Execute the service orchestration layer call
                    paymentService.create(uniqueIdempotencyKey, request);
                    successfulTransactions.incrementAndGet();

                } catch (InsufficientFundsException | OptimisticLockingException e) {
                    failedTransactions.incrementAndGet();
                } catch (Exception e) {
                    // Catch unexpected runtime thread exceptions to prevent test deadlock hangs
                    failedTransactions.incrementAndGet();
                } finally {
                    finishLineLatch.countDown();
                }
            });
        }

        // Drop the gate: All 50 threads race into the payment logic simultaneously
        startLineLatch.countDown();

        // Await thread completion with a strict fail-safe execution timeout barrier
        boolean threadsFinishedCleanly = finishLineLatch.await(5, TimeUnit.SECONDS);

        // Clean up allocation pools immediately
        executorService.shutdown();

        // --- 3. ASSERT (SUCCESS CRITERIA VERIFICATION) ---
        assertThat(threadsFinishedCleanly)
                .withFailMessage("Test timed out before all parallel execution lanes completed.")
                .isTrue();

        // Verify that exactly 10 threads completed successfully ($100 / $10 = 10)
        assertThat(successfulTransactions.get())
                .withFailMessage("Expected exactly %d transactions to pass but got %d", expectedSuccesses, successfulTransactions.get())
                .isEqualTo(expectedSuccesses);

        // Verify that exactly 40 threads failed safely without dropping into negative state balances
        assertThat(failedTransactions.get())
                .withFailMessage("Expected exactly %d transactions to fail but got %d", expectedFailures, failedTransactions.get())
                .isEqualTo(expectedFailures);

        // Confirm zero data drift: remaining value units must be exactly 0
        assertThat(remainingBalanceUnits.get())
                .withFailMessage("Data drift detected! Balance dropped illegally to: " + remainingBalanceUnits.get())
                .isZero();
    }
}
