package com.harikesh.razorpay.operations.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "settlement_payment")
@Data
public class SettlementPayment {

    @EmbeddedId
    private SettlementPaymentId id;

    @MapsId()
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement;
}
