package com.cwa.projetbdd.repositories;

import com.cwa.projetbdd.models.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    List<Transaction> findByUtilisateurUidOrderByDateTransactionDesc(Integer uid);
}
