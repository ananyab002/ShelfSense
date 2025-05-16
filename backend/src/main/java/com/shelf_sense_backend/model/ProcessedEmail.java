package com.shelf_sense_backend.model;


import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "processed_emails", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"messageId"}) 
})
public class ProcessedEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500) 
    private String messageId; // Unique ID provided by the mail server

    @Column(nullable = false)
    private Instant emailSentTimestamp; 

    @Column(nullable = false)
    private Instant processedTimestamp; 

    @OneToOne(mappedBy = "processedEmail") 
    private Order generatedOrder;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessedEmail that = (ProcessedEmail) o;
        return java.util.Objects.equals(messageId, that.messageId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(messageId);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Instant getEmailSentTimestamp() {
        return emailSentTimestamp;
    }

    public void setEmailSentTimestamp(Instant emailSentTimestamp) {
        this.emailSentTimestamp = emailSentTimestamp;
    }

    public Instant getProcessedTimestamp() {
        return processedTimestamp;
    }

    public void setProcessedTimestamp(Instant processedTimestamp) {
        this.processedTimestamp = processedTimestamp;
    }

    public Order getGeneratedOrder() {
        return generatedOrder;
    }

    public void setGeneratedOrder(Order generatedOrder) {
        this.generatedOrder = generatedOrder;
    }


}
