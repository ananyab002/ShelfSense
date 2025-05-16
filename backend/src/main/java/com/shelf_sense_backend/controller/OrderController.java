package com.shelf_sense_backend.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController; 

import com.shelf_sense_backend.model.Order;
import com.shelf_sense_backend.repo.OrderRepository;

@RestController
@RequestMapping("/api/orders") 
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping
    @Transactional(readOnly = true) 
    public List<Order> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        orders.forEach(order -> order.getItems().size()); 
        return orders;
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.getItems().size();
            return ResponseEntity.ok(order);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/number/{orderNumber}")
    @Transactional(readOnly = true)
    public ResponseEntity<Order> getOrderByOrderNumber(@PathVariable String orderNumber) {
        Optional<Order> orderOpt = orderRepository.findByOrderNumber(orderNumber);
         if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.getItems().size();
            return ResponseEntity.ok(order);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}