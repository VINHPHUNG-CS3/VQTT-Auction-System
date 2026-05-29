package com.bt.shared.protocol.dto;

import java.time.LocalDateTime;

/**
 * Server trả về sau khi nạp tiền thành công.
 */
public class DepositResponse {

    private double depositedAmount;   // Số tiền vừa nạp
    private double newBalance;        // Số dư mới sau khi nạp
    private LocalDateTime depositedAt; // Thời điểm nạp tiền

    public DepositResponse() { /* Gson */ }

    public DepositResponse(double depositedAmount, double newBalance,
                           LocalDateTime depositedAt) {
        this.depositedAmount = depositedAmount;
        this.newBalance = newBalance;
        this.depositedAt = depositedAt;
    }

    public double getDepositedAmount() { return depositedAmount; }
    public void setDepositedAmount(double depositedAmount) {
        this.depositedAmount = depositedAmount;
    }

    public double getNewBalance() { return newBalance; }
    public void setNewBalance(double newBalance) { this.newBalance = newBalance; }

    public LocalDateTime getDepositedAt() { return depositedAt; }
    public void setDepositedAt(LocalDateTime depositedAt) {
        this.depositedAt = depositedAt;
    }
}
