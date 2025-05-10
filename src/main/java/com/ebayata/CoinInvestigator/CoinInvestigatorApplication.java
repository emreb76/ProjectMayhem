package com.ebayata.CoinInvestigator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties
@SpringBootApplication
public class CoinInvestigatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoinInvestigatorApplication.class, args);
	}
}
