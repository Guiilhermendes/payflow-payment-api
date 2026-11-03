package com.payflow.controller;

import com.payflow.dto.*;
import com.payflow.enums.PaymentStatus;
import com.payflow.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService service;

    public PaymentController(PaymentService service) { 
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a payment", description = "Idempotency-Key is mandatory and prevents duplicate charges.")
    @ApiResponse(responseCode = "201", description = "Payment created")
    public ResponseEntity<PaymentResponse> create(@RequestHeader(value = "Idempotency-Key", required = false) String key,
                                                   @Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, key));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a payment")
    public PaymentResponse find(@PathVariable UUID id) { return service.find(id); }

    @GetMapping
    @Operation(summary = "List payments")
    public Page<PaymentResponse> findAll(@RequestParam(required = false) UUID customerId,
                                         @RequestParam(required = false) PaymentStatus status,
                                         @PageableDefault(size = 20) Pageable pageable) {
        return service.findAll(customerId, status, pageable);
    }
}
