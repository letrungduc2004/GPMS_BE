package com.example.tlu_gpms_system.testreport;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Thông tin của 1 test case trong báo cáo 5.1 (Unit Test).
 * Mỗi thuộc tính ứng với 1 cột trong sheet module của file Excel.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface UnitTestCase {

    /** Cột A - Test Case ID, bắt buộc bắt đầu bằng "UTC" (vd: UTC001). */
    String id();

    /** Dòng nhóm - tên method đang được test (vd: validateScore). */
    String method();

    /** Cột E - Normal / Abnormal / Boundary. */
    Type type();

    /** Cột B - Pre-conditions. */
    String precondition() default "N/A";

    /** Cột C - Input parameters (vd: "score = 10.5"). */
    String input();

    /** Cột D, dòng "Return:" - giá trị trả về mong đợi. */
    String returns() default "N/A";

    /** Cột D, dòng "Exception:" - exception mong đợi (loại + message). */
    String exception() default "N/A";

    /** Cột D, dòng "Log Message:" - thông báo mong đợi. */
    String log() default "N/A";

    enum Type {
        NORMAL("Normal"), ABNORMAL("Abnormal"), BOUNDARY("Boundary");

        private final String label;

        Type(String label) { this.label = label; }

        public String label() { return label; }
    }
}
