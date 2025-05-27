package org.example.cardealershipjavafx.db;

import org.postgresql.util.PSQLException;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseManager {
    // ОБЯЗАТЕЛЬНО ИЗМЕНИТЕ ЭТИ ДАННЫЕ НА СВОИ
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/car_dealership"; // Убедись, что имя БД car_dealership
    private static final String DB_USER = "postgres"; // Ваш пользователь PostgreSQL
    private static final String DB_PASSWORD = "5367"; // Ваш пароль

    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("org.postgresql.Driver");
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            } catch (ClassNotFoundException e) {
                throw new SQLException("PostgreSQL JDBC Driver not found.", e);
            }
        }
        return connection;
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    public static List<Map<String, Object>> fetchData(String query) throws SQLException {
        List<Map<String, Object>> resultList = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnLabel(i).toLowerCase(), rs.getObject(i));
                }
                resultList.add(row);
            }
        } catch (PSQLException e) {
            System.err.println("PSQLException in fetchData: " + e.getMessage() + " SQLState: " + e.getSQLState());
            throw e;
        }
        return resultList;
    }

    public static List<Map<String, Object>> getPaginatedData(String tableNameOrView, int page, int pageSize, String orderByColumn) throws SQLException {
        List<Map<String, Object>> resultList = new ArrayList<>();
        // Для представлений может не быть простого способа узнать PK для сортировки по умолчанию, если он не передан
        // orderByColumn должен быть валидным именем колонки в tableNameOrView
        String effectiveOrderBy = (orderByColumn != null && !orderByColumn.trim().isEmpty()) ? orderByColumn : "1"; // Сортировка по первой колонке, если не указано


        String query = "SELECT * FROM " + tableNameOrView +
                " ORDER BY " + effectiveOrderBy + // Добавляем сортировку
                " LIMIT ? OFFSET ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, pageSize);
            pstmt.setInt(2, (page - 1) * pageSize);
            try (ResultSet rs = pstmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(metaData.getColumnLabel(i).toLowerCase(), rs.getObject(i));
                    }
                    resultList.add(row);
                }
            }
        } catch (PSQLException e) {
            System.err.println("PSQLException in getPaginatedData: " + e.getMessage() + " SQLState: " + e.getSQLState());
            throw e;
        }
        return resultList;
    }

    public static int getTotalRecordCount(String tableNameOrView) throws SQLException {
        String query = "SELECT COUNT(*) FROM " + tableNameOrView;
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (PSQLException e) {
            System.err.println("PSQLException in getTotalRecordCount: " + e.getMessage() + " SQLState: " + e.getSQLState());
            throw e;
        }
        return 0;
    }

    public static Map<String, Object> getRecordById(String tableName, String idColumnName, Object id) throws SQLException {
        String query = "SELECT * FROM " + tableName + " WHERE " + idColumnName + " = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setObject(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> record = new HashMap<>();
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        record.put(metaData.getColumnLabel(i).toLowerCase(), rs.getObject(i));
                    }
                    return record;
                }
            }
        } catch (PSQLException e) {
            System.err.println("PSQLException in getRecordById: " + e.getMessage() + " SQLState: " + e.getSQLState());
            throw e;
        }
        return null;
    }

    public static int insertRecord(String tableName, Map<String, Object> data, String returningColumn) throws SQLException {
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        List<Object> params = new ArrayList<>();

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (columns.length() > 0) {
                columns.append(", ");
                values.append(", ");
            }
            columns.append(entry.getKey());
            values.append("?");
            params.add(entry.getValue());
        }

        String query = "INSERT INTO " + tableName + " (" + columns.toString() + ") VALUES (" + values.toString() + ")";
        if (returningColumn != null && !returningColumn.isEmpty()) {
            query += " RETURNING " + returningColumn;
        }

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, (returningColumn != null && !returningColumn.isEmpty()) ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS)) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            if (returningColumn != null && !returningColumn.isEmpty()) {
                ResultSet rs = pstmt.executeQuery(); // Для RETURNING используем executeQuery
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return -1;
            } else {
                return pstmt.executeUpdate();
            }
        } catch (PSQLException e) {
            System.err.println("PSQLException in insertRecord: " + e.getMessage() + " SQLState: " + e.getSQLState());
            throw e;
        }
    }

    public static int updateRecord(String tableName, Map<String, Object> data, String idColumnName, Object idValue) throws SQLException {
        StringBuilder setClause = new StringBuilder();
        List<Object> params = new ArrayList<>();

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            // Пропускаем обновление самого ID столбца в SET части
            if (entry.getKey().equalsIgnoreCase(idColumnName)) continue;

            if (setClause.length() > 0) {
                setClause.append(", ");
            }
            setClause.append(entry.getKey()).append(" = ?");
            params.add(entry.getValue());
        }

        if (setClause.length() == 0) { // Если обновлять нечего (например, передали только ID)
            return 0;
        }

        params.add(idValue); // Add ID value at the end for WHERE clause

        String query = "UPDATE " + tableName + " SET " + setClause.toString() + " WHERE " + idColumnName + " = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            return pstmt.executeUpdate();
        } catch (PSQLException e) {
            System.err.println("PSQLException in updateRecord: " + e.getMessage() + " SQLState: " + e.getSQLState());
            throw e;
        }
    }

    public static int deleteRecords(String tableName, String idColumnName, List<Object> ids) throws SQLException {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            placeholders.append("?");
            if (i < ids.size() - 1) {
                placeholders.append(",");
            }
        }
        String query = "DELETE FROM " + tableName + " WHERE " + idColumnName + " IN (" + placeholders.toString() + ")";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            for (int i = 0; i < ids.size(); i++) {
                pstmt.setObject(i + 1, ids.get(i));
            }
            return pstmt.executeUpdate();
        } catch (PSQLException e) {
            System.err.println("PSQLException in deleteRecords: " + e.getMessage() + " SQLState: " + e.getSQLState());
            throw e;
        }
    }

    public static List<Map<String, Object>> getTableSchema(String tableName) throws SQLException {
        List<Map<String, Object>> schema = new ArrayList<>();
        // Убедимся, что имя таблицы безопасно (базовая защита от SQL-инъекций здесь)
        if (!tableName.matches("^[a-zA-Z0-9_]+$")) {
            throw new SQLException("Invalid table name for schema retrieval: " + tableName);
        }
        // Пытаемся получить информацию о колонках из information_schema
        // Это может не работать для сложных view, которые не основаны напрямую на таблицах.
        String query = "SELECT column_name, data_type, udt_name " +
                "FROM information_schema.columns " +
                "WHERE table_schema = current_schema() AND table_name = ? " +
                "ORDER BY ordinal_position";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> columnInfo = new HashMap<>();
                    columnInfo.put("name", rs.getString("column_name"));
                    columnInfo.put("type", rs.getString("data_type")); // e.g., integer, character varying
                    columnInfo.put("udt_name", rs.getString("udt_name")); // e.g., int4, varchar
                    schema.add(columnInfo);
                }
            }
        } catch (PSQLException e) {
            System.err.println("PSQLException in getTableSchema for " + tableName + ": " + e.getMessage() + " SQLState: " + e.getSQLState());
            // Если не удалось получить схему (например, это view, для которого нет информации в information_schema.columns таким образом)
            // можно попробовать сделать SELECT * FROM view LIMIT 0; и получить метаданные из ResultSetMetaData
            // но это более сложный сценарий для общей функции. Пока оставляем так.
            throw e;
        }
        return schema;
    }
}