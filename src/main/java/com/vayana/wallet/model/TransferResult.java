package com.vayana.wallet.model;

/**
 * Encapsulates the outcome of a transfer operation, including idempotency state.
 */
public class TransferResult {
    private final boolean success;
    private final boolean idempotentReplay;
    private final Transfer transfer;
    private final String message;

    private TransferResult(boolean success, boolean idempotentReplay, Transfer transfer, String message) {
        this.success = success;
        this.idempotentReplay = idempotentReplay;
        this.transfer = transfer;
        this.message = message;
    }

    public static TransferResult success(Transfer transfer, String message) {
        return new TransferResult(true, false, transfer, message);
    }

    public static TransferResult failed(Transfer transfer, String reason) {
        return new TransferResult(false, false, transfer, reason);
    }

    public static TransferResult idempotent(Transfer priorTransfer) {
        return new TransferResult(
                priorTransfer.getStatus() == TransferStatus.COMPLETED,
                true,
                priorTransfer,
                priorTransfer.getStatus() == TransferStatus.COMPLETED
                        ? "Duplicate reference detected. Returning previous COMPLETED transfer."
                        : "Duplicate reference detected. Returning previous FAILED transfer: " + priorTransfer.getReason()
        );
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isIdempotentReplay() {
        return idempotentReplay;
    }

    public Transfer getTransfer() {
        return transfer;
    }

    public String getMessage() {
        return message;
    }
}
