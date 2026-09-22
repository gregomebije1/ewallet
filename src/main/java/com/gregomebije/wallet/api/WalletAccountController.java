package com.gregomebije.wallet.api;

import com.gregomebije.wallet.model.Models.*;
import com.gregomebije.wallet.service.account.*;
import com.gregomebije.wallet.service.transfer.*;
import com.gregomebije.wallet.service.funding.*;
import com.gregomebije.wallet.exception.Exceptions.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletAccountController {
    private final WalletAccountService service;
    public WalletAccountController(WalletAccountService service) { this.service = service; }

    @PostMapping("/accounts")
    ResponseEntity<?> create(@Valid @RequestBody WalletAccountRequestDto r) {
        WalletAccountResponse x = service.provision(
                new WalletAccountRequest(r.userId(), r.complianceTier(), r.currency()));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("status", "success", "data", x));
    }

    public record WalletAccountRequestDto(
            @NotBlank String userId,
            @NotNull ComplianceTier complianceTier,
            @NotBlank String currency) {}
}



@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(DuplicateRequestException.class)
    ResponseEntity<?> duplicate(DuplicateRequestException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("status", "error", "message", e.getMessage()));
    }
    @ExceptionHandler(InvalidWebhookSignatureException.class)
    ResponseEntity<?> signature(InvalidWebhookSignatureException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "error"));
    }
    @ExceptionHandler({IllegalArgumentException.class})
    ResponseEntity<?> badRequest(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
    }
}
