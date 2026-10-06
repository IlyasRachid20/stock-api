package com.ilyas.stockapi.controller;

import com.ilyas.stockapi.dto.report.CategorySales;
import com.ilyas.stockapi.dto.report.DailySales;
import com.ilyas.stockapi.dto.report.SalesSummary;
import com.ilyas.stockapi.dto.report.TopProduct;
import com.ilyas.stockapi.service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

// ADMIN only (see SecurityConfig). Dates are yyyy-MM-dd, both included; by default the last 30 days.
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // GET /api/reports/summary?from=2026-09-01&to=2026-09-30
    @GetMapping("/summary")
    public SalesSummary summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.summary(from, to);
    }

    // One entry per day, days without sales included: ready for a chart
    @GetMapping("/sales-by-day")
    public List<DailySales> salesByDay(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.salesByDay(from, to);
    }

    // Best sellers by quantity, then by revenue
    @GetMapping("/top-products")
    public List<TopProduct> topProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "5") int limit) {
        return reportService.topProducts(from, to, limit);
    }

    // Revenue per category, highest first ("Uncategorized" for products without one)
    @GetMapping("/sales-by-category")
    public List<CategorySales> salesByCategory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.salesByCategory(from, to);
    }

    // Downloads sales-2026-09-01_2026-09-30.csv
    @GetMapping(value = "/sales.csv", produces = "text/csv")
    public void salesCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletResponse response) throws IOException {
        ReportService.DateRange range = reportService.range(from, to);
        response.setContentType("text/csv");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"sales-" + range.from() + "_" + range.to() + ".csv\"");
        reportService.writeSalesCsv(range.from(), range.to(), response.getWriter());
    }
}
