package net.javaguides.payment_service.service;

import net.javaguides.payment_service.entity.Payment;
import net.javaguides.payment_service.entity.PaymentStatus;
import net.javaguides.payment_service.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SettlementScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettlementScheduler.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AuditLogger auditLogger;

    /**
     * Tourne toutes les 60 secondes.
     * Passe les paiements CAPTURED en SETTLED.
     * En production : batch quotidien a 23h.
     */
    @Scheduled(fixedDelay = 60000)
    public void processSettlement() {
        List<Payment> toSettle = paymentRepository.findByStatus(PaymentStatus.CAPTURED);

        if (toSettle.isEmpty()) {
            return;
        }

        LOGGER.info("SETTLEMENT_BATCH_STARTED count={}", toSettle.size());

        for (Payment payment : toSettle) {
            payment.setStatus(PaymentStatus.SETTLED);
            payment.setSettledAt(LocalDateTime.now());
            paymentRepository.save(payment);

            auditLogger.logSettlement(payment);
        }

        LOGGER.info("SETTLEMENT_BATCH_COMPLETED count={}", toSettle.size());
    }
}