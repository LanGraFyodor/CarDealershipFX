package org.example.cardealershipjavafx.controller;

import org.example.cardealershipjavafx.db.DatabaseManager;
import org.example.cardealershipjavafx.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExportController {

    @FXML private ComboBox<String> sourceComboBox;
    @FXML private CheckBox excelCheckBox;
    @FXML private CheckBox htmlCheckBox;
    @FXML private Button exportButton;

    // Используем LinkedHashMap для сохранения порядка добавления элементов
    private final Map<String, String> availableSources = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        availableSources.put("Производители (manufacturers)", "manufacturers");
        availableSources.put("Водители (drivers)", "drivers");
        availableSources.put("Список автомобилей (cars)", "cars");
        availableSources.put("Журнал поступлений (arrivals)", "arrivals");
        availableSources.put("Отчет: Авто с производителями (cars_with_manufacturer)", "cars_with_manufacturer");
        availableSources.put("Отчет: Детали поступлений (arrivals_detailed)", "arrivals_detailed");
        availableSources.put("Лог аудита (audit_log)", "audit_log");

        sourceComboBox.setItems(FXCollections.observableArrayList(availableSources.keySet()));
        if (!availableSources.isEmpty()) {
            sourceComboBox.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void handleExport(ActionEvent event) {
        String selectedDisplayName = sourceComboBox.getSelectionModel().getSelectedItem();
        if (selectedDisplayName == null) {
            AlertUtil.showError("Ошибка", "Источник не выбран", "Пожалуйста, выберите таблицу или отчет для экспорта.");
            return;
        }
        String dbSourceName = availableSources.get(selectedDisplayName);

        boolean exportToExcel = excelCheckBox.isSelected();
        boolean exportToHtml = htmlCheckBox.isSelected();

        if (!exportToExcel && !exportToHtml) {
            AlertUtil.showError("Ошибка", "Формат не выбран", "Пожалуйста, выберите хотя бы один формат для экспорта.");
            return;
        }

        try {
            // Для лога аудита сортируем по убыванию времени, для остальных по первой колонке если не указано иное
            String orderBy = dbSourceName.equals("audit_log") ? "changed_at DESC" : null;
            // В данном случае мы берем все данные, а не постранично, для экспорта.
            // Если таблицы очень большие, возможно, стоит подумать о потоковой записи.
            List<Map<String, Object>> data;
            if (orderBy != null) {
                data = DatabaseManager.fetchData("SELECT * FROM " + dbSourceName + " ORDER BY " + orderBy);
            } else {
                data = DatabaseManager.fetchData("SELECT * FROM " + dbSourceName);
            }


            if (data.isEmpty()) {
                AlertUtil.showInfo("Экспорт", "Нет данных", "Выбранный источник (" + selectedDisplayName + ") не содержит данных для экспорта.");
                return;
            }

            int successCount = 0;
            if (exportToExcel) {
                if (exportToExcelFile(data, selectedDisplayName, dbSourceName)) successCount++;
            }
            if (exportToHtml) {
                if (exportToHtmlFile(data, selectedDisplayName, dbSourceName)) successCount++;
            }

            if (successCount > 0) {
                AlertUtil.showInfo("Экспорт завершен", "Данные успешно экспортированы.", "Количество успешных операций экспорта: " + successCount);
            }

        } catch (SQLException e) {
            AlertUtil.handleDatabaseError(e, "экспорте данных из " + selectedDisplayName);
        } catch (Exception e) { // Ловим общие ошибки, включая IOException
            AlertUtil.showError("Ошибка экспорта", "Произошла ошибка при экспорте файла.", e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean exportToExcelFile(List<Map<String, Object>> data, String displayName, String baseFileName) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить Excel файл для: " + displayName);
        fileChooser.setInitialFileName(sanitizeFileName(baseFileName) + ".xlsx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files (*.xlsx)", "*.xlsx"));
        File file = fileChooser.showSaveDialog(getStage());

        if (file == null) return false; // Пользователь отменил сохранение

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(displayName.length() > 31 ? sanitizeSheetName(baseFileName) : sanitizeSheetName(displayName));

            Map<String, Object> firstRowData = data.get(0);
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            int cellIdx = 0;
            for (String key : firstRowData.keySet()) {
                Cell cell = headerRow.createCell(cellIdx++);
                cell.setCellValue(formatHeaderName(key));
                cell.setCellStyle(headerStyle);
            }

            CellStyle dateCellStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd"));

            CellStyle dateTimeCellStyle = workbook.createCellStyle();
            dateTimeCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));

            int rowIdx = 1;
            for (Map<String, Object> rowMap : data) {
                Row row = sheet.createRow(rowIdx++);
                cellIdx = 0;
                for (String key : firstRowData.keySet()) {
                    Cell cell = row.createCell(cellIdx++);
                    Object value = rowMap.get(key);
                    if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else if (value instanceof Boolean) {
                        cell.setCellValue((Boolean) value);
                    } else if (value instanceof java.sql.Date) {
                        cell.setCellValue((java.sql.Date) value);
                        cell.setCellStyle(dateCellStyle);
                    } else if (value instanceof java.sql.Timestamp) {
                        cell.setCellValue((java.sql.Timestamp) value);
                        cell.setCellStyle(dateTimeCellStyle);
                    } else if (value != null) {
                        cell.setCellValue(value.toString());
                    } else {
                        cell.setBlank();
                    }
                }
            }

            for (int i = 0; i < firstRowData.keySet().size(); i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
            }
            return true;
        }
    }

    private boolean exportToHtmlFile(List<Map<String, Object>> data, String displayName, String baseFileName) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить HTML файл для: " + displayName);
        fileChooser.setInitialFileName(sanitizeFileName(baseFileName) + ".html");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML Files (*.html)", "*.html"));
        File file = fileChooser.showSaveDialog(getStage());

        if (file == null) return false; // Пользователь отменил сохранение

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"ru\">\n<head>\n<meta charset=\"UTF-8\">\n");
        html.append("<title>Экспорт: ").append(escapeHtml(displayName)).append("</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }\n");
        html.append("h1 { color: #333; }\n");
        html.append("table { border-collapse: collapse; width: 90%; margin: 20px auto; box-shadow: 0 0 10px rgba(0,0,0,0.1); }\n");
        html.append("th, td { border: 1px solid #ddd; padding: 10px 12px; text-align: left; }\n");
        html.append("th { background-color: #f0f0f0; color: #333; font-weight: bold; }\n");
        html.append("tr:nth-child(even) { background-color: #f9f9f9; }\n");
        html.append("tr:hover { background-color: #f1f1f1; }\n");
        html.append("</style>\n</head>\n<body>\n");
        html.append("<h1>").append(escapeHtml(displayName)).append("</h1>\n");
        html.append("<table>\n");

        Map<String, Object> firstRowData = data.get(0);
        html.append("<thead>\n<tr>\n");
        for (String key : firstRowData.keySet()) {
            html.append("<th>").append(escapeHtml(formatHeaderName(key))).append("</th>\n");
        }
        html.append("</tr>\n</thead>\n");

        html.append("<tbody>\n");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Map<String, Object> rowMap : data) {
            html.append("<tr>\n");
            for (String key : firstRowData.keySet()) {
                Object value = rowMap.get(key);
                String cellValue = "";
                if (value != null) {
                    if (value instanceof java.sql.Date) {
                        cellValue = ((java.sql.Date) value).toLocalDate().format(dateFormatter);
                    } else if (value instanceof java.sql.Timestamp) {
                        cellValue = ((java.sql.Timestamp) value).toLocalDateTime().format(dateTimeFormatter);
                    } else {
                        cellValue = value.toString();
                    }
                }
                html.append("<td>").append(escapeHtml(cellValue)).append("</td>\n");
            }
            html.append("</tr>\n");
        }
        html.append("</tbody>\n</table>\n</body>\n</html>");

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(html.toString());
        }
        return true;
    }

    private String formatHeaderName(String dbColumnName) {
        if (dbColumnName == null || dbColumnName.isEmpty()) return "";
        String[] parts = dbColumnName.split("_");
        StringBuilder formattedName = new StringBuilder();
        for (String part : parts) {
            if (part.length() > 0) {
                formattedName.append(part.substring(0, 1).toUpperCase())
                        .append(part.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return formattedName.toString().trim();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&")
                .replace("<", "<")
                .replace(">", ">")
                .replace("\"", "'")
                .replace("'", "'");
    }

    private String sanitizeFileName(String name) {
        // Убираем из имени файла то, что может быть названием таблицы/представления в скобках
        name = name.replaceAll("\\s*\\([^)]*\\)\\s*", "").trim();
        return name.replaceAll("[^a-zA-Z0-9_.-]", "_").toLowerCase();
    }

    private String sanitizeSheetName(String name) {
         // Убираем из имени листа то, что может быть названием таблицы/представления в скобках
        name = name.replaceAll("\\s*\\([^)]*\\)\\s*", "").trim();
        // Имя листа в Excel не должно содержать: []:*?/\\ и быть не длиннее 31 символа
        String sanitized = name.replaceAll("[\\[\\]:*?/\\\\ ]", "_");
        return sanitized.length() > 31 ? sanitized.substring(0, 31) : sanitized;
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = getStage();
        if (stage != null) {
            stage.close();
        }
    }
    private Stage getStage() {
        return (Stage) exportButton.getScene().getWindow();
    }
}
