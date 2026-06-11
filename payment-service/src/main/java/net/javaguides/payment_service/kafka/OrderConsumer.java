package net.javaguides.payment_service.kafka;

import net.javaguides.common_lib.dto.order.OrderEvent;
import net.javaguides.common_lib.encryption.EncryptionService;
import net.javaguides.payment_service.entity.PaymentStatus;
import net.javaguides.payment_service.service.AcquirerService;
import net.javaguides.payment_service.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class OrderConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderConsumer.class);

    private final PaymentService paymentService;
    private final EncryptionService encryptionService;
    private final AcquirerService acquirerService;

    public OrderConsumer(PaymentService paymentService,
                         EncryptionService encryptionService,
                         AcquirerService acquirerService) {
        this.paymentService = paymentService;
        this.encryptionService = encryptionService;
        this.acquirerService = acquirerService;
    }

    @KafkaListener(topics = "${spring.kafka.order-topic.name}",
                   groupId = "${spring.kafka.consumer.group-id}")
    public void consume(OrderEvent orderEvent) {
        try {
            LOGGER.info("OrderDTO event received in payment service -> {}", orderEvent.toString());

            // Dechiffrer l'email
            if (orderEvent.getEmail() != null) {
                orderEvent.setEmail(encryptionService.decrypt(orderEvent.getEmail()));
            }

            // Gestion REFUND
            if ("REFUND".equals(orderEvent.getMessage())) {
                paymentService.refundPayment(orderEvent.getOrderDTO().getOrderId());
                return;
            }

            // Gestion PAID (update status)
            if ("PAID".equals(orderEvent.getMessage())) {
                paymentService.updateStatusPayment(orderEvent.getOrderDTO().getOrderId(), PaymentStatus.SUCCESS);
                return;
            }

            // Nouveau paiement : on passe par l'AcquirerService
            acquirerService.processPayment(orderEvent);

        } catch (Exception e) {
            LOGGER.warn("Error message -> {}", e.getMessage());
        }
    }
}