package com.employee.loan_system.agrimortgage.service;

import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplication;
import com.employee.loan_system.agrimortgage.repository.AgriMortgageApplicationRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Service for generating Excel reports for Agri Mortgage Applications.
 *
 * Interview story:
 * "I used Apache POI rather than streaming CSV because the operations team needed
 * formatted Excel output with bold headers and number formatting, not flat CSV.
 * The method returns byte[] to keep the service layer independent of HTTP concerns;
 * the controller handles Content-Disposition and streaming.
 *
 * For reporting, district summary aggregation now stays in SQL so the report path
 * does not pull every application into memory just to group by district."
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
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Agri Mortgage Applications");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("#,##0.00"));

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
     * a GROUP BY query on the applications table. That keeps the report path linear in the
     * number of districts instead of the total number of mortgage cases."
     */
    @Transactional(readOnly = true)
    public List<DistrictSummary> getDistrictSummary() {
        return applicationRepository.findDistrictSummaryRows().stream()
                .map(this::toSummary)
                .toList();
    }

    public record DistrictSummary(
            String district,
            long totalApplications,
            long sanctionedApplications,
            BigDecimal totalSanctionedAmount,
            BigDecimal averageLtvRatio
    ) {
    }

    private DistrictSummary toSummary(Object[] row) {
        String district = row[0] == null ? "UNKNOWN" : row[0].toString();
        long totalApplications = toLong(row[1]);
        long sanctionedApplications = toLong(row[2]);
        BigDecimal totalSanctionedAmount = toBigDecimal(row[3]).setScale(2, RoundingMode.HALF_UP);
        BigDecimal averageLtvRatio = toBigDecimal(row[4]).setScale(4, RoundingMode.HALF_UP);
        return new DistrictSummary(district, totalApplications, sanctionedApplications, totalSanctionedAmount, averageLtvRatio);
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }
}
