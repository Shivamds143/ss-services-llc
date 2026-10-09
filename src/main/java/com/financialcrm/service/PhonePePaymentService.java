package com.financialcrm.service;

import com.financialcrm.entity.UtilityBill;

public interface PhonePePaymentService {

    record PaymentInitResult(String redirectUrl, String merchantTransactionId) {}

    record PaymentStatusResult(
            boolean success,
            String state,
            String responseCode,
            String message,
            String paymentInstrumentType,
            String gatewayTransactionId,
            String referenceId
    ) {}

    boolean isConfigured();

    PaymentInitResult initiateUtilityBillPayment(UtilityBill bill, Long userId);

    PaymentStatusResult checkPaymentStatus(String merchantTransactionId);
}
