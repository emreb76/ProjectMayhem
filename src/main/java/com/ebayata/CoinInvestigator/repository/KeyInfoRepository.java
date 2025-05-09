package com.ebayata.CoinInvestigator.repository;

import com.ebayata.CoinInvestigator.entity.KeyInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KeyInfoRepository extends JpaRepository<KeyInfo, Long> {
}