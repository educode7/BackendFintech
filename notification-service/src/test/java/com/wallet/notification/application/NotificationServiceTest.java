package com.wallet.notification.application;

import com.wallet.notification.domain.Notification;
import com.wallet.notification.domain.NotificationStatus;
import com.wallet.notification.domain.NotificationType;
import com.wallet.notification.infrastructure.persistence.NotificationJpaRepository;
import com.wallet.shared.event.EventMetadata;
import com.wallet.shared.event.PaymentCompletedEvent;
import com.wallet.shared.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationJpaRepository repository;

    @Mock
    private NotificationSender sender;

    @InjectMocks
    private NotificationService service;

    @Test
    @DisplayName("handlePaymentCompleted: 1ª persistencia como PENDING, sender.send, 2ª persistencia como SENT")
    void handlePaymentCompleted_envia_marcaSent() {
        PaymentCompletedEvent event = sampleEvent("pay-1", "user-1", "100.00");
        java.util.List<Notification> snapshots = new java.util.ArrayList<>();
        given(repository.save(any(Notification.class))).willAnswer(inv -> {
            Notification orig = inv.getArgument(0);
            snapshots.add(clone(orig));
            return orig;
        });

        service.handlePaymentCompleted(event);

        assertThat(snapshots).hasSize(2);
        assertThat(snapshots.get(0).getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(snapshots.get(0).getUserId()).isEqualTo("user-1");
        assertThat(snapshots.get(0).getType()).isEqualTo(NotificationType.EMAIL);
        assertThat(snapshots.get(0).getSubject()).isEqualTo("Payment COMPLETED");
        assertThat(snapshots.get(0).getBody()).contains("100.00 USD");
        assertThat(snapshots.get(0).getBody()).contains("pay-1");
        assertThat(snapshots.get(0).getProcessedEventId()).isEqualTo(event.metadata().eventId());

        assertThat(snapshots.get(1).getStatus()).isEqualTo(NotificationStatus.SENT);

        InOrder ordered = inOrder(repository, sender);
        ordered.verify(repository).save(any(Notification.class));
        ordered.verify(sender).send(any(Notification.class));
        ordered.verify(repository).save(any(Notification.class));
    }

    private static Notification clone(Notification n) {
        Notification copy = new Notification();
        copy.setId(n.getId());
        copy.setUserId(n.getUserId());
        copy.setType(n.getType());
        copy.setSubject(n.getSubject());
        copy.setBody(n.getBody());
        copy.setStatus(n.getStatus());
        copy.setProcessedEventId(n.getProcessedEventId());
        copy.setCreatedAt(n.getCreatedAt());
        copy.setSentAt(n.getSentAt());
        return copy;
    }

    @Test
    @DisplayName("handlePaymentCompleted: si el sender lanza RuntimeException, marca FAILED y re-lanza")
    void handlePaymentCompleted_senderFalla_marcaFailedYRelanza() {
        PaymentCompletedEvent event = sampleEvent("pay-2", "user-2", "50.00");
        given(repository.save(any(Notification.class))).willAnswer(inv -> inv.getArgument(0));
        org.mockito.Mockito.doThrow(new RuntimeException("smtp down"))
            .when(sender).send(any(Notification.class));

        assertThatThrownBy(() -> service.handlePaymentCompleted(event))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("smtp down");

        InOrder ordered = inOrder(repository, sender);
        ordered.verify(repository).save(any(Notification.class));
        ordered.verify(sender).send(any(Notification.class));
        ArgumentCaptor<Notification> failedSave = ArgumentCaptor.forClass(Notification.class);
        ordered.verify(repository).save(failedSave.capture());
        assertThat(failedSave.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    @DisplayName("handlePaymentCompleted: el subject incluye el status literal del pago")
    void handlePaymentCompleted_subjectIncluyeStatus() {
        PaymentCompletedEvent event = sampleEvent("pay-3", "user-3", "10.00");
        given(repository.save(any(Notification.class))).willAnswer(inv -> inv.getArgument(0));

        service.handlePaymentCompleted(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        List<Notification> all = captor.getAllValues();
        assertThat(all.get(0).getSubject()).isEqualTo("Payment COMPLETED");
    }

    @Test
    @DisplayName("handlePaymentCompleted: el body contiene amount formateado y paymentId")
    void handlePaymentCompleted_bodyIncluyeAmountYId() {
        PaymentCompletedEvent event = sampleEvent("pay-4", "user-4", "75.50");
        given(repository.save(any(Notification.class))).willAnswer(inv -> inv.getArgument(0));

        service.handlePaymentCompleted(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        Notification first = captor.getAllValues().get(0);
        assertThat(first.getBody()).contains("75.50 USD");
        assertThat(first.getBody()).contains("pay-4");
    }

    @Test
    @DisplayName("handlePaymentCompleted: el sender NO se invoca cuando el save inicial falla")
    void handlePaymentCompleted_saveInicialFalla_noEnvia() {
        PaymentCompletedEvent event = sampleEvent("pay-5", "user-5", "10.00");
        given(repository.save(any(Notification.class)))
            .willThrow(new RuntimeException("db error"));

        assertThatThrownBy(() -> service.handlePaymentCompleted(event))
            .isInstanceOf(RuntimeException.class);

        verifyNoInteractions(sender);
    }

    private static PaymentCompletedEvent sampleEvent(String paymentId, String userId, String amount) {
        return new PaymentCompletedEvent(
            paymentId,
            userId,
            new Money(new BigDecimal(amount), "USD"),
            "COMPLETED",
            new EventMetadata(UUID.randomUUID().toString(), Instant.now(), "corr-1", 1)
        );
    }
}
