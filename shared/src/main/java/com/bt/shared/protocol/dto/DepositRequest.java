package com.bt.shared.protocol.dto;

/**
 * Bidder gửi request nạp tiền vào tài khoản.
 *
 * Server sẽ:
 *  - Validate amount > 0 và <= MAX_DEPOSIT
 *  - Cộng vào account_balance trong DB (atomic)
 *  - Trả về balance mới qua DepositResponse
 */
public class DepositRequest {

    /** Số tiền nạp tối đa mỗi lần: 1 tỷ VNĐ. */
    public static final double MAX_DEPOSIT = 1_000_000_000.0;

    private double amount;

    public DepositRequest() { /* Gson */ }

    public DepositRequest(double amount) {
        this.amount = amount;
    }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}
