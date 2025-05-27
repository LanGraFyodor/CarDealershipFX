package org.example.cardealershipjavafx.controller;

import org.example.cardealershipjavafx.MainApp;
import org.example.cardealershipjavafx.db.DatabaseManager;
import org.example.cardealershipjavafx.util.AlertUtil;
import org.example.cardealershipjavafx.util.FkSelectionResult;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ArrivalFormController {

    @FXML private TextField carIdField;
    @FXML private TextField driverIdField;
    @FXML private DatePicker arrivalDatePicker;
    @FXML private TextField purchasePriceField;
    @FXML private TextField vinNumberField;
    @FXML private TextArea notesArea;

    private Integer currentArrivalId;
    private Integer selectedCarId;
    private Integer selectedDriverId; // Может быть null

    public void setArrivalData(Map<String, Object> arrivalData) {
        this.currentArrivalId = (Integer) arrivalData.get("arrival_id");
        this.selectedCarId = (Integer) arrivalData.get("car_id");
        this.selectedDriverId = (Integer) arrivalData.get("driver_id"); // Может быть null

        carIdField.setText(selectedCarId != null ? selectedCarId.toString() : "");
        driverIdField.setText(selectedDriverId != null ? selectedDriverId.toString() : "");

        Object dateObj = arrivalData.get("arrival_date");
        if (dateObj instanceof java.sql.Date) {
            arrivalDatePicker.setValue(((java.sql.Date) dateObj).toLocalDate());
        } else if (dateObj instanceof LocalDate) {
            arrivalDatePicker.setValue((LocalDate) dateObj);
        }


        Object priceObj = arrivalData.get("purchase_price");
        purchasePriceField.setText(priceObj != null ? priceObj.toString() : "");
        vinNumberField.setText((String) arrivalData.get("vin_number"));
        notesArea.setText((String) arrivalData.get("notes"));
    }

    @FXML
    private void initialize() {
        // Установить текущую дату по умолчанию для новых записей
        if (currentArrivalId == null) { // Только если это новая запись
            arrivalDatePicker.setValue(LocalDate.now());
        }
    }

    @FXML
    private void handleSelectCar() {
        openFkPicker("cars_with_manufacturer", "Выбор автомобиля", "car_id", "car_id");
    }

    @FXML
    private void handleSelectDriver() {
        openFkPicker("drivers", "Выбор водителя", "driver_id", "driver_id");
    }

    private void openFkPicker(String tableNameOrView, String windowTitle, String idColumnName, String targetField) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(MainApp.class.getResource("fxml/universal_crud.fxml")));
            Parent root = loader.load();

            UniversalCrudController crudController = loader.getController();
            // Используем idColumnName и для сортировки по умолчанию, и как ключевую колонку
            crudController.setTableConfiguration(tableNameOrView, windowTitle, idColumnName, idColumnName, true);
            crudController.setPickerMode(true);

            Stage pickerStage = new Stage();
            pickerStage.setTitle(windowTitle);
            pickerStage.setScene(new Scene(root));
            pickerStage.initModality(Modality.WINDOW_MODAL);
            pickerStage.initOwner(getStage());
            pickerStage.showAndWait();

            Object resultId = FkSelectionResult.getSelectedId();
            if (resultId instanceof Integer) {
                if ("car_id".equals(targetField)) {
                    selectedCarId = (Integer) resultId;
                    carIdField.setText(selectedCarId.toString());
                } else if ("driver_id".equals(targetField)) {
                    selectedDriverId = (Integer) resultId;
                    driverIdField.setText(selectedDriverId.toString());
                }
            }
        } catch (IOException e) {
            AlertUtil.showError("Ошибка загрузки", "Не удалось открыть форму выбора: " + windowTitle, e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            AlertUtil.handleDatabaseError(e, "открытии формы выбора: " + windowTitle);
        }
    }

    @FXML
    private void handleSave(ActionEvent event) {
        LocalDate arrivalDate = arrivalDatePicker.getValue();
        String purchasePriceStr = purchasePriceField.getText();
        String vin = vinNumberField.getText();
        String notes = notesArea.getText();

        if (selectedCarId == null) {
            AlertUtil.showError("Ошибка ввода", "Не выбран автомобиль", "Пожалуйста, выберите автомобиль.");
            return;
        }
        if (arrivalDate == null) {
            AlertUtil.showError("Ошибка ввода", "Не выбрана дата поступления", "");
            arrivalDatePicker.requestFocus();
            return;
        }
        if (vin == null || vin.trim().isEmpty()) {
            AlertUtil.showError("Ошибка ввода", "Не указан VIN номер", "");
            vinNumberField.requestFocus();
            return;
        }
        if (vin.trim().length() > 17) { // VIN обычно 17 символов
            AlertUtil.showError("Ошибка ввода", "Некорректный VIN", "VIN номер не должен превышать 17 символов.");
            vinNumberField.requestFocus();
            return;
        }

        BigDecimal purchasePrice;
        try {
            if (purchasePriceStr == null || purchasePriceStr.trim().isEmpty()) {
                AlertUtil.showError("Ошибка ввода", "Не указана цена закупки", "");
                purchasePriceField.requestFocus();
                return;
            }
            purchasePrice = new BigDecimal(purchasePriceStr.trim().replace(",", "."));
            if (purchasePrice.compareTo(BigDecimal.ZERO) <= 0) {
                AlertUtil.showError("Ошибка ввода", "Некорректная цена закупки", "Цена должна быть положительным числом.");
                purchasePriceField.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            AlertUtil.showError("Ошибка ввода", "Некорректный формат цены закупки", "");
            purchasePriceField.requestFocus();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("car_id", selectedCarId);
        data.put("driver_id", selectedDriverId); // Может быть null, если водитель не выбран
        data.put("arrival_date", Date.valueOf(arrivalDate)); // Преобразуем LocalDate в java.sql.Date
        data.put("purchase_price", purchasePrice);
        data.put("vin_number", vin.trim());
        data.put("notes", (notes == null || notes.trim().isEmpty()) ? null : notes.trim());

        try {
            if (currentArrivalId == null) {
                DatabaseManager.insertRecord("arrivals", data, "arrival_id");
                AlertUtil.showInfo("Успех", "Поступление добавлено", "Новое поступление успешно зарегистрировано.");
            } else {
                DatabaseManager.updateRecord("arrivals", data, "arrival_id", currentArrivalId);
                AlertUtil.showInfo("Успех", "Поступление обновлено", "Данные о поступлении успешно обновлены.");
            }
            closeForm();
        } catch (SQLException e) {
            AlertUtil.handleDatabaseError(e, (currentArrivalId == null ? "добавлении" : "обновлении") + " поступления");
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeForm();
    }

    private void closeForm() {
        Stage stage = getStage();
        if (stage != null) {
            stage.close();
        }
    }

    private Stage getStage() {
        return (Stage) carIdField.getScene().getWindow();
    }
}