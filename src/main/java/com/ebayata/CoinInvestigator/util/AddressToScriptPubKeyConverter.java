package com.ebayata.CoinInvestigator.util;

import org.bitcoinj.base.Address;
import org.bitcoinj.base.LegacyAddress;
import org.bitcoinj.base.SegwitAddress;
import org.bitcoinj.params.MainNetParams;

public class AddressToScriptPubKeyConverter {

    private static final MainNetParams NETWORK = MainNetParams.get();

    public static byte[] convert(String addressStr) throws Exception {
        Address address = Address.fromString(NETWORK, addressStr);

        if (address instanceof LegacyAddress) {
            return getP2PKHScript(address.getHash());
        } else if (address instanceof SegwitAddress) {
            return getP2WPKHScript((SegwitAddress) address);
        } else {
            throw new IllegalArgumentException("Unsupported address type: " + addressStr);
        }
    }

    private static byte[] getP2PKHScript(byte[] pubKeyHash) {
        return new byte[]{
                (byte) 0x76,               // OP_DUP
                (byte) 0xA9,               // OP_HASH160
                0x14                      // Push 20 bytes
        };
    }

    private static byte[] getP2WPKHScript(SegwitAddress segwitAddress) {
        byte[] program = segwitAddress.getHash();
        byte[] script = new byte[2 + program.length];
        script[0] = 0x00; // version 0
        script[1] = (byte) program.length;
        System.arraycopy(program, 0, script, 2, program.length);
        return script;
    }
}
