package com.example.tlu_gpms_system.common.utils.excel;

import com.example.tlu_gpms_system.configuration.exception.AppException;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Các thao tác dùng chung khi import file Excel (.xlsx)
public final class ExcelUtils {

    private static final String XLSX_EXTENSION = ".xlsx";

    private ExcelUtils() {
    }

    // Kiểm tra file rỗng, đuôi .xlsx và magic bytes thực tế (không tin vào đuôi file / content-type) rồi mở workbook
    public static Workbook openXlsx(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_EMPTY);
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(XLSX_EXTENSION)) {
            throw new AppException(ErrorCode.FILE_INVALID_FORMAT);
        }
        // XSSFWorkbook đọc toàn bộ file vào bộ nhớ nên đóng stream ngay sau khi mở là an toàn
        try (InputStream inputStream = FileMagic.prepareToCheckMagic(file.getInputStream())) {
            if (FileMagic.valueOf(inputStream) != FileMagic.OOXML) {
                throw new AppException(ErrorCode.FILE_INVALID_FORMAT);
            }
            return new XSSFWorkbook(inputStream);
        } catch (AppException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            // File zip nhưng không phải workbook (VD: .docx đổi đuôi), hoặc file hỏng
            throw new AppException(ErrorCode.FILE_INVALID_FORMAT);
        }
    }

    // Có ô gộp chạm vào columnCount cột dữ liệu đầu tiên hay không
    public static boolean hasMergedCells(Sheet sheet, int columnCount) {
        return sheet.getMergedRegions().stream()
                .anyMatch(region -> region.getFirstColumn() < columnCount);
    }

    // Đọc columnCount ô đầu tiên của dòng dưới dạng text đã chuẩn hóa. Dòng/ô không tồn tại -> ""
    public static List<String> readRow(Row row, int columnCount, DataFormatter formatter) {
        List<String> cells = new ArrayList<>(columnCount);
        for (int i = 0; i < columnCount; i++) {
            Cell cell = row == null ? null : row.getCell(i);
            cells.add(TextNormalizer.normalize(readCellAsText(cell, formatter)));
        }
        return cells;
    }

    // Dòng trống hoàn toàn (ghost row): mọi ô đều rỗng sau chuẩn hóa
    public static boolean isBlankRow(List<String> cells) {
        return cells.stream().allMatch(String::isEmpty);
    }

    // Đọc mọi ô dưới dạng text để giữ số 0 ở đầu và không bị số khoa học
    public static String readCellAsText(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() != CellType.FORMULA) {
            // Ô kiểu ERROR được DataFormatter trả về dạng "#N/A", "#REF!", ...
            return formatter.formatCellValue(cell);
        }
        // Ô công thức: dùng giá trị đã tính sẵn trong file, không tự tính lại
        return switch (cell.getCachedFormulaResultType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> formatter.formatRawCellContents(cell.getNumericCellValue(),
                    cell.getCellStyle().getDataFormat(), cell.getCellStyle().getDataFormatString());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            // Lỗi công thức -> trả "#..." để validator coi là sai định dạng
            case ERROR -> formulaErrorText(cell.getErrorCellValue());
            default -> "";
        };
    }

    private static String formulaErrorText(byte errorCode) {
        try {
            return FormulaError.forInt(errorCode).getString();
        } catch (IllegalArgumentException e) {
            return "#ERROR";
        }
    }
}
