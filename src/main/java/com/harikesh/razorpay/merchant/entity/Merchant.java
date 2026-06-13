package com.harikesh.razorpay.merchant.entity;

import java.util.UUID;
import com.harikesh.razorpay.common.enums.BusinessType;
import com.harikesh.razorpay.common.enums.MerchantStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "merchants")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Merchant {
    
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(unique = true, nullable = false, length = 100)
    private String email;
    
    @Column(length = 20)
    private String contactNumber;
    
    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private BusinessType businessType;
    
    @Column(length = 100)
    private String businessName;
    
    @Column(length = 100)
    private String websiteUrl;

    @Column(length = 200, nullable = false)
    @Enumerated(EnumType.STRING)
    private MerchantStatus status = MerchantStatus.PENDING_KYC;
    
    @Column(length = 50)
    private String gstId;
    
    @Column(length = 50)
    private String panId;

    private String settlementBankAccount;
    private String settlementBankIFSC;
    private String settlementBankAccountHolderName;

}
