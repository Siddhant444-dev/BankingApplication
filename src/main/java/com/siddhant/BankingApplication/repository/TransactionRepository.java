package com.siddhant.BankingApplication.repository;

import com.siddhant.BankingApplication.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


public interface TransactionRepository extends JpaRepository<Transaction, String> {

}
