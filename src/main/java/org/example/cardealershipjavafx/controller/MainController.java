package org.example.cardealershipjavafx.controller;

import org.example.cardealershipjavafx.MainApp;
import org.example.cardealershipjavafx.util.AlertUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class MainController {

    @FXML
    private BorderPane mainPane;

    private void openUniversalCrudWindow(String tableName, String title, String idColumnName, String defaultSortColumn, boolean readOnly) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(MainApp.class.getResource("fxml/universal_crud.fxml")));
            Parent root = loader.load();

            UniversalCrudController controller = loader.getController();
            // Передаем title в setTableConfiguration, чтобы он использовался для заголовка окна и формы
            controller.setTableConfiguration(tableName, title, idColumnName, defaultSortColumn, readOnly);

            Stage stage = new Stage();
            stage.setTitle(title); // Используем переданный title для заголовка окна
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            if (mainPane.getScene() != null && mainPane.getScene().getWindow() != null) {
                stage.initOwner(mainPane.getScene().getWindow());
            }
            stage.showAndWait();

        } catch (IOException e) {
            AlertUtil.showError("Ошибка загрузки", "Не удалось загрузить форму: " + title, e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            AlertUtil.handleDatabaseError(e, "открытии формы '" + title + "'");
        }
    }


    @FXML
    void handleManufacturers(ActionEvent event) {
        openUniversalCrudWindow("manufacturers", "Справочник: Производители", "manufacturer_id", "name", false);
    }

    @FXML
    void handleDrivers(ActionEvent event) {
        openUniversalCrudWindow("drivers", "Справочник: Водители", "driver_id", "full_name", false);
    }

    @FXML
    void handleCarsList(ActionEvent event) {
        openUniversalCrudWindow("cars", "Список автомобилей", "car_id", "model", false);
    }

    @FXML
    void handleArrivals(ActionEvent event) {
        openUniversalCrudWindow("arrivals", "Журнал поступлений", "arrival_id", "arrival_date DESC", false); // Сортировка по убыванию даты
    }

    @FXML
    void handleReportCarsWithManufacturer(ActionEvent event) {
        openUniversalCrudWindow("cars_with_manufacturer", "Отчет: Автомобили с производителями", "car_id", "manufacturer_name", true);
    }

    @FXML
    void handleReportArrivalsDetailed(ActionEvent event) {
        openUniversalCrudWindow("arrivals_detailed", "Отчет: Детализированные поступления", "arrival_id", "arrival_date DESC", true);
    }

    @FXML
    void handleExportData(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(MainApp.class.getResource("fxml/export_form.fxml")));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Экспорт данных");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            if (mainPane.getScene() != null && mainPane.getScene().getWindow() != null) {
                stage.initOwner(mainPane.getScene().getWindow());
            }
            stage.showAndWait();
        } catch (IOException e) {
            AlertUtil.showError("Ошибка загрузки", "Не удалось загрузить форму экспорта.", e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void handleAbout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(MainApp.class.getResource("fxml/about.fxml")));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("О программе");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            AlertUtil.showError("Ошибка", "Не удалось загрузить окно 'О программе'", e.getMessage());
        }
    }

    @FXML
    void handleExit(ActionEvent event) {
        Stage stage = (Stage) mainPane.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
        // DatabaseManager.closeConnection(); // Закрытие соединения будет в MainApp.stop() или setOnCloseRequest
        System.exit(0);
    }
}