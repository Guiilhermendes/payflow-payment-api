package com.payflow.entity;

import com.payflow.enums.PaymentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_customer", columnList = "customer_id"),
        @Index(name = "idx_payments_status", columnList = "status"),
        @Index(name = "idx_payments_created", columnList = "created_at")
})
public class Payment {
    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentStatus status;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Payment() {}

    public Payment(UUID id, UUID customerId, BigDecimal amount, String currency,
                   PaymentStatus status, String idempotencyKey, String requestFingerprint,
                   LocalDateTime createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.status = status; 
        this.idempotencyKey = idempotencyKey; 
        this.requestFingerprint = requestFingerprint;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getId() { 
        return id;
    }

    public UUID getCustomerId() { 
        return customerId;
    }

    public BigDecimal getAmount() { 
        return amount;
    }

    public String getCurrency() { 
        return currency;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) { 
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
}
