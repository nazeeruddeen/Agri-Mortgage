package com.employee.loan_system.agrimortgage.repository;

import com.employee.loan_system.agrimortgage.entity.AgriMortgageDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgriMortgageDocumentRepository extends JpaRepository<AgriMortgageDocument, Long> {
    List<AgriMortgageDocument> findByApplicationIdOrderByUploadedAtAsc(Long applicationId);
}
