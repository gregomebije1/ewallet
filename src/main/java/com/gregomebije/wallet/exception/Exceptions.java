package com.gregomebije.wallet.exception;

public final class Exceptions {
    private Exceptions() {}

    public static class ComplianceLimitExceededException extends RuntimeException {
        public ComplianceLimitExceededException() { super("Compliance limit exceeded"); }
    }
    public static class DuplicateRequestException extends RuntimeException {
        public DuplicateRequestException() { super("Request already processed"); }
        public DuplicateRequestException(String message) { super(message); }
    }
    public static class InsufficientFundsException extends RuntimeException {
        public InsufficientFundsException() { super("Insufficient funds"); }
    }
    public static class OptimisticLockingFailureException extends RuntimeException {
        public OptimisticLockingFailureException() { super("Optimistic locking collision"); }
    }
    public static class ProviderTimeoutException extends RuntimeException {
        public ProviderTimeoutException() { super("Provider timeout"); }
    }
    public static class VasPurchaseFailedException extends RuntimeException {
        public VasPurchaseFailedException() { super("VAS purchase failed"); }
    }
    public static class SagaFailedException extends RuntimeException {
        public SagaFailedException() { super("Saga failed"); }
    }
    public static class InvalidWebhookSignatureException extends RuntimeException {
        public InvalidWebhookSignatureException() { super("Invalid webhook signature"); }
    }
    public static class TransferPendingRetryException extends RuntimeException {
        public TransferPendingRetryException() { super("Transfer queued for retry"); }
    }
    public static class LedgerTransactionException extends RuntimeException {
        public LedgerTransactionException() { super("Ledger transaction failed"); }
    }
}
