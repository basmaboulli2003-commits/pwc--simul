package net.javaguides.payment_service.service;

import net.javaguides.common_lib.dto.order.OrderEvent;
import net.javaguides.payment_service.entity.Payment;
import net.javaguides.payment_service.entity.PaymentStatus;
import net.javaguides.payment_service.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AcquirerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquirerService.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private FraudDetectionService fraudService;

    @Autowired
    private AuditLogger auditLogger;

    /**
     * Methode principale appelee par le KafkaListener
     * Orchestre les 2 phases : Authorization + Capture
     * (Settlement est fait par SettlementScheduler)
     *
     * @param event OrderEvent recu de Kafka avec email DEJA dechiffre par OrderConsumer
     */
    @Transactional
    public Payment processPayment(OrderEvent event) {
        Long userId = event.getOrderDTO().getUserId();
        String orderId = event.getOrderDTO().getOrderId();
        BigDecimal amount = calculateTotal(event);
        String method = event.getPaymentMethod();
        String email = event.getEmail();  // Deja dechiffre par OrderConsumer

        // PHASE 0 : INITIATION
        auditLogger.logInitiation(userId, orderId, amount, method);

        try {
            // PHASE 1 : AUTHORIZATION
            Payment payment = authorize(userId, email, orderId, amount, method);

            // Si DECLINED, on arrete
            if (payment.getStatus() == PaymentStatus.DECLINED) {
                return payment;
            }

            // PHASE 2 : CAPTURE (immediat dans la simulation)
            capture(payment);

            // PHASE 3 : SETTLEMENT sera fait par SettlementScheduler
            return payment;

        } catch (Exception e) {
            LOGGER.error("Payment processing failed", e);
            auditLogger.logFailure(userId, orderId, amount, e.getMessage());
            throw e;
        }
    }

    /**
     * PHASE 1 : Authorization
     * Verifie la fraude, reserve le montant
     */
    private Payment authorize(Long userId, String email, String orderId,
                              BigDecimal amount, String method) {

        // Verification fraude
        String fraudReason = fraudService.checkFraud(userId, amount);

        if (fraudReason != null) {
            // DECLINED
            Payment declined = new Payment();
            declined.setId(UUID.randomUUID().toString());
            declined.setOrderId(orderId);
            declined.setUserId(userId);
            declined.setUserEmail(email);
            declined.setAmount(amount);
            declined.setPaymentMethod(method);
            declined.setStatus(PaymentStatus.DECLINED);
            declined.setDeclineReason(fraudReason);

            Payment saved = paymentRepository.save(declined);
            auditLogger.logDecline(userId, orderId, amount, fraudReason);
            return saved;
        }

        // AUTHORIZED
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID().toString());
        payment.setOrderId(orderId);
        payment.setUserId(userId);
        payment.setUserEmail(email);
        payment.setAmount(amount);
        payment.setPaymentMethod(method);
        payment.setStatus(PaymentStatus.AUTHORIZED);
        payment.setAuthorizationCode(generateAuthCode());
        payment.setAuthorizedAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);
        auditLogger.logAuthorization(saved);
        return saved;
    }

    /**
     * PHASE 2 : Capture (debit effectif simule)
     */
    private Payment capture(Payment payment) {
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setCapturedAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);
        auditLogger.logCapture(saved);
        return saved;
    }

    /**
     * Genere un code d'autorisation style "AUTH-A3B7F2"
     */
    private String generateAuthCode() {
        return "AUTH-" + UUID.randomUUID().toString()
                            .substring(0, 6)
                            .toUpperCase();
    }

    /**
     * Calcule le total des orderItems
     */
    private BigDecimal calculateTotal(OrderEvent event) {
    if (event.getOrderDTO() == null || event.getOrderDTO().getOrderItems() == null) {
        return BigDecimal.ZERO;
    }
    return event.getOrderDTO().getOrderItems().stream()
            .map(item -> item.getPrice() != null
                    ? item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                    : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
}
}