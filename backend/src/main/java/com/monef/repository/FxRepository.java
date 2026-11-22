package com.monef.repository;

import com.monef.entity.FxRate;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface FxRepository extends JpaRepository<FxRate, String> {
    Optional<FxRate> findByBaseCurrencyAndQuoteCurrencyAndRateDateAndSource(
        String base,
        String quote,
        LocalDate date,
        String source
    );
    Page<FxRate> findByBaseCurrencyAndQuoteCurrencyOrderByRateDateDesc(
        String base,
        String quote,
        Pageable page
    );
}
