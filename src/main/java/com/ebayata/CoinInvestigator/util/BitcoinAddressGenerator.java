package com.ebayata.CoinInvestigator.util;

import com.ebayata.CoinInvestigator.entity.KeyData;
import org.bitcoinj.base.BitcoinNetwork;
import org.bitcoinj.base.ScriptType;
import org.bitcoinj.crypto.ECKey;

import java.security.SecureRandom;

public class BitcoinAddressGenerator {

    public static KeyData generateRandomKey() {
        ECKey key = new ECKey(new SecureRandom());
        // Generate Legacy (P2PKH) address
        String legacyAddress = key.toAddress(ScriptType.P2PKH, BitcoinNetwork.MAINNET).toString();

        // Generate Bech32 (P2WPKH) address
        String bech32Address = key.toAddress(ScriptType.P2WPKH, BitcoinNetwork.MAINNET).toString();

        // Private key in WIF format
        String privateKeyWIF = key.getPrivateKeyAsWiF(BitcoinNetwork.MAINNET);

        // You can create your own object that holds both addresses
        return new KeyData(legacyAddress, bech32Address, privateKeyWIF);
    }
}
