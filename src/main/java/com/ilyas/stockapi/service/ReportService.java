package com.ilyas.stockapi.service;

import com.ilyas.stockapi.dto.report.DailySales;
import com.ilyas.stockapi.dto.report.SalesSummary;
import com.ilyas.stockapi.dto.report.TopProduct;
import com.ilyas.stockapi.exception.BadRequestException;
import com.ilyas.stockapi.repository.SaleItemRepository;
import com.ilyas.stockapi.repository.SaleLine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sales reports for a date range. Dates are days in the shop's time zone (app.time-zone),
 * so a sale at 23:30 UTC counts on the next day in Morocco (UTC+1).
 * Each report loads the sold lines with one query and adds them up in Java.
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private static final int MAX_DAYS = 366;
    private static final DateTimeFormatter CSV_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SaleItemRepository saleItemRepository;
    private final ZoneId zone;

    public ReportService(SaleItemRepository saleItemRepository, @Value("${app.time-zone:UTC}") String zone) {
        this.saleItemRepository = saleItemRepository;
        this.zone = ZoneId.of(zone);
    }

    public SalesSummary summary(LocalDate from, LocalDate to) {
        DateRange range = range(from, to);
        List<SaleLine> lines = lines(range);
        long salesCount = lines.stream().map(SaleLine::saleId).distinct().count();
        BigDecimal revenue = total(lines);
        BigDecimal average = salesCount == 0 ? money(BigDecimal.ZERO)
                : revenue.divide(BigDecimal.valueOf(salesCount), 2, RoundingMode.HALF_UP);
        return new SalesSummary(range.from(), range.to(), salesCount, itemsSold(lines), revenue, average);
    }

    public List<DailySales> salesByDay(LocalDate from, LocalDate to) {
        DateRange range = range(from, to);
        Map<LocalDate, List<SaleLine>> byDay = lines(range).stream()
                .collect(Collectors.groupingBy(line -> line.saleDate().atZone(zone).toLocalDate()));
        return range.from().datesUntil(range.to().plusDays(1))
                .map(day -> {
                    List<SaleLine> dayLines = byDay.getOrDefault(day, List.of());
                    long sales = dayLines.stream().map(SaleLine::saleId).distinct().count();
                    return new DailySales(day, sales, itemsSold(dayLines), total(dayLines));
                })
                .toList();
    }

    public List<TopProduct> topProducts(LocalDate from, LocalDate to, int limit) {
        if (limit < 1 || limit > 50) {
            throw new BadRequestException("limit must be between 1 and 50");
        }
        Map<Long, List<SaleLine>> byProduct = lines(range(from, to)).stream()
                .collect(Collectors.groupingBy(SaleLine::productId, LinkedHashMap::new, Collectors.toList()));
        return byProduct.values().stream()
                .map(productLines -> new TopProduct(productLines.get(0).productId(), productLines.get(0).productName(),
                        itemsSold(productLines), total(productLines)))
                .sorted(Comparator.comparingLong(TopProduct::quantitySold).reversed()
                        .thenComparing(TopProduct::revenue, Comparator.reverseOrder()))
                .limit(limit)
                .toList();
    }

    // One row per sold line; opens fine in Excel and Google Sheets
    public void writeSalesCsv(LocalDate from, LocalDate to, Writer out) throws IOException {
        out.write("date,sale_id,customer,product,quantity,unit_price,line_total\n");
        for (SaleLine line : lines(range(from, to))) {
            out.write(String.join(",",
                    CSV_DATE_TIME.format(line.saleDate().atZone(zone)),
                    line.saleId().toString(),
                    csv(line.customerName()),
                    csv(line.productName()),
                    line.quantity().toString(),
                    line.unitPrice().toPlainString(),
                    line.lineTotal().toPlainString()));
            out.write("\n");
        }
    }

    // Missing dates mean "the last 30 days, today included"
    public DateRange range(LocalDate from, LocalDate to) {
        LocalDate end = (to != null) ? to : LocalDate.now(zone);
        LocalDate start = (from != null) ? from : end.minusDays(29);
        if (start.isAfter(end)) {
            throw new BadRequestException("from must be on or before to");
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_DAYS) {
            throw new BadRequestException("The range can't be longer than " + MAX_DAYS + " days");
        }
        return new DateRange(start, end);
    }

    private List<SaleLine> lines(DateRange range) {
        return saleItemRepository.findLines(
                range.from().atStartOfDay(zone).toInstant(),
                range.to().plusDays(1).atStartOfDay(zone).toInstant());
    }

    private static long itemsSold(List<SaleLine> lines) {
        return lines.stream().mapToLong(SaleLine::quantity).sum();
    }

    private static BigDecimal total(List<SaleLine> lines) {
        return money(lines.stream().map(SaleLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    // Quotes values that contain a comma, quote or line break, and neutralizes values that a
    // spreadsheet would run as a formula (=, +, -, @), e.g. a customer named "=HYPERLINK(...)"
    static String csv(String value) {
        if (value == null) {
            return "";
        }
        String safe = value.matches("^[=+\\-@].*") ? "'" + value : value;
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            safe = "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    public record DateRange(LocalDate from, LocalDate to) {
    }
}
