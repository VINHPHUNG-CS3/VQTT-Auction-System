package com.bt.client.controller;

import com.bt.client.net.AuctionClient;
import com.bt.client.net.AuctionClientException;
import com.bt.client.session.Session;
import com.bt.client.ui.SceneManager;
import com.bt.client.util.Dialogs;
import com.bt.client.util.MoneyFormat;
import com.bt.shared.protocol.dto.DepositResponse;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.text.DecimalFormat;
import java.text.ParseException;

/**
 * Controller màn hình nạp tiền (deposit.fxml).
 *
 * Luồng:
 *  1. User nhập số tiền → bấm "Nạp tiền"
 *  2. Validate phía client (> 0, <= MAX)
 *  3. Gửi DEPOSIT_REQUEST lên server qua background thread
 *  4. Nhận DepositResponse → cập nhật Session + hiển thị balance mới
 */
public class DepositController {

    // Preset amount buttons (VNĐ)
    private static final double[] PRESETS = {
            100_000, 500_000, 1_000_000, 5_000_000, 10_000_000
    };

    @FXML private Label currentBalanceLabel;
    @FXML private TextField amountField;
    @FXML private Button depositBtn;
    @FXML private Label statusLabel;

    // Nút preset — đặt tên theo thứ tự PRESETS[]
    @FXML private Button preset1Btn;  // 100k
    @FXML private Button preset2Btn;  // 500k
    @FXML private Button preset3Btn;  // 1tr
    @FXML private Button preset4Btn;  // 5tr
    @FXML private Button preset5Btn;  // 10tr

    private final AuctionClient client = new AuctionClient();

    @FXML
    public void initialize() {
        refreshBalanceLabel();

        // Gán giá trị preset vào các nút
        Button[] presetBtns = { preset1Btn, preset2Btn, preset3Btn, preset4Btn, preset5Btn };
        for (int i = 0; i < presetBtns.length; i++) {
            final double amount = PRESETS[i];
            presetBtns[i].setText(MoneyFormat.vnd(amount));
            presetBtns[i].setOnAction(e -> amountField.setText(String.valueOf((long) amount)));
        }

        // Chỉ cho nhập số và dấu chấm
        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) {
                amountField.setText(oldVal);
            }
        });
    }

    @FXML
    public void handleDeposit() {
        String raw = amountField.getText().trim();
        if (raw.isEmpty()) {
            statusLabel.setText("⚠ Vui lòng nhập số tiền cần nạp.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            statusLabel.setText("⚠ Số tiền không hợp lệ.");
            return;
        }

        if (amount < 1_000) {
            statusLabel.setText("⚠ Số tiền tối thiểu là " + MoneyFormat.vnd(1_000));
            return;
        }
        if (amount > 1_000_000_000) {
            statusLabel.setText("⚠ Mỗi lần nạp tối đa " + MoneyFormat.vnd(1_000_000_000));
            return;
        }

        setLoading(true);
        statusLabel.setText("Đang xử lý...");

        final double finalAmount = amount;
        new Thread(() -> {
            try {
                DepositResponse resp = client.deposit(finalAmount);
                Platform.runLater(() -> onDepositSuccess(resp));
            } catch (AuctionClientException ex) {
                Platform.runLater(() -> onDepositFail(ex.getMessage()));
            }
        }, "deposit-thread").start();
    }

    private void onDepositSuccess(DepositResponse resp) {
        // Cập nhật session để số dư hiển thị đúng ở các màn hình khác
        Session.get().updateBalance(resp.getNewBalance());

        refreshBalanceLabel();
        amountField.clear();
        statusLabel.setText("✅ Nạp thành công "
                + MoneyFormat.vnd(resp.getDepositedAmount()) + "!");
        setLoading(false);

        Dialogs.info("Nạp tiền thành công",
                "Đã nạp: " + MoneyFormat.vnd(resp.getDepositedAmount())
                + "\nSố dư mới: " + MoneyFormat.vnd(resp.getNewBalance()));
    }

    private void onDepositFail(String message) {
        statusLabel.setText("❌ " + message);
        setLoading(false);
    }

    @FXML
    public void goBack() {
        SceneManager.get().switchTo("dashboard");
    }

    // ---------- Helpers ----------

    private void refreshBalanceLabel() {
        currentBalanceLabel.setText(
                "Số dư hiện tại: " + MoneyFormat.vnd(Session.get().getAccountBalance()));
    }

    private void setLoading(boolean loading) {
        depositBtn.setDisable(loading);
        amountField.setDisable(loading);
    }
}
