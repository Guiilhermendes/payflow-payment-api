package com.payflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID customerId,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull @Pattern(regexp = "BRL", message = "currency must be BRL") String currency) {
}
