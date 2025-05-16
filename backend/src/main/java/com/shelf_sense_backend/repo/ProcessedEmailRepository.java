package com.shelf_sense_backend.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.shelf_sense_backend.model.ProcessedEmail;

@Repository
public interface ProcessedEmailRepository extends JpaRepository<ProcessedEmail, Long> {
    
    boolean existsByMessageId(String messageId);
}
