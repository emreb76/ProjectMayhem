package com.ebayata.CoinInvestigator.service;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BalanceCheckerService {

    private final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private final String rpcUser = "bitcoinrpcxxx";
    private final String rpcPassword = "yourStrongPassword56752735";
    private final String endpoint = "http://127.0.0.1:8332";

    public boolean hasBalance(List<String> addresses) {
        try {
            String auth = Base64.getEncoder().encodeToString((rpcUser + ":" + rpcPassword).getBytes());

            String addressDescriptors = addresses.stream()
                    .map(addr -> "{\"desc\": \"addr(" + addr + ")\"}")
                    .collect(Collectors.joining(", "));

            String requestBody = """
                    {
                      "jsonrpc": "1.0",
                      "id": "scan",
                      "method": "scantxoutset",
                      "params": [
                        "start",
                        [
                          %s
                        ]
                      ]
                    }
                    """.formatted(addressDescriptors);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            if (responseBody.contains("Scan already in progress")) {
                Thread.sleep(2000);
                return hasBalance(addresses); // retry after 2 seconds
            }

            return responseBody.contains("\"total_amount\":") && !responseBody.contains("\"total_amount\":0");

        } catch (Exception e) {
            e.printStackTrace();
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            return false;
        }
    }
}
