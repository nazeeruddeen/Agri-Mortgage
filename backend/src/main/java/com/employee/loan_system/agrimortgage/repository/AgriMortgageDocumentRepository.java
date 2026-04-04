package com.employee.loan_system.agrimortgage.repository;

import com.employee.loan_system.agrimortgage.entity.AgriMortgageDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AgriMortgageDocumentRepository extends JpaRepository<AgriMortgageDocument, Long> {
    List<AgriMortgageDocument> findByApplicationIdOrderByUploadedAtAsc(Long applicationId);

    @Query(value = """
            select count(*) from (
                select d.application_id
                from agri_mortgage_documents d
                where d.document_type in ('PATTADAR_PASSBOOK', 'OWNERSHIP_PROOF', 'ENCUMBRANCE_CERTIFICATE', 'LAND_VALUATION_REPORT')
                  and d.document_status = 'VERIFIED'
                group by d.application_id
                having count(distinct d.document_type) = 4
            ) ready_documents
            """, nativeQuery = true)
    long countApplicationsWithVerifiedRequiredDocuments();
}
