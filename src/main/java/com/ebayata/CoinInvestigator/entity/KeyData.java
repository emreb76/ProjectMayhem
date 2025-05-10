package com.ebayata.CoinInvestigator.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class KeyData {

    private String legacyAddress;
    private String bech32Address;
    private final String privateKeyWIF;
}
