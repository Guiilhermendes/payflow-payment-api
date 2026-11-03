package com.payflow.repository;

import com.payflow.entity.Payment;
import com.payflow.enums.PaymentStatus;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.idempotencyKey = :idempotencyKey")
    Optional<Payment> findByIdempotencyKeyForUpdate(@Param("idempotencyKey") String idempotencyKey);

    @Modifying
    @Query(value = """
            insert into payments
                (id, customer_id, amount, currency, status, idempotency_key,
                 request_fingerprint, created_at, updated_at)
            values (:id, :customerId, :amount, :currency, 'PENDING',
                    :idempotencyKey, :requestFingerprint, :createdAt, :createdAt)
            on conflict (idempotency_key) do nothing
            """, nativeQuery = true)
    int reserveIdempotencyKey(@Param("id") UUID id, @Param("customerId") UUID customerId,
                              @Param("amount") java.math.BigDecimal amount, @Param("currency") String currency,
                              @Param("idempotencyKey") String idempotencyKey,
                              @Param("requestFingerprint") String requestFingerprint,
                              @Param("createdAt") java.time.LocalDateTime createdAt);

    Page<Payment> findByCustomerIdAndStatus(UUID customerId, PaymentStatus status, Pageable pageable);
    Page<Payment> findByCustomerId(UUID customerId, Pageable pageable);
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);
}
