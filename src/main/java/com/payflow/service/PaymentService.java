package com.payflow.service;

import com.payflow.dto.CreatePaymentRequest;
import com.payflow.dto.PaymentResponse;
import com.payflow.entity.Payment;
import com.payflow.enums.PaymentStatus;
import com.payflow.exception.*;
import com.payflow.repository.PaymentRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private final PaymentRepository repository;
    private final PaymentGateway gateway;

    public PaymentService(PaymentRepository repository, PaymentGateway gateway) {
        this.repository = repository;
        this.gateway = gateway;
    }

    @Transactional
    public PaymentResponse create(CreatePaymentRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }

        idempotencyKey = idempotencyKey.trim();
        if (idempotencyKey.length() > 255) {
            throw new IllegalArgumentException("Idempotency-Key must be at most 255 characters");
        }

        String fingerprint = fingerprint(request);
        LocalDateTime now = LocalDateTime.now();
        int reserved = repository.reserveIdempotencyKey(UUID.randomUUID(), request.customerId(), request.amount(),
                request.currency(), idempotencyKey, fingerprint, now);

        Payment payment = repository.findByIdempotencyKeyForUpdate(idempotencyKey)
                .orElseThrow(() -> new IllegalStateException("Payment reservation was not found"));

        if (reserved == 0) {
            return sameOrConflict(payment, fingerprint);
        }

        payment.setStatus(gateway.process(payment));
        return toResponse(repository.saveAndFlush(payment));
    }

    @Transactional(readOnly = true)
    public PaymentResponse find(UUID id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> findAll(UUID customerId, PaymentStatus status, Pageable pageable) {
        int pageSize = Math.min(pageable.getPageSize(), 100);

        Pageable ordered = PageRequest.of(pageable.getPageNumber(), pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Payment> page = customerId != null && status != null ? repository.findByCustomerIdAndStatus(customerId, status, ordered)
                : customerId != null ? repository.findByCustomerId(customerId, ordered)
                : status != null ? repository.findByStatus(status, ordered) : repository.findAll(ordered);

        return page.map(this::toResponse);
    }

    private PaymentResponse sameOrConflict(Payment payment, String fingerprint) {
        if (!payment.getRequestFingerprint().equals(fingerprint)) {
            throw new IdempotencyConflictException("Idempotency-Key was already used with a different request");
        }

        return toResponse(payment);
    }

    private String fingerprint(CreatePaymentRequest request) {
        String value = request.customerId() + "|" + request.amount().setScale(2).toPlainString()
                + "|" + request.currency().trim().toUpperCase(java.util.Locale.ROOT);
                
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(p.getId(), p.getCustomerId(), p.getAmount(), p.getCurrency(),
                p.getStatus(), p.getIdempotencyKey(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
