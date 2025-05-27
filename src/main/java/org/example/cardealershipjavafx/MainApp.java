package org.example.cardealershipjavafx;

import org.example.cardealershipjavafx.db.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(MainApp.class.getResource("fxml/main.fxml")));
            primaryStage.setTitle("Учет автомобилей в магазине v1.0");
            Scene scene = new Scene(root, 950, 750); // Немного увеличил размер
            primaryStage.setScene(scene);
            primaryStage.setMinHeight(600); // Минимальная высота
            primaryStage.setMinWidth(800);  // Минимальная ширина
            primaryStage.show();

            primaryStage.setOnCloseRequest(event -> {
                System.out.println("Application is closing. Closing DB connection.");
                DatabaseManager.closeConnection();
            });

        } catch (IOException e) {
            e.printStackTrace();
            // Здесь можно показать Alert пользователю, что не удалось загрузить главный FXML
            // AlertUtil.showError("Критическая ошибка", "Не удалось загрузить главный интерфейс.", e.getMessage());
            // System.exit(1); // Выход, если главный интерфейс не загружен
        } catch (NullPointerException e) {
            e.printStackTrace();
            // AlertUtil.showError("Критическая ошибка", "Ресурс fxml/main.fxml не найден.", e.getMessage());
            // System.exit(1);
        }
    }

    @Override
    public void stop() throws Exception {
        System.out.println("Application stop method called. Ensuring DB connection is closed.");
        DatabaseManager.closeConnection();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}