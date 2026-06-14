package com.harikesh.razorpay.operations.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SettlementPaymentId {

    private UUID settlementId;

    private UUID paymentId;
}
