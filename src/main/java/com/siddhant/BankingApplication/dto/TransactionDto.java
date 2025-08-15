package com.siddhant.BankingApplication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class TransactionDto {
    private String accountNumber;
    private String transactionType; // "credit" or "debit"
    private BigDecimal amount;
    private String status; // "success" or "failed"

}
