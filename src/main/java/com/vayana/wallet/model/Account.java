package com.vayana.wallet.model;

import java.time.LocalDateTime;

/**
 * Account entity representing a wallet account.
 * Money is strictly stored as whole paise (integers) in 'balance'.
 */
public class Account {
    private final long id;
    private final String ownerName;
    private final long balance; // in paise
    private final String createdAt;

    public Account(long id, String ownerName, long balance, String createdAt) {
        this.id = id;
        this.ownerName = ownerName;
        this.balance = balance;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public long getBalance() {
        return balance;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", ownerName='" + ownerName + '\'' +
                ", balance=" + balance + " paise" +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}
