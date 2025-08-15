package com.siddhant.BankingApplication.service.impl;

import com.siddhant.BankingApplication.dto.TransactionDto;
import com.siddhant.BankingApplication.entity.Transaction;

public interface TransactionService {
    void saveTransaction(TransactionDto transactionDto);
}
