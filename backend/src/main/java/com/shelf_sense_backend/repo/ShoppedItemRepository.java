package com.shelf_sense_backend.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.shelf_sense_backend.model.ShoppedItem;

@Repository
public interface ShoppedItemRepository extends JpaRepository<ShoppedItem, Long> {
    
    List<ShoppedItem> findByGeneralNameIsNull();
    
    List<ShoppedItem> findByFoodType(String foodType);
    
    List<ShoppedItem> findByGeneralName(String generalName);
    
    List<ShoppedItem> findByRawNameContainingIgnoreCase(String keyword);

    @Query("SELECT si FROM ShoppedItem si JOIN FETCH si.order")
List<ShoppedItem> findAllWithOrderDate();
}