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

public class ManufacturerFormController {

    @FXML private TextField nameField;
    @FXML private TextField countryField;

    private Integer currentManufacturerId; // Храним ID для режима редактирования

    // Метод для установки данных при редактировании
    public void setManufacturerData(Map<String, Object> manufacturerData) {
        // Ключи в map приходят в нижнем регистре от DatabaseManager
        this.currentManufacturerId = (Integer) manufacturerData.get("manufacturer_id");
        nameField.setText((String) manufacturerData.get("name"));
        countryField.setText((String) manufacturerData.get("country"));
    }

    @FXML
    private void handleSave(ActionEvent event) {
        String name = nameField.getText();
        String country = countryField.getText();

        if (name == null || name.trim().isEmpty()) {
            AlertUtil.showError("Ошибка ввода", "Не заполнено обязательное поле", "Поле 'Название' обязательно для заполнения.");
            nameField.requestFocus();
            return;
        }
        if (country != null && country.length() > 50) {
            AlertUtil.showError("Ошибка ввода", "Слишком длинное значение", "Поле 'Страна' не должно превышать 50 символов.");
            countryField.requestFocus();
            return;
        }
        if (name.length() > 100) {
            AlertUtil.showError("Ошибка ввода", "Слишком длинное значение", "Поле 'Название' не должно превышать 100 символов.");
            nameField.requestFocus();
            return;
        }


        Map<String, Object> data = new HashMap<>();
        data.put("name", name.trim());
        data.put("country", (country == null || country.trim().isEmpty()) ? null : country.trim());


        try {
            if (currentManufacturerId == null) { // Режим добавления
                DatabaseManager.insertRecord("manufacturers", data, "manufacturer_id");
                AlertUtil.showInfo("Успех", "Производитель добавлен", "Новый производитель успешно добавлен в базу данных.");
            } else { // Режим обновления
                DatabaseManager.updateRecord("manufacturers", data, "manufacturer_id", currentManufacturerId);
                AlertUtil.showInfo("Успех", "Производитель обновлен", "Данные производителя успешно обновлены.");
            }
            closeForm();
        } catch (SQLException e) {
            AlertUtil.handleDatabaseError(e, (currentManufacturerId == null ? "добавлении" : "обновлении") + " производителя");
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeForm();
    }

    private void closeForm() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}
