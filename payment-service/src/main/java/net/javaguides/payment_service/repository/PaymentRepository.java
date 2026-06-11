package net.javaguides.payment_service.repository;

import net.javaguides.payment_service.entity.Payment;
import net.javaguides.payment_service.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    Payment findByOrderId(String orderId);

    long countByUserIdAndCreatedAtAfter(Long userId, Date since);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.userId = :userId AND p.createdAt >= :since " +
           "AND p.status IN ('AUTHORIZED', 'CAPTURED', 'SETTLED', 'SUCCESS')")
    BigDecimal sumAmountByUserIdAndCreatedAtAfter(@Param("userId") Long userId,
                                                  @Param("since") Date since);

    List<Payment> findByStatus(PaymentStatus status);
}