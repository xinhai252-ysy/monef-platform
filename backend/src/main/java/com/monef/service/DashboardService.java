package com.monef.service;

import com.monef.dto.CategoryPoint;
import com.monef.dto.Dashboard;
import com.monef.dto.Metric;
import com.monef.dto.MonthPoint;
import com.monef.exception.ApiException;
import com.monef.repository.LedgerRepository;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final LedgerRepository entries;
    private final Clock clock;

    public DashboardService(LedgerRepository entries, Clock clock) {
        this.entries = entries;
        this.clock = clock;
    }

    public YearMonth month(String value) {
        try {
            var m = YearMonth.parse(value);
            if (
                m.isBefore(YearMonth.of(2000, 1)) || m.isAfter(YearMonth.now(clock))
            ) throw new IllegalArgumentException();
            return m;
        } catch (Exception e) {
            throw new ApiException(400, "请选择 2000 年至本月之间的月份");
        }
    }

    @Transactional(readOnly = true)
    public Dashboard dashboard(String tenant, String selected) {
        var m = month(selected);
        var current = totals(tenant, m);
        var previous = totals(tenant, m.minusMonths(1));
        BigDecimal income = current.income,
            expense = current.expense,
            net = income.subtract(expense),
            prevNet = previous.income.subtract(previous.expense);
        var series = new LinkedHashMap<String, BigDecimal[]>();
        for (int i = 11; i >= 0; i--) series.put(m.minusMonths(i).toString(), new BigDecimal[] {
            BigDecimal.ZERO,
            BigDecimal.ZERO,
        });
        for (var row : entries.monthly(tenant, m.minusMonths(11).atDay(1), m.atEndOfMonth())) {
            String key = YearMonth.of(
                ((Number) row[0]).intValue(),
                ((Number) row[1]).intValue()
            ).toString();
            series.get(key)["INCOME".equals(row[2]) ? 0 : 1] = (BigDecimal) row[3];
        }
        List<MonthPoint> points = series
            .entrySet()
            .stream()
            .map(e -> new MonthPoint(e.getKey(), e.getValue()[0], e.getValue()[1]))
            .toList();
        var cats = entries
            .categories(tenant, m.atDay(1), m.atEndOfMonth())
            .stream()
            .map(r ->
                new CategoryPoint((String) r[0], (BigDecimal) r[1], ((Number) r[2]).longValue())
            )
            .toList();
        return new Dashboard(
            m.toString(),
            "USD",
            metric(income, previous.income),
            metric(expense, previous.expense),
            metric(net, prevNet),
            income.signum() == 0
                ? null
                : net.multiply(BigDecimal.valueOf(100)).divide(income, 2, RoundingMode.HALF_UP),
            current.count,
            points,
            cats,
            clock.instant()
        );
    }

    private record Totals(BigDecimal income, BigDecimal expense, long count) {}

    private Totals totals(String tenant, YearMonth m) {
        BigDecimal in = BigDecimal.ZERO,
            out = BigDecimal.ZERO;
        long count = 0;
        for (var r : entries.totals(tenant, m.atDay(1), m.atEndOfMonth())) {
            if ("INCOME".equals(r[0])) in = (BigDecimal) r[1];
            else out = (BigDecimal) r[1];
            count += ((Number) r[2]).longValue();
        }
        return new Totals(in, out, count);
    }

    private Metric metric(BigDecimal now, BigDecimal prior) {
        return new Metric(
            now,
            prior.signum() == 0
                ? null
                : now
                      .subtract(prior)
                      .multiply(BigDecimal.valueOf(100))
                      .divide(prior.abs(), 2, RoundingMode.HALF_UP)
        );
    }
}
