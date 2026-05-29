package com.bt.server.service;

import com.bt.server.dao.DatabaseConnection;
import com.bt.server.dao.UserDAO;
import com.bt.shared.exception.ValidationException;
import com.bt.shared.protocol.dto.DepositRequest;
import com.bt.shared.protocol.dto.DepositResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Xử lý nạp tiền cho Bidder.
 *
 * Toàn bộ logic (validate → đọc balance → cộng → ghi) chạy trong 1 transaction
 * IMMEDIATE để tránh race condition khi bidder bấm nạp nhiều lần cùng lúc.
 *
 * Giới hạn nghiệp vụ:
 *  - Mỗi lần nạp: 1,000 đ – 1,000,000,000 đ
 *  - Số dư tối đa sau nạp: 10,000,000,000 đ (10 tỷ)
 */
public class DepositService {

    private static final Logger log = LoggerFactory.getLogger(DepositService.class);

    /** Số dư tối đa cho phép (10 tỷ VNĐ). */
    private static final double MAX_BALANCE = 10_000_000_000.0;

    /** Số tiền nạp tối thiểu. */
    private static final double MIN_DEPOSIT = 1_000.0;

    private final UserDAO userDAO;

    public DepositService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Nạp tiền cho bidder.
     *
     * @param bidderId  ID của bidder (đã được RequestRouter xác thực)
     * @param req       DepositRequest từ client
     * @return          DepositResponse với balance mới
     * @throws ValidationException nếu dữ liệu không hợp lệ
     */
    public DepositResponse deposit(long bidderId, DepositRequest req)
            throws ValidationException {

        double amount = req.getAmount();

        // --- Validate đầu vào ---
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount < MIN_DEPOSIT) {
            throw new ValidationException(
                    "Số tiền nạp phải >= " + formatVnd(MIN_DEPOSIT)
                            + " (nhận: " + formatVnd(amount) + ")");
        }
        if (amount > DepositRequest.MAX_DEPOSIT) {
            throw new ValidationException(
                    "Mỗi lần nạp tối đa " + formatVnd(DepositRequest.MAX_DEPOSIT));
        }

        Connection c = null;
        try {
            c = DatabaseConnection.getConnection();
            c.setAutoCommit(false);

            // 1. Đọc balance hiện tại (trong transaction để lock row)
            double currentBalance = readBalance(c, bidderId);

            // 2. Kiểm tra giới hạn tổng số dư
            double newBalance = currentBalance + amount;
            if (newBalance > MAX_BALANCE) {
                throw new ValidationException(
                        "Số dư vượt giới hạn tối đa " + formatVnd(MAX_BALANCE)
                                + ". Hiện có: " + formatVnd(currentBalance)
                                + ", nạp thêm: " + formatVnd(amount));
            }

            // 3. Cập nhật balance
            updateBalance(c, bidderId, newBalance);

            c.commit();

            LocalDateTime now = LocalDateTime.now();
            log.info("Deposit OK: bidderId={} amount={} newBalance={}",
                    bidderId, amount, newBalance);

            return new DepositResponse(amount, newBalance, now);

        } catch (SQLException ex) {
            if (c != null) {
                try { c.rollback(); } catch (SQLException ignore) {}
            }
            log.error("Deposit DB error: bidderId={}", bidderId, ex);
            throw new ValidationException("Lỗi DB khi nạp tiền: " + ex.getMessage());
        } catch (ValidationException ex) {
            if (c != null) {
                try { c.rollback(); } catch (SQLException ignore) {}
            }
            throw ex;
        } finally {
            if (c != null) {
                try { c.setAutoCommit(true); } catch (SQLException ignore) {}
                try { c.close(); }           catch (SQLException ignore) {}
            }
        }
    }

    // ---------- Helpers ----------

    /** Đọc balance hiện tại trong transaction đang mở. */
    private double readBalance(Connection c, long bidderId)
            throws SQLException, ValidationException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT account_balance FROM users WHERE id = ? AND role = 'BIDDER'")) {
            ps.setLong(1, bidderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new ValidationException("Bidder không tồn tại: " + bidderId);
                }
                return rs.getDouble("account_balance");
            }
        }
    }

    /** Cập nhật balance trong transaction đang mở. */
    private void updateBalance(Connection c, long bidderId, double newBalance)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET account_balance = ? WHERE id = ? AND role = 'BIDDER'")) {
            ps.setDouble(1, newBalance);
            ps.setLong(2, bidderId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Update balance affected 0 rows (bidderId=" + bidderId + ")");
            }
        }
    }

    private static String formatVnd(double v) {
        try { return String.format("%,.0f đ", v); }
        catch (Exception ex) { return String.valueOf(v); }
    }
}
