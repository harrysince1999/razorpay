package com.harikesh.razorpay.merchant.entity;

import java.util.UUID;

import com.harikesh.razorpay.common.enums.Environment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "api_key")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    private Merchant merchant;

    @Column(nullable = false, length = 100)
    private String keyId;

    @Column(nullable = false, length = 100)
    private String keySecretHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private Environment environment;

    private boolean enabled = true;

}
