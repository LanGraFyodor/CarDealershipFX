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
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CarFormController {

    @FXML private TextField manufacturerIdField;
    @FXML private TextField modelField;
    @FXML private TextField yearField;
    @FXML private TextField colorField;
    @FXML private TextField basePriceField;

    private Integer currentCarId;
    private Integer selectedManufacturerId; // Храним выбранный ID производителя

    public void setCarData(Map<String, Object> carData) {
        this.currentCarId = (Integer) carData.get("car_id");
        this.selectedManufacturerId = (Integer) carData.get("manufacturer_id");
        manufacturerIdField.setText(selectedManufacturerId != null ? selectedManufacturerId.toString() : "");
        modelField.setText((String) carData.get("model"));
        Object yearObj = carData.get("year_of_manufacture");
        yearField.setText(yearObj != null ? yearObj.toString() : "");
        colorField.setText((String) carData.get("color"));
        Object priceObj = carData.get("base_price");
        basePriceField.setText(priceObj != null ? priceObj.toString() : "");
    }

    @FXML
    private void handleSelectManufacturer() {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(MainApp.class.getResource("fxml/universal_crud.fxml")));
            Parent root = loader.load();

            UniversalCrudController crudController = loader.getController();
            // Передаем имя колонки для сортировки (name) и ID колонки (manufacturer_id)
            crudController.setTableConfiguration("manufacturers", "Выбор производителя", "manufacturer_id", "name", true);
            crudController.setPickerMode(true);

            Stage pickerStage = new Stage();
            pickerStage.setTitle("Выберите производителя");
            pickerStage.setScene(new Scene(root));
            pickerStage.initModality(Modality.WINDOW_MODAL);
            pickerStage.initOwner(getStage());
            pickerStage.showAndWait();

            Object resultId = FkSelectionResult.getSelectedId();
            if (resultId instanceof Integer) {
                selectedManufacturerId = (Integer) resultId;
                manufacturerIdField.setText(selectedManufacturerId.toString());
            }
        } catch (IOException e) {
            AlertUtil.showError("Ошибка загрузки", "Не удалось открыть форму выбора производителя.", e.getMessage());
            e.printStackTrace();
        } catch (Exception e) { // Ловим другие возможные ошибки, например, от БД при загрузке списка
            AlertUtil.handleDatabaseError(e, "открытии формы выбора производителя");
        }
    }


    @FXML
    private void handleSave(ActionEvent event) {
        String model = modelField.getText();
        String yearStr = yearField.getText();
        String color = colorField.getText();
        String basePriceStr = basePriceField.getText();

        if (selectedManufacturerId == null) {
            AlertUtil.showError("Ошибка ввода", "Не выбран производитель", "Пожалуйста, выберите производителя.");
            return;
        }
        if (model == null || model.trim().isEmpty()) {
            AlertUtil.showError("Ошибка ввода", "Не заполнено поле 'Модель'", "");
            modelField.requestFocus();
            return;
        }
        if (model.length() > 100) {
            AlertUtil.showError("Ошибка ввода", "Слишком длинное значение", "Поле 'Модель' не должно превышать 100 символов.");
            modelField.requestFocus();
            return;
        }
        if (color != null && color.length() > 50) {
            AlertUtil.showError("Ошибка ввода", "Слишком длинное значение", "Поле 'Цвет' не должно превышать 50 символов.");
            colorField.requestFocus();
            return;
        }


        Integer year;
        try {
            if (yearStr == null || yearStr.trim().isEmpty()) {
                AlertUtil.showError("Ошибка ввода", "Не заполнено поле 'Год выпуска'", "");
                yearField.requestFocus();
                return;
            }
            year = Integer.parseInt(yearStr.trim());
            if (year <= 1900 || year >= 2100) { // Проверка диапазона из БД
                AlertUtil.showError("Ошибка ввода", "Некорректный год выпуска", "Год должен быть в диапазоне 1901-2099.");
                yearField.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            AlertUtil.showError("Ошибка ввода", "Некорректный формат года", "Поле 'Год выпуска' должно быть числом.");
            yearField.requestFocus();
            return;
        }

        BigDecimal basePrice;
        try {
            if (basePriceStr == null || basePriceStr.trim().isEmpty()) {
                AlertUtil.showError("Ошибка ввода", "Не заполнено поле 'Базовая цена'", "");
                basePriceField.requestFocus();
                return;
            }
            basePrice = new BigDecimal(basePriceStr.trim().replace(",", ".")); // Замена запятой на точку для BigDecimal
            if (basePrice.compareTo(BigDecimal.ZERO) <= 0) { // Проверка из БД (base_price > 0)
                AlertUtil.showError("Ошибка ввода", "Некорректная базовая цена", "Цена должна быть положительным числом.");
                basePriceField.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            AlertUtil.showError("Ошибка ввода", "Некорректный формат цены", "Поле 'Базовая цена' должно быть числом (например, 15000.00).");
            basePriceField.requestFocus();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("manufacturer_id", selectedManufacturerId);
        data.put("model", model.trim());
        data.put("year_of_manufacture", year);
        data.put("color", (color == null || color.trim().isEmpty()) ? null : color.trim());
        data.put("base_price", basePrice);

        try {
            if (currentCarId == null) {
                DatabaseManager.insertRecord("cars", data, "car_id");
                AlertUtil.showInfo("Успех", "Автомобиль добавлен", "Новый автомобиль успешно добавлен.");
            } else {
                DatabaseManager.updateRecord("cars", data, "car_id", currentCarId);
                AlertUtil.showInfo("Успех", "Автомобиль обновлен", "Данные автомобиля успешно обновлены.");
            }
            closeForm();
        } catch (SQLException e) {
            AlertUtil.handleDatabaseError(e, (currentCarId == null ? "добавлении" : "обновлении") + " автомобиля");
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
        return (Stage) modelField.getScene().getWindow();
    }
}