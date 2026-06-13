package com.harikesh.razorpay.merchant.entity;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Customer {
    
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID)
    private UUID id;

    @ManyToOne
    private Merchant merchant;

    @Column(length = 200)
    private String name;
    
    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String contactNumber;

    @Column(length = 50)
    private LocalDate deletedAt;
    
}
