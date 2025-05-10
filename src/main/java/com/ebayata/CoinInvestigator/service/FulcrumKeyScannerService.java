package com.ebayata.CoinInvestigator.service;

import com.ebayata.CoinInvestigator.config.ScannerProperties;
import com.ebayata.CoinInvestigator.entity.KeyData;
import com.ebayata.CoinInvestigator.entity.KeyInfo;
import com.ebayata.CoinInvestigator.repository.KeyInfoRepository;
import com.ebayata.CoinInvestigator.util.BitcoinAddressGenerator;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@Service
@RequiredArgsConstructor
public class FulcrumKeyScannerService {

    private final FulcrumBalanceCheckerService balanceChecker;
    private final KeyInfoRepository keyInfoRepository;
    private final ScannerProperties properties;

    private final BlockingQueue<List<KeyData>> queue = new LinkedBlockingQueue<>(100); // limit memory usage
    private ExecutorService executor;

    @PostConstruct
    public void start() {
        executor = Executors.newFixedThreadPool(properties.getThreads() + 1); // +1 for producer

        // Start producer
        executor.submit(this::generateKeysLoop);
//        executor.submit(this::generateTestKeysOnce); // one batch only

        // Start consumers
        for (int i = 0; i < properties.getThreads(); i++) {
            executor.submit(this::scan);
        }
    }

    private void generateTestKeysOnce() {
        try {
            List<KeyData> testBatch = List.of(
                    new KeyData("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa", "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq", "WIF-TEST-1"),
                    new KeyData("1BoatSLRHtKNngkdXEeobR76b53LETtpyT", "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh", "WIF-TEST-2"),
                    new KeyData("1dice8EMZmqKvrGE4Qc9bUFf9PX3xaYDp", "bc1q9ctfvqfpy859exgfr9grn53p2g6tjfs6fyh5zm", "WIF-TEST-3"),
                    new KeyData("1BoatSLRHtKNngkdXEeobR76b53LETtpyT", "bc1qp4q68c6a8snejg8kaxtv9ql8j0pphtznlkts95", "WIF-TEST-4")
            );

            queue.put(testBatch);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void generateKeysLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<KeyData> batch = new ArrayList<>();
                for (int i = 0; i < properties.getBatchSize(); i++) {
                    batch.add(BitcoinAddressGenerator.generateRandomKey());
                }
                queue.put(batch); // blocks if full
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void scan() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<KeyData> batch = queue.take(); // blocks if empty

                List<String> addresses = new ArrayList<>();
                for (KeyData key : batch) {
                    addresses.add(key.getLegacyAddress());
                    addresses.add(key.getBech32Address());
                }

                Map<String, Long> balances = balanceChecker.getBalances(addresses);

                if (!balances.isEmpty()) {
                    for (KeyData key : batch) {
                        Long legacyBalance = balances.get(key.getLegacyAddress());
                        Long bech32Balance = balances.get(key.getBech32Address());

                        if (legacyBalance != null || bech32Balance != null) {
                            long totalSats = Optional.ofNullable(legacyBalance).orElse(0L)
                                    + Optional.ofNullable(bech32Balance).orElse(0L);

                            String btcBalance = BigDecimal.valueOf(totalSats, 8).stripTrailingZeros().toPlainString();

                            System.out.printf("🔥 FOUND Address: %s | Balance: %s BTC | PrivateKey(WIF): %s%n",
                                    key.getLegacyAddress(), btcBalance, key.getPrivateKeyWIF());

                            KeyInfo keyInfo = KeyInfo.builder()
                                    .legacyAddress(key.getLegacyAddress())
                                    .bech32Address(key.getBech32Address())
                                    .privateKey(key.getPrivateKeyWIF())
                                    .balance(btcBalance)
                                    .build();

                            keyInfoRepository.save(keyInfo);
                        }
                    }
                }

                Thread.sleep(properties.getDelayMs());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                // log error if needed
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}

