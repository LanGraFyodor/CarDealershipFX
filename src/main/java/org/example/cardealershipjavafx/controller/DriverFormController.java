package org.example.cardealershipjavafx.controller;

import org.example.cardealershipjavafx.db.DatabaseManager;
import org.example.cardealershipjavafx.util.AlertUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DriverFormController {

    @FXML private TextField fullNameField;
    @FXML private TextField licenseNumberField;

    private Integer currentDriverId;

    public void setDriverData(Map<String, Object> driverData) {
        this.currentDriverId = (Integer) driverData.get("driver_id");
        fullNameField.setText((String) driverData.get("full_name"));
        licenseNumberField.setText((String) driverData.get("license_number"));
    }

    @FXML
    private void handleSave(ActionEvent event) {
        String fullName = fullNameField.getText();
        String licenseNumber = licenseNumberField.getText();

        if (fullName == null || fullName.trim().isEmpty()) {
            AlertUtil.showError("Ошибка ввода", "Не заполнено обязательное поле", "Поле 'Полное имя' обязательно для заполнения.");
            fullNameField.requestFocus();
            return;
        }
        if (fullName.length() > 150) {
            AlertUtil.showError("Ошибка ввода", "Слишком длинное значение", "Поле 'Полное имя' не должно превышать 150 символов.");
            fullNameField.requestFocus();
            return;
        }
        if (licenseNumber != null && licenseNumber.length() > 50) {
            AlertUtil.showError("Ошибка ввода", "Слишком длинное значение", "Поле 'Номер ВУ' не должно превышать 50 символов.");
            licenseNumberField.requestFocus();
            return;
        }


        Map<String, Object> data = new HashMap<>();
        data.put("full_name", fullName.trim());
        data.put("license_number", (licenseNumber == null || licenseNumber.trim().isEmpty()) ? null : licenseNumber.trim());

        try {
            if (currentDriverId == null) {
                DatabaseManager.insertRecord("drivers", data, "driver_id");
                AlertUtil.showInfo("Успех", "Водитель добавлен", "Новый водитель успешно добавлен.");
            } else {
                DatabaseManager.updateRecord("drivers", data, "driver_id", currentDriverId);
                AlertUtil.showInfo("Успех", "Водитель обновлен", "Данные водителя успешно обновлены.");
            }
            closeForm();
        } catch (SQLException e) {
            AlertUtil.handleDatabaseError(e, (currentDriverId == null ? "добавлении" : "обновлении") + " водителя");
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeForm();
    }

    private void closeForm() {
        Stage stage = (Stage) fullNameField.getScene().getWindow();
        stage.close();
    }
}