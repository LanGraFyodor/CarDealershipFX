package org.example.cardealershipjavafx.util;

import javafx.scene.control.Alert;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;


public class AlertUtil {

    public static void showError(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.setResizable(true); // Позволяет изменять размер окна для длинных сообщений
        alert.getDialogPane().setPrefWidth(480); // Устанавливаем предпочтительную ширину
        alert.showAndWait();
    }

    public static void showInfo(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void handleDatabaseError(Exception e, String operation) {
        String message = "Произошла ошибка во время операции: " + operation + ".\n";
        if (e instanceof PSQLException) {
            PSQLException psqlEx = (PSQLException) e;
            ServerErrorMessage serverError = psqlEx.getServerErrorMessage();
            String serverMsgStr = (serverError != null) ? serverError.getMessage() : "Нет деталей от сервера.";
            String serverDetailStr = (serverError != null && serverError.getDetail() != null) ? "\nДетали: " + serverError.getDetail() : "";
            String serverHintStr = (serverError != null && serverError.getHint() != null) ? "\nПодсказка: " + serverError.getHint() : "";


            message += "Ошибка PostgreSQL: " + serverMsgStr + serverDetailStr + serverHintStr + "\nSQLState: " + psqlEx.getSQLState();

            if ("P0001".equals(psqlEx.getSQLState()) || "RAISE".equals(psqlEx.getSQLState())) { // unhandled_exception / raise_exception
                if (serverMsgStr.contains("DDL operations are blocked")) {
                    message = "Операция DDL заблокирована триггером базы данных.\n" + serverMsgStr;
                }
            } else if ("23503".equals(psqlEx.getSQLState())) { // foreign_key_violation
                message = "Ошибка внешнего ключа: Запись не может быть удалена или изменена, так как на нее ссылаются другие записи.\n" + serverMsgStr + serverDetailStr;
            } else if ("23505".equals(psqlEx.getSQLState())) { // unique_violation
                message = "Ошибка уникальности: Запись с такими данными уже существует.\n" + serverMsgStr + serverDetailStr;
            } else if ("23502".equals(psqlEx.getSQLState())) { // not_null_violation
                message = "Ошибка NOT NULL: Одно из обязательных полей не заполнено.\n" + serverMsgStr + serverDetailStr;
            } else if ("22P02".equals(psqlEx.getSQLState())) { // invalid_text_representation (неверный формат ввода для типа данных)
                message = "Ошибка формата данных: Введено некорректное значение для одного из полей (например, текст вместо числа).\n" + serverMsgStr + serverDetailStr;
            } else if ("42P01".equals(psqlEx.getSQLState())){ // undefined_table
                message = "Ошибка: Таблица или представление не найдено.\n" + serverMsgStr;
            } else if ("42703".equals(psqlEx.getSQLState())){ // undefined_column
                message = "Ошибка: Колонка не найдена в таблице/представлении.\n" + serverMsgStr;
            }
        } else {
            message += "Общая ошибка: " + e.getMessage();
        }
        showError("Ошибка базы данных", "Сбой операции", message);
        e.printStackTrace(); // Для отладки в консоли
    }
}