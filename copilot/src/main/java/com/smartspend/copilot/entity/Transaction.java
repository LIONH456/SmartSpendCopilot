package com.smartspend.copilot.entity;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name="transactions")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Double amount;
    String category;
    String merchant;
    String currency;
    String originalCurrency;

    @Column(length = 500)
    String originalDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id")
    User user;
}
