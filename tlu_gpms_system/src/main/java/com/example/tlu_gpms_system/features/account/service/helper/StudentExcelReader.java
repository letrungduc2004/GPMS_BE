package com.example.tlu_gpms_system.features.account.service.helper;

import com.example.tlu_gpms_system.common.utils.excel.ExcelUtils;
import com.example.tlu_gpms_system.configuration.exception.AppException;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Kiểm tra mức toàn file và đọc các dòng dữ liệu (đã chuẩn hóa) từ file mẫu import sinh viên
public final class StudentExcelReader {

    private static final int HEADER_ROW = 0;
    private static final int COLUMN_COUNT = StudentColumn.values().length;

    private StudentExcelReader() {
    }

    public static List<StudentRowData> read(MultipartFile file) {
        try (Workbook workbook = ExcelUtils.openXlsx(file)) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null) {
                throw new AppException(ErrorCode.FILE_NO_DATA);
            }
            DataFormatter formatter = new DataFormatter();
            validateHeader(sheet.getRow(HEADER_ROW), formatter);
            if (ExcelUtils.hasMergedCells(sheet, COLUMN_COUNT)) {
                throw new AppException(ErrorCode.FILE_MERGED_CELLS);
            }

            List<StudentRowData> rows = new ArrayList<>();
            for (int i = HEADER_ROW + 1; i <= sheet.getLastRowNum(); i++) {
                List<String> cells = ExcelUtils.readRow(sheet.getRow(i), COLUMN_COUNT, formatter);
                // Bỏ qua dòng trống hoàn toàn (ghost rows)
                if (ExcelUtils.isBlankRow(cells)) {
                    continue;
                }
                // rowNumber = số dòng hiển thị trong Excel
                rows.add(StudentRowData.fromCells(i + 1, cells));
            }
            if (rows.isEmpty()) {
                throw new AppException(ErrorCode.FILE_NO_DATA);
            }
            return rows;
        } catch (IOException e) {
            throw new AppException(ErrorCode.FILE_INVALID_FORMAT);
        }
    }

    private static void validateHeader(Row header, DataFormatter formatter) {
        if (header == null) {
            throw new AppException(ErrorCode.FILE_HEADER_INVALID);
        }
        List<String> cells = ExcelUtils.readRow(header, COLUMN_COUNT, formatter);
        StudentColumn[] columns = StudentColumn.values();
        for (int i = 0; i < COLUMN_COUNT; i++) {
            if (!columns[i].matchesHeader(cells.get(i))) {
                throw new AppException(ErrorCode.FILE_HEADER_INVALID);
            }
        }
    }
}
