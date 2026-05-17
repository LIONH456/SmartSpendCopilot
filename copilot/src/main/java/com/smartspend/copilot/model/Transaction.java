package com.smartspend.copilot.model;

import jakarta.persistence.*;

import lombok.Data;

@Entity
@Data
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double amount;
    private String category;
    private String merchant;
    private String currency;
    private String originalCurrency;

    @Column(length = 500)
    private String originalDescription;
}
