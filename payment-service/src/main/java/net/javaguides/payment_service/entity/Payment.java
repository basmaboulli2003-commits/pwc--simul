package net.javaguides.payment_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.javaguides.common_lib.entity.AbstractEntity;
import net.javaguides.payment_service.entity.converter.PaymentStatusConverter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "payments")
public class Payment extends AbstractEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String orderId;

    @Column
    private Long userId;

    @Column
    private String userEmail;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String paymentMethod;

    @Convert(converter = PaymentStatusConverter.class)
    private PaymentStatus status;

    @Column
    private String authorizationCode;

    @Column(length = 500)
    private String declineReason;

    @Column
    private LocalDateTime authorizedAt;

    @Column
    private LocalDateTime capturedAt;

    @Column
    private LocalDateTime settledAt;
}