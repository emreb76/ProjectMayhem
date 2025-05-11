package com.ebayata.CoinInvestigator.service;

import com.ebayata.CoinInvestigator.util.AddressToScriptPubKeyConverter;
import com.ebayata.CoinInvestigator.util.FulcrumSocketPool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FulcrumBalanceCheckerService {

    private final FulcrumSocketPool socketPool;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public long getBalance(String address) {
        FulcrumSocketPool.SocketResources socketResources = null;

        try {
            Map<String, Object> json = new HashMap<>();
            json.put("jsonrpc", "2.0");
            json.put("id", 1);
            json.put("method", "blockchain.scripthash.get_balance");
            json.put("params", List.of(toScriptHash(address)));

            String request = objectMapper.writeValueAsString(json);

            socketResources = socketPool.borrow();
            socketResources.getWriter().write(request);
            socketResources.getWriter().newLine();
            socketResources.getWriter().flush();

            String response = socketResources.getReader().readLine();
            if (response == null) {
                return 0L;
            }

            JsonNode balancesNode = objectMapper.readTree(response).path("result");

            long confirmed = balancesNode.path("confirmed").asLong();
            long unconfirmed = balancesNode.path("unconfirmed").asLong();
            long total = confirmed + unconfirmed;

            return Math.max(total, 0L);
        } catch (Exception e) {
            return 0L;
        } finally {
            if (socketResources != null) {
                socketPool.release(socketResources);
            }
        }
    }

    private String toScriptHash(String address) throws Exception {
        byte[] scriptPubKey = AddressToScriptPubKeyConverter.convert(address);
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(scriptPubKey);
        reverseBytes(hash);
        return Hex.encodeHexString(hash);
    }

    private void reverseBytes(byte[] array) {
        for (int i = 0; i < array.length / 2; i++) {
            byte temp = array[i];
            array[i] = array[array.length - i - 1];
            array[array.length - i - 1] = temp;
        }
    }
}