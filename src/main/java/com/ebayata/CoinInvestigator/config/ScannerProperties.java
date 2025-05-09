package com.ebayata.CoinInvestigator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scanner")
@Data
public class ScannerProperties {

    private int threads;
    private int batchSize;
    private int delayMs;
    private Fulcrum fulcrum = new Fulcrum();

    @Data
    public static class Fulcrum {
        private String host;
        private int port;
    }
}
