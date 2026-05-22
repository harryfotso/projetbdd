package com.cwa.projetbdd.repositories;

import com.cwa.projetbdd.models.Classement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassementRepository extends JpaRepository<Classement, Integer> {
}
