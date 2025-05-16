package com.shelf_sense_backend.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.shelf_sense_backend.model.SuggestedItem;

import jakarta.transaction.Transactional;

@Repository
public interface SuggestedItemRepository extends JpaRepository<SuggestedItem, Long> {
    
    @Modifying
    @Transactional
    @Query("DELETE FROM SuggestedItem")
    void deleteAllSuggestedItems();
}