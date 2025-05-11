package com.ebayata.CoinInvestigator.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Data
@Configuration
public class ScannerProperties {

    @Value("${scanner.threads}")
    private int threads;
    @Value("${scanner.delay-ms}")
    private int delayMs;

    private Fulcrum fulcrum = new Fulcrum();

    @Data
    public static class Fulcrum {
        private String host;
        private int port;
    }
}
