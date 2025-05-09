package com.ebayata.CoinInvestigator.service;

import com.ebayata.CoinInvestigator.util.AddressToScriptPubKeyConverter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FulcrumBalanceCheckerService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String host = "127.0.0.1";
    private final int port = 50001;

    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;

    @PostConstruct
    public void init() throws IOException {
        connect();
    }

    private void connect() throws IOException {
        socket = new Socket(host, port);
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }

    public synchronized boolean hasBalance(List<String> addresses) {
        try {
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
                // Socket died? Reconnect and retry once
                connect();
                return hasBalance(addresses);
            }

            JsonNode jsonResponse = objectMapper.readTree(response);
            JsonNode balancesNode = jsonResponse.path("result");

            if (balancesNode.isObject()) {
                for (Iterator<Map.Entry<String, JsonNode>> it = balancesNode.fields(); it.hasNext(); ) {
                    Map.Entry<String, JsonNode> entry = it.next();
                    JsonNode balance = entry.getValue();
                    long confirmed = balance.path("confirmed").asLong();
                    long unconfirmed = balance.path("unconfirmed").asLong();
                    if (confirmed > 0 || unconfirmed > 0) {
                        return true;
                    }
                }
            }

            return false;
        } catch (Exception e) {
            try {
                connect(); // try to reconnect
            } catch (IOException ignored) {}
            return false;
        }
    }

    private String toScriptHash(String address) throws Exception {
        byte[] scriptPubKey = AddressToScriptPubKeyConverter.convert(address);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(scriptPubKey);
        reverseBytes(hash);
        return bytesToHex(hash);
    }

    private void reverseBytes(byte[] array) {
        for (int i = 0; i < array.length / 2; i++) {
            byte temp = array[i];
            array[i] = array[array.length - i - 1];
            array[array.length - i - 1] = temp;
        }
    }

    private String bytesToHex(byte[] bytes) {
        return String.format("%064x", new java.math.BigInteger(1, bytes));
    }
}
