package com.banking.exception;

import com.banking.dto.BankingDtos;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

// ===== Exceptions Métier =====

class CompteNotFoundException extends RuntimeException {
    public CompteNotFoundException(String message) { super(message); }
}

class SoldeInsuffisantException extends RuntimeException {
    public SoldeInsuffisantException(String message) { super(message); }
}

class CompteDejaExistantException extends RuntimeException {
    public CompteDejaExistantException(String message) { super(message); }
}

class TransactionDuplicatException extends RuntimeException {
    public TransactionDuplicatException(String message) { super(message); }
}

class AccesNonAutoriseException extends RuntimeException {
    public AccesNonAutoriseException(String message) { super(message); }
}

// ===== Gestionnaire Global des Exceptions =====

/**
 * Intercepte toutes les exceptions et retourne des réponses HTTP standardisées.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CompteNotFoundException.class)
    public ResponseEntity<BankingDtos.ErreurResponse> handleCompteNotFound(CompteNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "COMPTE_INTROUVABLE", ex.getMessage());
    }

    @ExceptionHandler(SoldeInsuffisantException.class)
    public ResponseEntity<BankingDtos.ErreurResponse> handleSoldeInsuffisant(SoldeInsuffisantException ex) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "SOLDE_INSUFFISANT", ex.getMessage());
    }

    @ExceptionHandler(CompteDejaExistantException.class)
    public ResponseEntity<BankingDtos.ErreurResponse> handleCompteDejaExistant(CompteDejaExistantException ex) {
        return buildResponse(HttpStatus.CONFLICT, "COMPTE_DEJA_EXISTANT", ex.getMessage());
    }

    @ExceptionHandler(TransactionDuplicatException.class)
    public ResponseEntity<BankingDtos.ErreurResponse> handleTransactionDuplicat(TransactionDuplicatException ex) {
        return buildResponse(HttpStatus.CONFLICT, "TRANSACTION_DUPLIQUEE", ex.getMessage());
    }

    @ExceptionHandler(AccesNonAutoriseException.class)
    public ResponseEntity<BankingDtos.ErreurResponse> handleAccesNonAutorise(AccesNonAutoriseException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "ACCES_NON_AUTORISE", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BankingDtos.ErreurResponse> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return buildResponse(HttpStatus.BAD_REQUEST, "DONNEES_INVALIDES", details);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BankingDtos.ErreurResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "ARGUMENT_INVALIDE", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BankingDtos.ErreurResponse> handleGenericException(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "ERREUR_INTERNE",
                "Une erreur inattendue s'est produite. Veuillez réessayer.");
    }

    private ResponseEntity<BankingDtos.ErreurResponse> buildResponse(
            HttpStatus status, String erreur, String message) {
        return ResponseEntity.status(status).body(
                BankingDtos.ErreurResponse.builder()
                        .statut(status.value())
                        .erreur(erreur)
                        .message(message)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
