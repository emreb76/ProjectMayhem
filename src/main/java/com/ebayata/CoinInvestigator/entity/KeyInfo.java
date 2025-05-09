package com.ebayata.CoinInvestigator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "keyinfo")
public class KeyInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    private long id;

    @Column(name = "legacyAddress")
    private String legacyAddress;

    @Column(name = "bech32Address")
    private String bech32Address;

    @Column(name = "privateKey")
    private String privateKey;

    @Column(name = "balance")
    private String balance;
}
