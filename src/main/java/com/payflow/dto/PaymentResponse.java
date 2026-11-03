package com.payflow.dto;

import com.payflow.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(UUID id, UUID customerId, BigDecimal amount, String currency,
                              PaymentStatus status, String idempotencyKey,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
}
