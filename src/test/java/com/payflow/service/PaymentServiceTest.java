package com.payflow.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.payflow.dto.CreatePaymentRequest;
import com.payflow.entity.Payment;
import com.payflow.enums.PaymentStatus;
import com.payflow.exception.IdempotencyConflictException;
import com.payflow.exception.PaymentNotFoundException;
import com.payflow.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.*;

class PaymentServiceTest {
    @Mock PaymentRepository repository;
    @Mock PaymentGateway gateway;
    @InjectMocks PaymentService service;
    private final UUID customer = UUID.randomUUID();
    private final CreatePaymentRequest request = new CreatePaymentRequest(customer, new BigDecimal("99.90"), "BRL");

    @BeforeEach void setUp() { MockitoAnnotations.openMocks(this); }

    @Test void createsApprovedPayment() {
        when(repository.reserveIdempotencyKey(any(), any(), any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(1);
        when(repository.findByIdempotencyKey("key")).thenReturn(Optional.empty());
        when(repository.findByIdempotencyKeyForUpdate("key")).thenAnswer(invocation -> Optional.of(
                new Payment(UUID.randomUUID(), customer, request.amount(), request.currency(),
                        PaymentStatus.PENDING, "key", fingerprint(request), java.time.LocalDateTime.now())));
        when(gateway.process(any())).thenReturn(PaymentStatus.APPROVED);
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals(PaymentStatus.APPROVED, service.create(request, "key").status());
    }
    @Test void repeatsSameKeyWithoutCreatingAgain() {
        when(repository.reserveIdempotencyKey(any(), any(), any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(1);
        when(repository.findByIdempotencyKeyForUpdate("key")).thenAnswer(invocation -> Optional.of(
                new Payment(UUID.randomUUID(), customer, request.amount(), request.currency(),
                        PaymentStatus.PENDING, "key", fingerprint(request), java.time.LocalDateTime.now())));
        when(gateway.process(any())).thenReturn(PaymentStatus.APPROVED);
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var first = service.create(request, "key");
        Payment payment = new Payment(first.id(), customer, request.amount(), request.currency(),
                first.status(), "key", fingerprint(request), first.createdAt());
        when(repository.reserveIdempotencyKey(any(), any(), any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(0);
        when(repository.findByIdempotencyKeyForUpdate("key")).thenReturn(Optional.of(payment));
        assertEquals(first.id(), service.create(request, "key").id());
        verify(repository, times(1)).saveAndFlush(any());
        verify(gateway, times(1)).process(any());
    }
    @Test void rejectsDifferentPayloadForSameKey() {
        Payment payment = payment(PaymentStatus.APPROVED, "key");
        when(repository.reserveIdempotencyKey(any(), any(), any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(0);
        when(repository.findByIdempotencyKeyForUpdate("key")).thenReturn(Optional.of(payment));
        assertThrows(IdempotencyConflictException.class, () -> service.create(
                new CreatePaymentRequest(customer, new BigDecimal("10.00"), "BRL"), "key"));
    }
    @Test void supportsRejectedAndFailedGatewayOutcomes() {
        when(repository.reserveIdempotencyKey(any(), any(), any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(1);
        when(repository.findByIdempotencyKeyForUpdate(anyString())).thenAnswer(invocation -> Optional.of(
                new Payment(UUID.randomUUID(), customer, request.amount(), request.currency(),
                        PaymentStatus.PENDING, invocation.getArgument(0), fingerprint(request),
                        java.time.LocalDateTime.now())));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(gateway.process(any())).thenReturn(PaymentStatus.REJECTED);
        assertEquals(PaymentStatus.REJECTED, service.create(request, "rejected").status());
        when(gateway.process(any())).thenReturn(PaymentStatus.FAILED);
        assertEquals(PaymentStatus.FAILED, service.create(request, "failed").status());
    }
    @Test void requiresIdempotencyKey() {
        assertThrows(IllegalArgumentException.class, () -> service.create(request, " "));
    }
    @Test void reportsMissingPayment() {
        when(repository.findById(any())).thenReturn(Optional.empty());
        assertThrows(PaymentNotFoundException.class, () -> service.find(UUID.randomUUID()));
    }
    private Payment payment(PaymentStatus status, String key) {
        return new Payment(UUID.randomUUID(), customer, request.amount(), request.currency(), status, key,
                "unused",
                java.time.LocalDateTime.now());
    }
    private String fingerprint(CreatePaymentRequest value) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest((value.customerId() + "|" + value.amount().toPlainString() + "|" + value.currency())
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
