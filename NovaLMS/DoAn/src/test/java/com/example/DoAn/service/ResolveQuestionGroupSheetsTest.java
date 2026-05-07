package com.example.DoAn.service.impl;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 Unit Tests for resolveQuestionGroupSheets()
 * Test Matrix ID: resolveQuestionGroupSheets_MATRIX
 * Total Test Cases: 10 (4 Abnormal + 6 Normal)
 */
@DisplayName("Unit Test: resolveQuestionGroupSheets()")
class ResolveQuestionGroupSheetsTest {

    private static final String METHOD_NAME = "resolveQuestionGroupSheets";
    private static final Class<?> SERVICE_CLASS = ExcelQuestionImportServiceImpl.class;

    private ExcelQuestionImportServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        service = new ExcelQuestionImportServiceImpl(
                null, null, null, null, null);
    }

    private Object invokeResolveMethod(Workbook workbook) throws Exception {
        Method method = SERVICE_CLASS.getDeclaredMethod(METHOD_NAME, Workbook.class);
        method.setAccessible(true);
        try {
            return method.invoke(service, workbook);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                } else if (cause instanceof Error) {
                    throw (Error) cause;
                } else {
                    throw new RuntimeException(cause);
                }
            }
            throw e;
        }
    }

    private Workbook createWorkbook() {
        return new XSSFWorkbook();
    }

    private Sheet createSheet(Workbook wb, String name) {
        return wb.createSheet(name);
    }

    private Cell setCellValue(Sheet sheet, int rowIdx, int colIdx, String value) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) {
            row = sheet.createRow(rowIdx);
        }
        Cell cell = row.getCell(colIdx);
        if (cell == null) {
            cell = row.createCell(colIdx);
        }
        cell.setCellValue(value);
        return cell;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INNER CLASS: ResolvedQuestionGroupExcel (mimics the private class in service)
    // ─────────────────────────────────────────────────────────────────────────
    private static class ResolvedQuestionGroupExcel {
        final Sheet groupSheet;
        final Sheet childSheet;
        final int groupHeaderRowIndex;
        final int childHeaderRowIndex;

        ResolvedQuestionGroupExcel(Sheet groupSheet, Sheet childSheet, int groupHeaderRowIndex,
                int childHeaderRowIndex) {
            this.groupSheet = groupSheet;
            this.childSheet = childSheet;
            this.groupHeaderRowIndex = groupHeaderRowIndex;
            this.childHeaderRowIndex = childHeaderRowIndex;
        }

        Sheet getGroupSheet() {
            return groupSheet;
        }

        Sheet getChildSheet() {
            return childSheet;
        }

        int getGroupHeaderRowIdx() {
            return groupHeaderRowIndex;
        }

        int getChildHeaderRowIdx() {
            return childHeaderRowIndex;
        }
    }

    private ResolvedQuestionGroupExcel castToResult(Object result) {
        assertNotNull(result, "Result should not be null");
        assertTrue(result.getClass().getSimpleName().contains("ResolvedQuestionGroupExcel"),
                "Result should be ResolvedQuestionGroupExcel");
        return new ResolvedQuestionGroupExcel(
                (Sheet) getFieldValue(result, "groupSheet"),
                (Sheet) getFieldValue(result, "childSheet"),
                (Integer) getFieldValue(result, "groupHeaderRowIndex"),
                (Integer) getFieldValue(result, "childHeaderRowIndex"));
    }

    private Object getFieldValue(Object obj, String fieldName) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST GROUP 1: Abnormal - Less than 2 sheets
    // Matrix: T1
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("T1: Less than 2 sheets (Abnormal)")
    class T1_LessThanTwoSheets {

        @Test
        @DisplayName("Should throw IllegalArgumentException when workbook has only 1 sheet")
        void testLessThanTwoSheets_OnlyOneSheet() {
            // Arrange
            try (Workbook wb = createWorkbook()) {
                createSheet(wb, "Single Sheet");

                // Act & Assert
                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
                    invokeResolveMethod(wb);
                });
                assertTrue(ex.getMessage().toLowerCase(Locale.ROOT).contains("ít nhất 2 sheet")
                        || ex.getMessage().toLowerCase(Locale.ROOT).contains("at least 2 sheet"),
                        "Error message should mention 'at least 2 sheets'. Got: " + ex.getMessage());
            } catch (Exception e) {
                fail("Unexpected exception during test: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when workbook has no sheets")
        void testLessThanTwoSheets_NoSheets() {
            // Arrange
            try (Workbook wb = createWorkbook()) {
                // Empty workbook (no sheets)

                // Act & Assert
                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
                    invokeResolveMethod(wb);
                });
                assertTrue(ex.getMessage().toLowerCase(Locale.ROOT).contains("ít nhất 2 sheet")
                        || ex.getMessage().toLowerCase(Locale.ROOT).contains("at least 2 sheet"),
                        "Error message should mention 'at least 2 sheets'. Got: " + ex.getMessage());
            } catch (Exception e) {
                fail("Unexpected exception during test: " + e.getMessage());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST GROUP 2: Abnormal - No PASSAGE header found
    // Matrix: T2
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("T2: No PASSAGE header found (Abnormal)")
    class T2_NoPassageHeader {

        @Test
        @DisplayName("Should throw IllegalArgumentException when no sheet has PASSAGE header")
        void testNoPassageHeader() {
            // Arrange
            try (Workbook wb = createWorkbook()) {
                Sheet sheet1 = createSheet(wb, "Sheet 1");
                Sheet sheet2 = createSheet(wb, "Sheet 2");

                // Neither sheet has PASSAGE header - both have CONTENT only
                setCellValue(sheet1, 0, 0, "CONTENT");
                setCellValue(sheet2, 0, 0, "CONTENT");

                // Act & Assert
                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
                    invokeResolveMethod(wb);
                });
                assertTrue(ex.getMessage().toLowerCase(Locale.ROOT).contains("không tìm thấy sheet nhóm")
                        || ex.getMessage().toLowerCase(Locale.ROOT).contains("passage"),
                        "Error message should mention 'Không tìm thấy sheet nhóm'. Got: " + ex.getMessage());
            } catch (Exception e) {
                fail("Unexpected exception during test: " + e.getMessage());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST GROUP 3: Abnormal - No CONTENT header found
    // Matrix: T3
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("T3: No CONTENT header found (Abnormal)")
    class T3_NoContentHeader {

        @Test
        @DisplayName("Should throw IllegalArgumentException when no sheet has CONTENT header")
        void testNoContentHeader() {
            // Arrange
            try (Workbook wb = createWorkbook()) {
                Sheet groupSheet = createSheet(wb, "Group Info");
                // Create second sheet to meet minimum requirement (>= 2 sheets check)
                // This sheet intentionally has no CONTENT header
                createSheet(wb, "Other Sheet");

                // Group sheet has PASSAGE
                setCellValue(groupSheet, 0, 0, "PASSAGE");
                // otherSheet has no CONTENT header

                // Act & Assert
                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
                    invokeResolveMethod(wb);
                });
                assertTrue(ex.getMessage().toLowerCase(Locale.ROOT).contains("không tìm thấy sheet câu hỏi con")
                        || ex.getMessage().toLowerCase(Locale.ROOT).contains("content"),
                        "Error message should mention 'Không tìm thấy sheet câu hỏi con'. Got: " + ex.getMessage());
            } catch (Exception e) {
                fail("Unexpected exception during test: " + e.getMessage());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST GROUP 4: Abnormal - Group and child same sheet
    // Matrix: T4
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("T4: Group and child same sheet (Abnormal)")
    class T4_SameSheet {

        @Test
        @DisplayName("Should throw IllegalArgumentException when PASSAGE and CONTENT are on same sheet")
        void testGroupAndChildSameSheet() {
            // Arrange
            try (Workbook wb = createWorkbook()) {
                Sheet sheet = createSheet(wb, "Same Sheet");

                // PASSAGE at row 0
                setCellValue(sheet, 0, 0, "PASSAGE");
                // CONTENT at row 1 (same sheet)
                setCellValue(sheet, 1, 0, "CONTENT");

                // Act & Assert
                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
                    invokeResolveMethod(wb);
                });
                assertTrue(ex.getMessage().toLowerCase(Locale.ROOT).contains("ít nhất 2 sheet")
                        || ex.getMessage().toLowerCase(Locale.ROOT).contains("at least 2 sheet"),
                        "Error message should mention 'at least 2 sheets'. Got: " + ex.getMessage());
            } catch (Exception e) {
                fail("Unexpected exception during test: " + e.getMessage());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST GROUP 5: Normal - Valid file with default header row (row 0)
    // Matrix: T5
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("T5: Valid file - default header row (Normal)")
    class T5_ValidFileDefaultHeader {

        @Test
        @DisplayName("Should return ResolvedQuestionGroupExcel with correct sheets and header indices")
        void testValidFileDefaultHeader() {
            // Arrange
            try (Workbook wb = createWorkbook()) {
                Sheet groupSheet = createSheet(wb, "Group Info");
                Sheet childSheet = createSheet(wb, "Child Questions");

                // PASSAGE header at row 0
                setCellValue(groupSheet, 0, 0, "PASSAGE");
                // CONTENT header at row 0
                setCellValue(childSheet, 0, 0, "CONTENT");

                // Act
                Object result = invokeResolveMethod(wb);

                // Assert
                ResolvedQuestionGroupExcel resolved = castToResult(result);
                assertEquals(groupSheet, resolved.getGroupSheet(), "Group sheet should be 'Group Info'");
                assertEquals(childSheet, resolved.getChildSheet(), "Child sheet should be 'Child Questions'");
                assertEquals(0, resolved.getGroupHeaderRowIdx(), "Group header row index should be 0");
                assertEquals(0, resolved.getChildHeaderRowIdx(), "Child header row index should be 0");
                assertNotSame(resolved.getGroupSheet(), resolved.getChildSheet(),
                        "Group and child sheets should be different");
            } catch (Exception e) {
                fail("Unexpected exception: " + e.getMessage());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST GROUP 6: Normal - Valid file with custom header row
    // Matrix: T6
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("T6: Valid file - custom header row (Normal)")
    class T6_ValidFileCustomHeader {

        @Test
        @DisplayName("Should find PASSAGE header at custom row (row 5)")
        void testValidFileCustomHeaderRow() {
            // Arrange
            try (Workbook wb = createWorkbook()) {
                Sheet groupSheet = createSheet(wb, "Group Info");
                Sheet childSheet = createSheet(wb, "Child Questions");

                // Add empty rows before header
                groupSheet.createRow(0);
                groupSheet.createRow(1);
                groupSheet.createRow(2);
                groupSheet.createRow(3);
                groupSheet.createRow(4);

                // PASSAGE header at row 5 (custom position)
                setCellValue(groupSheet, 5, 0, "PASSAGE");
                // CONTENT header at row 0
                setCellValue(childSheet, 0, 0, "CONTENT");

                // Act
                Object result = invokeResolveMethod(wb);

                // Assert
                ResolvedQuestionGroupExcel resolved = castToResult(result);
                assertEquals(5, resolved.getGroupHeaderRowIdx(), "Group header row index should be 5");
                assertEquals(0, resolved.getChildHeaderRowIdx(), "Child header row index should be 0");
            } catch (Exception e) {
                fail("Unexpected exception: " + e.getMessage());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST GROUP 7: Normal - Case-insensitive header matching
    // Matrix: T7
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("T7: Case-insensitive header matching (Normal)")
    class T7_CaseInsensitiveHeader {

        @Test
        @DisplayName("Should find headers with lowercase 'passage' and 'content'")
        void testCaseInsensitiveHeader() {
            // Arrange
            try (Workbook wb = createWorkbook()) {
                Sheet groupSheet = createSheet(wb, "GROUP INFO");
                Sheet childSheet = createSheet(wb, "CHILD QUESTIONS");

                // Lowercase headers
                setCellValue(groupSheet, 0, 0, "passage");
                setCellValue(childSheet, 0, 0, "content");

                // Act
                Object result = invokeResolveMethod(wb);

                // Assert
                ResolvedQuestionGroupExcel resolved = castToResult(result);
                assertEquals(groupSheet, resolved.getGroupSheet(), "Should find 'passage' header (case-insensitive)");
                assertEquals(childSheet, resolved.getChildSheet(), "Should find 'content' header (case-insensitive)");
            } catch (Exception e) {
                fail("Unexpected exception: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("Should find headers with mixed case 'Passage' and 'Content'")
        void testMixedCaseHeader() {
            // Arrange
            try (Workbook wb = createWorkbook()) {
                Sheet groupSheet = createSheet(wb, "Group Info");
                Sheet childSheet = createSheet(wb, "Child Questions");

                // Mixed case headers
                setCellValue(groupSheet, 0, 0, "Passage");
                setCellValue(childSheet, 0, 0, "Content");

                // Act
                Object result = invokeResolveMethod(wb);

                // Assert
                ResolvedQuestionGroupExcel resolved = castToResult(result);
                assertNotNull(resolved.getGroupSheet(), "Should find 'Passage' header (case-insensitive)");
                assertNotNull(resolved.getChildSheet(), "Should find 'Content' header (case-insensitive)");
            } catch (Exception e) {
                fail("Unexpected exception: " + e.getMessage());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST GROUP 8: Normal - Special characters in header
    // Matrix: T8
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("T8: Special characters in header (Normal)")
    class T8_SpecialCharacters {

        @Test
        @DisplayName("Should find PASSAGE * header with asterisk")
        void testPassageWithAsterisk() {
            // Arrange
            try (Workbook wb = createWorkbook()) {
                Sheet groupSheet = createSheet(wb, "Group Info");
                Sheet childSheet = createSheet(wb, "Child Questions");

                // PASSAGE with asterisk
                setCellValue(groupSheet, 0, 0, "PASSAGE *");
                setCellValue(childSheet, 0, 0, "CONTENT");

                // Act
                Object result = invokeResolveMethod(wb);

                // Assert
                ResolvedQuestionGroupExcel resolved = castToResult(result);
                assertEquals(groupSheet, resolved.getGroupSheet(), "Should find 'PASSAGE *' header");
            } catch (Exception e) {
                fail("Unexpected exception: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("Should find header with trailing spaces")
        void testHeaderWithTrailingSpaces() {
            // Arrange
            try (Workbook wb = createWorkbook()) {
                Sheet groupSheet = createSheet(wb, "Group Info");
                Sheet childSheet = createSheet(wb, "Child Questions");

                // Headers with spaces
                setCellValue(groupSheet, 0, 0, "  PASSAGE  ");
                setCellValue(childSheet, 0, 0, "  CONTENT  ");

                // Act
                Object result = invokeResolveMethod(wb);

                // Assert
                ResolvedQuestionGroupExcel resolved = castToResult(result);
                assertNotNull(resolved.getGroupSheet(), "Should find header with spaces");
                assertNotNull(resolved.getChildSheet(), "Should find header with spaces");
            } catch (Exception e) {
                fail("Unexpected exception: " + e.getMessage());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST GROUP 9: Normal - Select sheet with "group" in name
    // Matrix: T9
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("T9: Select sheet with 'group' in name (Normal)")
    class T9_SelectGroupSheet {

        @Test
        @DisplayName("Should select sheet with 'Group' in name when multiple sheets have PASSAGE")
        void testSelectSheetWithGroupInName() {
            // Arrange
            try (Workbook wb = createWorkbook()) {
                Sheet groupSheet = createSheet(wb, "Group Info");
                Sheet otherSheet = createSheet(wb, "Other Sheet");
                Sheet childSheet = createSheet(wb, "Child Questions");

                // Both groupSheet and otherSheet have PASSAGE
                setCellValue(groupSheet, 0, 0, "PASSAGE");
                setCellValue(otherSheet, 0, 0, "PASSAGE");
                setCellValue(childSheet, 0, 0, "CONTENT");

                // Act
                Object result = invokeResolveMethod(wb);

                // Assert
                ResolvedQuestionGroupExcel resolved = castToResult(result);
                assertEquals(groupSheet, resolved.getGroupSheet(),
                        "Should select sheet with 'Group' in name when multiple sheets have PASSAGE");
                assertEquals(childSheet, resolved.getChildSheet(), "Child sheet should be correctly identified");
            } catch (Exception e) {
                fail("Unexpected exception: " + e.getMessage());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST GROUP 10: Normal - Select sheet with "child" in name
    // Matrix: T10
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("T10: Select sheet with 'child' in name (Normal)")
    class T10_SelectChildSheet {

        @Test
        @DisplayName("Should select sheet with 'Child' in name when multiple sheets have CONTENT")
        void testSelectSheetWithChildInName() {
            // Arrange
            try (Workbook wb = createWorkbook()) {
                Sheet groupSheet = createSheet(wb, "Group Info");
                Sheet childSheet = createSheet(wb, "Child Questions");
                Sheet otherSheet = createSheet(wb, "Other Questions");

                // Group sheet has PASSAGE
                setCellValue(groupSheet, 0, 0, "PASSAGE");
                // Both childSheet and otherSheet have CONTENT
                setCellValue(childSheet, 0, 0, "CONTENT");
                setCellValue(otherSheet, 0, 0, "CONTENT");

                // Act
                Object result = invokeResolveMethod(wb);

                // Assert
                ResolvedQuestionGroupExcel resolved = castToResult(result);
                assertEquals(groupSheet, resolved.getGroupSheet(), "Group sheet should be 'Group Info'");
                assertEquals(childSheet, resolved.getChildSheet(),
                        "Should select sheet with 'Child' in name when multiple sheets have CONTENT");
            } catch (Exception e) {
                fail("Unexpected exception: " + e.getMessage());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ADDITIONAL EDGE CASE TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Additional Edge Cases")
    class AdditionalEdgeCases {

        @Test
        @DisplayName("Should handle sheet names with different cases")
        void testSheetNamesCaseInsensitive() {
            // Arrange
            try (Workbook wb = createWorkbook()) {
                Sheet groupSheet = createSheet(wb, "GROUP INFO");
                Sheet childSheet = createSheet(wb, "child questions");

                setCellValue(groupSheet, 0, 0, "PASSAGE");
                setCellValue(childSheet, 0, 0, "CONTENT");

                // Act
                Object result = invokeResolveMethod(wb);

                // Assert
                ResolvedQuestionGroupExcel resolved = castToResult(result);
                assertNotNull(resolved.getGroupSheet());
                assertNotNull(resolved.getChildSheet());
            } catch (Exception e) {
                fail("Unexpected exception: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("Should handle Vietnamese characters in sheet names")
        void testVietnameseSheetNames() {
            // Arrange
            try (Workbook wb = createWorkbook()) {
                Sheet groupSheet = createSheet(wb, "Nhóm Câu Hỏi");
                Sheet childSheet = createSheet(wb, "Câu Hỏi Con");

                setCellValue(groupSheet, 0, 0, "PASSAGE");
                setCellValue(childSheet, 0, 0, "CONTENT");

                // Act
                Object result = invokeResolveMethod(wb);

                // Assert
                ResolvedQuestionGroupExcel resolved = castToResult(result);
                assertEquals(groupSheet, resolved.getGroupSheet());
                assertEquals(childSheet, resolved.getChildSheet());
            } catch (Exception e) {
                fail("Unexpected exception: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("Should correctly identify PASSAGE within longer text")
        void testPassageInLongerText() {
            // Arrange
            try (Workbook wb = createWorkbook()) {
                Sheet groupSheet = createSheet(wb, "Group Info");
                Sheet childSheet = createSheet(wb, "Child Questions");

                setCellValue(groupSheet, 0, 0, "PASSAGE_HEADER");
                setCellValue(childSheet, 0, 0, "CONTENT");

                // Act
                Object result = invokeResolveMethod(wb);

                // Assert
                ResolvedQuestionGroupExcel resolved = castToResult(result);
                assertEquals(groupSheet, resolved.getGroupSheet(),
                        "Should find PASSAGE even when part of longer text");
            } catch (Exception e) {
                fail("Unexpected exception: " + e.getMessage());
            }
        }
    }
}
