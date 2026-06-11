package net.javaguides.payment_service.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
    PENDING("Pending"),
    AUTHORIZED("Authorized"),
    CAPTURED("Captured"),
    SETTLED("Settled"),
    DECLINED("Declined"),
    SUCCESS("Success"),
    FAILED("Failed"),
    REFUND("Refund");

    public final String label;
}