package com.example.tlu_gpms_system.testreport;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sau khi chạy xong 1 test class, ghi file mapping .xlsx có đúng thứ tự cột A..P
 * của sheet module trong báo cáo 5.1. Kết quả Passed/Failed lấy từ lần chạy thật của JUnit.
 *
 * Tuỳ chọn (system property hoặc biến môi trường):
 *   utc.round  / UTC_ROUND   : vòng test 1, 2 hoặc 3 (mặc định 1)
 *   utc.tester / UTC_TESTER  : tên người test (mặc định để trống)
 *   utc.outDir / UTC_OUT_DIR : thư mục xuất file (mặc định ../readme.md/test_report/Unit_Test_Report,
 *                              tính từ thư mục tlu_gpms_system khi chạy mvnw)
 *
 * Dùng: @ExtendWith({MockitoExtension.class, UtcReportExtension.class}) trên test class.
 */
public class UtcReportExtension implements TestWatcher, AfterAllCallback {

    static final String[] HEADER = {
            "Test Case ID", "Pre-conditions", "Input parameters", "Expected Results", "Type",
            "Round 1", "Test date", "Tester", "Round 2", "Test date", "Tester",
            "Round 3", "Test date", "Tester", "Defect IDs", "Note"};

    // Thư mục báo cáo 5.1 chung của dự án (readme.md/test_report/Unit_Test_Report ở git root)
    static final String DEFAULT_OUT_DIR = "../readme.md/test_report/Unit_Test_Report";

    private static final Map<String, List<Row>> RESULTS = new ConcurrentHashMap<>();

    @Override
    public void testSuccessful(ExtensionContext context) {
        record(context, "Passed", "");
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        record(context, "Failed", shortMessage(cause));
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        record(context, "Pending", "Aborted: " + shortMessage(cause));
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        record(context, "Pending", "Disabled" + reason.map(r -> ": " + r).orElse(""));
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();
        List<Row> rows = RESULTS.remove(testClass.getName());
        if (rows == null || rows.isEmpty()) return;

        int round = Math.max(1, Math.min(3, Integer.parseInt(option("utc.round", "UTC_ROUND", "1").trim())));
        String tester = option("utc.tester", "UTC_TESTER", "");
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String module = moduleName(testClass);

        List<Row> sorted = new ArrayList<>(rows);
        sorted.sort(Comparator.comparingInt((Row r) -> r.idNumber).thenComparing(r -> r.id));

        Map<String, Integer> idCount = new HashMap<>();
        for (Row r : sorted) idCount.merge(r.id, 1, Integer::sum);

        // Gom theo method được test (dòng nhóm), các nhóm xếp theo mã UTC nhỏ nhất.
        Map<String, List<Row>> groups = new LinkedHashMap<>();
        for (Row r : sorted) groups.computeIfAbsent(r.method, k -> new ArrayList<>()).add(r);

        List<String[]> table = new ArrayList<>();
        table.add(HEADER.clone());
        int passed = 0, failed = 0, pending = 0;
        for (Map.Entry<String, List<Row>> g : groups.entrySet()) {
            String[] groupRow = new String[HEADER.length];
            groupRow[0] = g.getKey();
            table.add(groupRow);
            for (Row r : g.getValue()) {
                String[] c = new String[HEADER.length];
                c[0] = r.id;
                c[1] = r.precondition;
                c[2] = r.input;
                c[3] = r.expected;
                c[4] = r.type;
                int base = 5 + (round - 1) * 3;
                c[base] = r.result;
                c[base + 1] = date;
                c[base + 2] = tester;
                if (round == 1) { c[8] = "Pending"; c[11] = "Pending"; }
                List<String> notes = new ArrayList<>();
                if (!r.note.isEmpty()) notes.add(r.note);
                if (!r.id.startsWith("UTC")) notes.add("[CẢNH BÁO] ID phải bắt đầu bằng UTC");
                if (idCount.get(r.id) > 1) notes.add("[CẢNH BÁO] Trùng mã test case");
                c[15] = String.join(" | ", notes);
                table.add(c);
                if ("Passed".equals(r.result)) passed++;
                else if ("Failed".equals(r.result)) failed++;
                else pending++;
            }
        }

        Path outDir = Paths.get(option("utc.outDir", "UTC_OUT_DIR", DEFAULT_OUT_DIR));
        Path out = outDir.resolve(module + "_round" + round + ".xlsx");
        ReportXlsx.write(out, module, table);
        System.out.printf("[UTC-REPORT] %s | round %d | %d Passed, %d Failed, %d Pending | %s%n",
                module, round, passed, failed, pending, out.toAbsolutePath());
    }

    // ---------------------------------------------------------------- helpers

    private static void record(ExtensionContext context, String result, String note) {
        Method m = context.getRequiredTestMethod();
        UnitTestCase tc = m.getAnnotation(UnitTestCase.class);
        RESULTS.computeIfAbsent(context.getRequiredTestClass().getName(),
                        k -> Collections.synchronizedList(new ArrayList<>()))
                .add(new Row(tc, m.getName(), result, note));
    }

    private static String moduleName(Class<?> testClass) {
        String n = testClass.getSimpleName();
        if (n.endsWith("Tests")) return n.substring(0, n.length() - 5);
        if (n.endsWith("Test")) return n.substring(0, n.length() - 4);
        return n;
    }

    private static String option(String prop, String env, String def) {
        String v = System.getProperty(prop);
        if (v == null || v.trim().isEmpty()) v = System.getenv(env);
        return (v == null || v.trim().isEmpty()) ? def : v.trim();
    }

    private static String shortMessage(Throwable t) {
        String msg = t.getMessage() == null ? "" : t.getMessage().trim();
        int nl = msg.indexOf('\n');
        if (nl >= 0) msg = msg.substring(0, nl);
        String s = t.getClass().getSimpleName() + (msg.isEmpty() ? "" : ": " + msg);
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }

    private static final class Row {
        final String id, method, type, precondition, input, expected, result, note;
        final int idNumber;

        Row(UnitTestCase tc, String testMethodName, String result, String note) {
            this.result = result;
            if (tc == null) {
                this.id = "?" + testMethodName;
                this.method = "(chưa gắn @UnitTestCase)";
                this.type = this.precondition = this.input = this.expected = "";
                this.note = ("[CẢNH BÁO] Test chưa gắn @UnitTestCase " + note).trim();
            } else {
                this.id = tc.id().trim();
                this.method = tc.method().trim();
                this.type = tc.type().label();
                this.precondition = tc.precondition();
                this.input = tc.input();
                this.expected = "Return: " + tc.returns()
                        + "\nException: " + tc.exception()
                        + "\nLog Message: " + tc.log();
                this.note = note;
            }
            String digits = this.id.replaceAll("\\D", "");
            this.idNumber = digits.isEmpty() ? Integer.MAX_VALUE
                    : (int) Math.min(Integer.MAX_VALUE, Long.parseLong(digits.length() > 9 ? digits.substring(0, 9) : digits));
        }
    }
}
