package com.ebayata.CoinInvestigator.service;

import com.ebayata.CoinInvestigator.config.ScannerProperties;
import com.ebayata.CoinInvestigator.util.AddressToScriptPubKeyConverter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FulcrumBalanceCheckerService {

    private final ScannerProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;

    @PostConstruct
    public void connect() throws IOException {
        socket = new Socket(properties.getFulcrum().getHost(), properties.getFulcrum().getPort());
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
    }

    public Map<String, Long> getBalances(List<String> addresses) {
        try {
            if (properties.isShutdownSignal()) {
                this.shutdown();
            }
            List<String> scriptHashes = new ArrayList<>();
            for (String address : addresses) {
                scriptHashes.add(toScriptHash(address));
            }

            String request = """
                    {
                      "id": 1,
                      "method": "blockchain.scripthash.get_balances",
                      "params": [%s]
                    }
                    """.formatted(objectMapper.writeValueAsString(scriptHashes));

            writer.write(request);
            writer.newLine();
            writer.flush();

            String response = reader.readLine();

            if (response == null) {
                connect();
                return getBalances(addresses);
            }

            JsonNode balancesNode = objectMapper.readTree(response).path("result");

            Map<String, Long> balanceMap = new HashMap<>();
            if (balancesNode.isArray()) {
                int i = 0;
                for (JsonNode balanceNode : balancesNode) {
                    long confirmed = balanceNode.path("confirmed").asLong();
                    long unconfirmed = balanceNode.path("unconfirmed").asLong();
                    long total = confirmed + unconfirmed;

                    if (total > 0) {
                        String address = addresses.get(i);
                        balanceMap.put(address, total);
                    }
                    i++;
                }
            }

            return balanceMap;

        } catch (Exception e) {
//            try {
//                connect();
//            } catch (IOException ignored) {
//            }
            return Collections.emptyMap();
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
