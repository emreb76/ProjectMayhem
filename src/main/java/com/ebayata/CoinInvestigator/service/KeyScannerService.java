package com.ebayata.CoinInvestigator.service;

import com.ebayata.CoinInvestigator.entity.KeyData;
import com.ebayata.CoinInvestigator.entity.KeyInfo;
import com.ebayata.CoinInvestigator.repository.KeyInfoRepository;
import com.ebayata.CoinInvestigator.util.BitcoinAddressGenerator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class KeyScannerService {

    private final BalanceCheckerService balanceChecker;
    private final KeyInfoRepository keyInfoRepository;

    public void startScanning() {
        int THREADS = 1;
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        for (int i = 0; i < THREADS; i++) {
            executor.submit(this::scanLoop);
        }
    }

    private void scanLoop() {
        while (true) {
            List<KeyData> keyBatch = new ArrayList<>();

            // Generate 5 legacy and 5 bech32 addresses (total 10 addresses)
            for (int i = 0; i < 5; i++) {
                KeyData keyData = BitcoinAddressGenerator.generateRandomKey();
                keyBatch.add(keyData);
            }

            List<String> addressesToScan = new ArrayList<>();
            for (KeyData keyData : keyBatch) {
                addressesToScan.add(keyData.getLegacyAddress());
                addressesToScan.add(keyData.getBech32Address());
            }

            if (balanceChecker.hasBalance(addressesToScan)) {
                for (KeyData keyData : keyBatch) {
                    // Double check individually if you want here (optional)
                    System.out.printf("🔥 FOUND Address: %s or %s | PrivateKey(WIF): %s%n",
                            keyData.getLegacyAddress(),
                            keyData.getBech32Address(),
                            keyData.getPrivateKeyWIF());

                    KeyInfo keyInfo = KeyInfo.builder()
                            .legacyAddress(keyData.getLegacyAddress())
                            .bech32Address(keyData.getBech32Address())
                            .privateKey(keyData.getPrivateKeyWIF())
                            .build();
                    keyInfoRepository.save(keyInfo);
                }
            }
        }
    }
}
