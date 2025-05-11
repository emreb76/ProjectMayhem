package com.ebayata.CoinInvestigator.service;

import com.ebayata.CoinInvestigator.config.ScannerProperties;
import com.ebayata.CoinInvestigator.entity.KeyData;
import com.ebayata.CoinInvestigator.entity.KeyInfo;
import com.ebayata.CoinInvestigator.repository.KeyInfoRepository;
import com.ebayata.CoinInvestigator.util.BitcoinAddressGenerator;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class FulcrumKeyScannerService {

    private final FulcrumBalanceCheckerService balanceChecker;
    private final KeyInfoRepository keyInfoRepository;
    private final ScannerProperties properties;

    private final BlockingQueue<KeyData> queue = new LinkedBlockingQueue<>(100); // limit memory usage
    private ExecutorService executor;

    @PostConstruct
    public void start() {
        executor = Executors.newFixedThreadPool(properties.getThreads() + 1); // +1 for producer

        // Start producer
        executor.submit(this::generateKey);
//        executor.submit(this::generateTestKeysOnce); // one batch only

        // Start consumers
        for (int i = 0; i < properties.getThreads(); i++) {
            executor.submit(this::scan);
        }
    }

//    private void generateTestKeysOnce() {
//        try {
//            KeyData testBatch=new KeyData("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa", "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq", "WIF-TEST-1");
//            KeyData testBatch2=new KeyData("1BoatSLRHtKNngkdXEeobR76b53LETtpyT", "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh", "WIF-TEST-2");
//            KeyData testBatch3=new KeyData("1dice8EMZmqKvrGE4Qc9bUFf9PX3xaYDp", "bc1q9ctfvqfpy859exgfr9grn53p2g6tjfs6fyh5zm", "WIF-TEST-3");
//            KeyData testBatch4=new KeyData("1BoatSLRHtKNngkdXEeobR76b53LETtpyT", "bc1qp4q68c6a8snejg8kaxtv9ql8j0pphtznlkts95", "WIF-TEST-4");
//
//            queue.put(testBatch);
//            queue.put(testBatch2);
//            queue.put(testBatch3);
//            queue.put(testBatch4);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//    }

    private void generateKey() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                queue.put(BitcoinAddressGenerator.generateRandomKey()); // blocks if full
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void scan() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                KeyData key = queue.take();

//                System.out.println("processing legacy: " + key.getLegacyAddress());
                long legacyBalance = balanceChecker.getBalance(key.getLegacyAddress());
//                System.out.println("processing bech32: " + key.getBech32Address());
                long bech32Balance = balanceChecker.getBalance(key.getBech32Address());
                long total = legacyBalance + bech32Balance;

                if (total > 0) {
                    long totalSats = Optional.ofNullable(legacyBalance).orElse(0L)
                            + Optional.ofNullable(bech32Balance).orElse(0L);

                    String btcBalance = BigDecimal.valueOf(totalSats, 8).stripTrailingZeros().toPlainString();

                    System.out.printf("🔥 FOUND Address: %s | Balance: %s BTC | PrivateKey(WIF): %s%n",
                            key.getLegacyAddress(), btcBalance, key.getPrivateKeyWIF());

                    keyInfoRepository.save(KeyInfo.builder()
                            .legacyAddress(key.getLegacyAddress())
                            .bech32Address(key.getBech32Address())
                            .privateKey(key.getPrivateKeyWIF())
                            .balance(btcBalance)
                            .build());
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

