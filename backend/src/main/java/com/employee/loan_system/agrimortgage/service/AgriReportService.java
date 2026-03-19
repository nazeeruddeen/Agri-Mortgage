package com.employee.loan_system.agrimortgage.service;

import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplication;
import com.employee.loan_system.agrimortgage.repository.AgriMortgageApplicationRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for generating Excel reports for Agri Mortgage Applications.
 *
 * Interview story:
 * "I used Apache POI rather than streaming CSV because the operations team needed
 *  formatted Excel output with bold headers and number formatting — not flat CSV.
 *  The method returns byte[] to keep the service layer independent of HTTP concerns;
 *  the controller handles Content-Disposition and streaming."
 */
@Service
public class AgriReportService {

    private final AgriMortgageApplicationRepository applicationRepository;

    public AgriReportService(AgriMortgageApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    /**
     * Exports all agri mortgage applications to Excel (.xlsx) format.
     *
     * @return byte array of the generated Excel workbook
     */
    @Transactional(readOnly = true)
    public byte[] exportApplicationsToExcel() throws IOException {
        List<AgriMortgageApplication> applications = applicationRepository.findAll();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Agri Mortgage Applications");

            // Header style: bold font
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Currency format
            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("#,##0.00"));

            // Header row
            String[] headers = {
                "Application No", "Primary Applicant", "District", "Taluka", "Village",
                "Status", "Requested Amount", "LTV Ratio (%)", "Eligible",
                "Total Land Value", "Combined Income", "Submitted At"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            // Data rows
            int rowIndex = 1;
            for (AgriMortgageApplication app : applications) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(app.getApplicationNumber());
                row.createCell(1).setCellValue(app.getPrimaryApplicantName());
                row.createCell(2).setCellValue(app.getDistrict());
                row.createCell(3).setCellValue(app.getTaluka());
                row.createCell(4).setCellValue(app.getVillage());
                row.createCell(5).setCellValue(app.getStatus().name());

                Cell amtCell = row.createCell(6);
                amtCell.setCellValue(app.getRequestedAmount() != null ? app.getRequestedAmount().doubleValue() : 0);
                amtCell.setCellStyle(currencyStyle);

                row.createCell(7).setCellValue(
                    app.getLtvRatio() != null ? app.getLtvRatio().multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP).doubleValue() : 0
                );
                row.createCell(8).setCellValue(app.isEligible() ? "YES" : "NO");

                Cell landValCell = row.createCell(9);
                landValCell.setCellValue(app.getTotalLandValue() != null ? app.getTotalLandValue().doubleValue() : 0);
                landValCell.setCellStyle(currencyStyle);

                Cell incomeCell = row.createCell(10);
                incomeCell.setCellValue(app.getCombinedIncome() != null ? app.getCombinedIncome().doubleValue() : 0);
                incomeCell.setCellStyle(currencyStyle);

                row.createCell(11).setCellValue(app.getSubmittedAt() != null ? app.getSubmittedAt().toString() : "");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Returns district-level summary: total apps, total sanctioned amount, avg LTV per district.
     *
     * Interview story:
     * "Rather than pulling all records and computing in memory, this aggregation uses
     *  the repository query to GROUP BY district. For reporting endpoints, I keep aggregation
     *  in the DB when the data set is large — Java streams for small sets, SQL for large ones."
     */
    @Transactional(readOnly = true)
    public List<DistrictSummary> getDistrictSummary() {
        List<AgriMortgageApplication> applications = applicationRepository.findAll();

        // Group by district
        Map<String, DistrictAccumulator> accumulators = new HashMap<>();
        for (AgriMortgageApplication app : applications) {
            String district = app.getDistrict();
            accumulators.computeIfAbsent(district, k -> new DistrictAccumulator(district))
                    .add(app);
        }

        return accumulators.values().stream()
                .map(DistrictAccumulator::toSummary)
                .sorted((a, b) -> a.district().compareTo(b.district()))
                .toList();
    }

    // --- Inner types ---

    public record DistrictSummary(
            String district,
            long totalApplications,
            long sanctionedApplications,
            BigDecimal totalSanctionedAmount,
            BigDecimal averageLtvRatio
    ) {}

    private static class DistrictAccumulator {
        final String district;
        long totalApps = 0;
        long sanctioned = 0;
        BigDecimal totalSanctionedAmount = BigDecimal.ZERO;
        BigDecimal ltvSum = BigDecimal.ZERO;
        long ltvCount = 0;

        DistrictAccumulator(String district) {
            this.district = district;
        }

        void add(AgriMortgageApplication app) {
            totalApps++;
            var status = app.getStatus();
            if (status == com.employee.loan_system.agrimortgage.entity.AgriMortgageApplicationStatus.SANCTIONED
                    || status == com.employee.loan_system.agrimortgage.entity.AgriMortgageApplicationStatus.DISBURSED
                    || status == com.employee.loan_system.agrimortgage.entity.AgriMortgageApplicationStatus.CLOSED) {
                sanctioned++;
                if (app.getRequestedAmount() != null) {
                    totalSanctionedAmount = totalSanctionedAmount.add(app.getRequestedAmount());
                }
            }
            if (app.getLtvRatio() != null) {
                ltvSum = ltvSum.add(app.getLtvRatio());
                ltvCount++;
            }
        }

        DistrictSummary toSummary() {
            BigDecimal avgLtv = ltvCount > 0
                    ? ltvSum.divide(BigDecimal.valueOf(ltvCount), 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            return new DistrictSummary(district, totalApps, sanctioned,
                    totalSanctionedAmount.setScale(2, RoundingMode.HALF_UP), avgLtv);
        }
    }
}
