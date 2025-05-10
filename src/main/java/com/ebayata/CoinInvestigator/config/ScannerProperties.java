package com.ebayata.CoinInvestigator.config;

import lombok.Data;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Data
@Configuration
public class ScannerProperties {

    private int threads;
    private int batchSize;
    private int delayMs;
    private boolean shutdownSignal;

    private Fulcrum fulcrum = new Fulcrum();

    @Data
    public static class Fulcrum {
        private String host;
        private int port;
    }
}
