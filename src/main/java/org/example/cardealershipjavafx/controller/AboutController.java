package org.example.cardealershipjavafx.controller;

import org.example.cardealershipjavafx.db.DatabaseManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

public class AboutController {
    @FXML
    private Label dbInfoLabel;

    @FXML
    public void initialize() {
        try (Connection conn = DatabaseManager.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String dbName = metaData.getDatabaseProductName();
            String dbVersion = metaData.getDatabaseProductVersion();
            dbInfoLabel.setText("СУБД: " + dbName + " (Версия: " + dbVersion + ")");
        } catch (SQLException e) {
            dbInfoLabel.setText("СУБД: PostgreSQL (ошибка получения версии)");
            System.err.println("Ошибка получения информации о БД: " + e.getMessage());
            // e.printStackTrace(); // Раскомментировать для детальной отладки, если нужно
        }
    }
}