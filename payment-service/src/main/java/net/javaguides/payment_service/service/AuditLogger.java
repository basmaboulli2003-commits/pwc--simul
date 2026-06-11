package net.javaguides.payment_service.service;

import net.javaguides.payment_service.entity.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AuditLogger {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    public void logInitiation(Long userId, String orderId, BigDecimal amount, String method) {
        AUDIT.info("EVENT=PAYMENT_INITIATED userId={} orderId={} amount={} method={}",
                userId, orderId, amount, method);
    }

    public void logAuthorization(Payment payment) {
        AUDIT.info("EVENT=PAYMENT_AUTHORIZED paymentId={} userId={} orderId={} amount={} authCode={}",
                payment.getId(), payment.getUserId(), payment.getOrderId(),
                payment.getAmount(), payment.getAuthorizationCode());
    }

    public void logCapture(Payment payment) {
        AUDIT.info("EVENT=PAYMENT_CAPTURED paymentId={} userId={} orderId={} amount={}",
                payment.getId(), payment.getUserId(), payment.getOrderId(),
                payment.getAmount());
    }

    public void logSettlement(Payment payment) {
        AUDIT.info("EVENT=PAYMENT_SETTLED paymentId={} userId={} orderId={} amount={}",
                payment.getId(), payment.getUserId(), payment.getOrderId(),
                payment.getAmount());
    }

    public void logDecline(Long userId, String orderId, BigDecimal amount, String reason) {
        AUDIT.warn("EVENT=PAYMENT_DECLINED userId={} orderId={} amount={} reason={}",
                userId, orderId, amount, reason);
    }

    public void logFailure(Long userId, String orderId, BigDecimal amount, String error) {
        AUDIT.error("EVENT=PAYMENT_FAILED userId={} orderId={} amount={} error={}",
                userId, orderId, amount, error);
    }
}