

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