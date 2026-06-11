package net.javaguides.payment_service.service;

import net.javaguides.payment_service.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

@Service
public class FraudDetectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FraudDetectionService.class);

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000");
    private static final int MAX_PAYMENTS_PER_5MIN = 3;
    private static final BigDecimal DAILY_LIMIT = new BigDecimal("50000");

    @Autowired
    private PaymentRepository paymentRepository;

    /**
     * Verifie 4 regles de fraude.
     * @return null si OK, sinon la raison du refus
     */
    public String checkFraud(Long userId, BigDecimal amount) {

        // Regle 1 : Montant trop eleve
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            LOGGER.warn("FRAUD_RULE userId={} amount={} rule=MAX_AMOUNT_EXCEEDED", userId, amount);
            return "Amount exceeds maximum allowed: " + MAX_AMOUNT;
        }

        // Regle 2 : Velocity (trop de paiements en 5 min)
        if (userId != null) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, -5);
            Date fiveMinutesAgo = cal.getTime();

            long recentCount = paymentRepository.countByUserIdAndCreatedAtAfter(userId, fiveMinutesAgo);
            if (recentCount >= MAX_PAYMENTS_PER_5MIN) {
                LOGGER.warn("FRAUD_RULE userId={} rule=VELOCITY_LIMIT count={}", userId, recentCount);
                return "Too many payment attempts in short time period";
            }

            // Regle 3 : Limite journaliere
            Calendar startOfDay = Calendar.getInstance();
            startOfDay.set(Calendar.HOUR_OF_DAY, 0);
            startOfDay.set(Calendar.MINUTE, 0);
            startOfDay.set(Calendar.SECOND, 0);
            startOfDay.set(Calendar.MILLISECOND, 0);

            BigDecimal dailyTotal = paymentRepository.sumAmountByUserIdAndCreatedAtAfter(
                userId, startOfDay.getTime());

            if (dailyTotal != null && dailyTotal.add(amount).compareTo(DAILY_LIMIT) > 0) {
                LOGGER.warn("FRAUD_RULE userId={} rule=DAILY_LIMIT_EXCEEDED total={}", userId, dailyTotal);
                return "Daily payment limit exceeded";
            }
        }

        // Regle 4 : Montant invalide
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            LOGGER.warn("FRAUD_RULE userId={} amount={} rule=INVALID_AMOUNT", userId, amount);
            return "Invalid amount";
        }

        return null;  // Tout est OK
    }
}