package com.vayana.wallet.model;

/**
 * Transfer entity representing a money movement attempt between two accounts.
 * Required fields: (id, from_id, to_id, amount, reference, status, reason, created_at)
 */
public class Transfer {
    private final long id;
    private final long fromId;
    private final long toId;
    private final long amount; // in paise
    private final String reference;
    private final TransferStatus status;
    private final String reason;
    private final String createdAt;

    public Transfer(long id, long fromId, long toId, long amount, String reference,
                    TransferStatus status, String reason, String createdAt) {
        this.id = id;
        this.fromId = fromId;
        this.toId = toId;
        this.amount = amount;
        this.reference = reference;
        this.status = status;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public long getFromId() {
        return fromId;
    }

    public long getToId() {
        return toId;
    }

    public long getAmount() {
        return amount;
    }

    public String getReference() {
        return reference;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Transfer{" +
                "id=" + id +
                ", fromId=" + fromId +
                ", toId=" + toId +
                ", amount=" + amount + " paise" +
                ", reference='" + reference + '\'' +
                ", status=" + status +
                ", reason='" + reason + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}
