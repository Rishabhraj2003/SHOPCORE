package com.shopcore.service;

import com.shopcore.enums.AuditAction;
import com.shopcore.enums.PaymentMethod;
import com.shopcore.enums.PaymentStatus;
import com.shopcore.exception.PaymentFailedException;
import com.shopcore.exception.ShopCoreException;
import com.shopcore.factory.PaymentFactory;
import com.shopcore.logging.AuditLogger;
import com.shopcore.model.Payment;
import com.shopcore.repository.PaymentRepository;
import com.shopcore.util.IdGenerator;
import com.shopcore.util.MoneyUtil;

import java.util.Optional;

public class PaymentService {
    private final PaymentRepository paymentRepo;
    private final AuditLogger audit = AuditLogger.getInstance();

    public PaymentService(PaymentRepository paymentRepo) { this.paymentRepo = paymentRepo; }

    /** Runs the simulated payment. A declined payment is saved as FAILED and then PaymentFailedException is thrown. */
    public Payment pay(String orderId, String userId, double amount, PaymentMethod method) {
        PaymentStatus result = PaymentFactory.getProcessor(method).process(amount);
        Payment payment = new Payment(IdGenerator.next("PAY"), orderId, userId, amount, method, result);
        paymentRepo.save(payment);

        if (result == PaymentStatus.FAILED) {
            audit.log(userId, AuditAction.PAYMENT_FAILED, orderId, amount, "FAILED");
            throw new PaymentFailedException(method + " payment of " + MoneyUtil.format(amount)
                    + " was declined (amount exceeds the " + method + " limit). Try another payment method.");
        }
        audit.log(userId, AuditAction.PAYMENT_SUCCESS, orderId, amount, result == PaymentStatus.PENDING ? "PENDING (COD)" : "SUCCESS");
        return payment;
    }

    /** Paid online -> REFUNDED. Cash on delivery (nothing collected yet) -> CANCELLED. */
    public Payment refund(String orderId) {
        Payment payment = findByOrderId(orderId).orElseThrow(() -> new ShopCoreException("No payment found for order " + orderId));
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            payment.setStatus(PaymentStatus.REFUNDED);
            audit.log(payment.getUserId(), AuditAction.PAYMENT_REFUNDED, orderId, payment.getAmount(), "REFUNDED");
        } else if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.CANCELLED);
        }
        paymentRepo.save(payment);
        return payment;
    }

    public Payment markCodPaid(String orderId) {
        Payment payment = findByOrderId(orderId).orElseThrow(() -> new ShopCoreException("No payment found for order " + orderId));
        if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.SUCCESS);
            paymentRepo.save(payment);
            audit.log(payment.getUserId(), AuditAction.PAYMENT_SUCCESS, orderId, payment.getAmount(), "SUCCESS (COD collected)");
        }
        return payment;
    }

    public Optional<Payment> findByOrderId(String orderId) { return paymentRepo.findByOrderId(orderId); }
}
