package com.ebayata.CoinInvestigator.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KeyData {

    private String legacyAddress;
    private String bech32Address;
    private final String privateKeyWIF;
}
