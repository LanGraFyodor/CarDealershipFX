package org.example.cardealershipjavafx.controller;

import org.example.cardealershipjavafx.MainApp;
import org.example.cardealershipjavafx.db.DatabaseManager;
import org.example.cardealershipjavafx.util.AlertUtil;
import org.example.cardealershipjavafx.util.FkSelectionResult;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class UniversalCrudController {

    @FXML private Label titleLabel;
    @FXML private TableView<Map<String, Object>> tableView;
    @FXML private Pagination pagination;
    @FXML private TextField pageSizeField;
    @FXML private Button addButton, editButton, deleteButton, selectButton;
    @FXML private HBox crudButtonsBox;
    @FXML private HBox paginationBox;


    private String currentTableNameOrView;
    private String currentIdColumnName; // Для таблиц, не для представлений, используемых для удаления/редактирования
    private String formTitle; // Заголовок, переданный из MainController
    private String defaultSortColumn;
    private boolean readOnlyMode = false;
    private boolean pickerMode = false;

    private int currentPage = 1;
    private int pageSize = 10;
    private int totalRecords = 0;

    @FXML
    public void initialize() {
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        pageSizeField.setText(String.valueOf(pageSize));
        pagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            if (newIndex.intValue() +1 != currentPage) { // Избегаем лишней загрузки при программной установке страницы
                currentPage = newIndex.intValue() + 1;
                loadData();
            }
        });
        // Устанавливаем плейсхолдер для пустой таблицы
        tableView.setPlaceholder(new Label("Нет данных для отображения"));
    }

    public void setTableConfiguration(String tableNameOrView, String title, String idColumnName, String defaultSort, boolean readOnly) {
        this.currentTableNameOrView = tableNameOrView;
        this.formTitle = title; // Сохраняем переданный заголовок
        this.titleLabel.setText(title); // Отображаем его в Label
        this.currentIdColumnName = idColumnName; // Это имя PK для операций CUD
        this.defaultSortColumn = defaultSort != null && !defaultSort.trim().isEmpty() ? defaultSort : idColumnName;
        this.readOnlyMode = readOnly;

        if (readOnly || pickerMode) { // pickerMode также должен быть readOnly для основной таблицы
            crudButtonsBox.setVisible(false);
            crudButtonsBox.setManaged(false);
        }
        if (pickerMode) { // Если это режим выбора FK
            selectButton.setVisible(true);
            selectButton.setManaged(true);
            // Скрыть кнопки CRUD, если они не были скрыты readOnlyMode
            addButton.setVisible(false); addButton.setManaged(false);
            editButton.setVisible(false); editButton.setManaged(false);
            deleteButton.setVisible(false); deleteButton.setManaged(false);
            // Также можно скрыть пагинацию для простоты в режиме выбора, если записей не очень много
            // paginationBox.setVisible(false); paginationBox.setManaged(false);
        }

        updateTotalRecords(); // Это вызовет loadData через слушателя пагинации, если страница изменится, или если это первая загрузка
        loadData(); // Явный вызов для первой загрузки
    }

    public void setPickerMode(boolean isPicker) {
        this.pickerMode = isPicker;
        // Перенастраиваем видимость кнопок при установке/снятии режима выбора
        if (isPicker) {
            readOnlyMode = true; // В режиме выбора основная таблица только для чтения
            crudButtonsBox.setVisible(true); crudButtonsBox.setManaged(true); // Показываем HBox для кнопки "Выбрать"
            addButton.setVisible(false); addButton.setManaged(false);
            editButton.setVisible(false); editButton.setManaged(false);
            deleteButton.setVisible(false); deleteButton.setManaged(false);
            selectButton.setVisible(true); selectButton.setManaged(true);
            tableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE); // В режиме выбора только одну запись
        } else {
            // Возвращаем к обычному режиму (если он не readOnly изначально)
            // readOnlyMode остается как было установлено в setTableConfiguration
            crudButtonsBox.setVisible(!this.readOnlyMode); // this.readOnlyMode - это исходный readOnly
            crudButtonsBox.setManaged(!this.readOnlyMode);
            addButton.setVisible(!this.readOnlyMode); addButton.setManaged(!this.readOnlyMode);
            editButton.setVisible(!this.readOnlyMode); editButton.setManaged(!this.readOnlyMode);
            deleteButton.setVisible(!this.readOnlyMode); deleteButton.setManaged(!this.readOnlyMode);
            selectButton.setVisible(false); selectButton.setManaged(false);
            tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        }
    }


    @FXML
    private void handleChangePageSize() {
        try {
            int newSize = Integer.parseInt(pageSizeField.getText());
            if (newSize > 0 && newSize <= 1000) { // Ограничим максимальный размер
                pageSize = newSize;
                currentPage = 1;
                updateTotalRecords(); // Обновляем пагинацию и данные
            } else {
                AlertUtil.showError("Ошибка", "Неверный размер страницы", "Размер страницы должен быть положительным числом (1-1000).");
                pageSizeField.setText(String.valueOf(pageSize)); // Восстанавливаем предыдущее значение
            }
        } catch (NumberFormatException e) {
            AlertUtil.showError("Ошибка", "Неверный формат", "Введите корректное число для размера страницы.");
            pageSizeField.setText(String.valueOf(pageSize));
        }
    }

    private void updateTotalRecords() {
        if (currentTableNameOrView == null) return;
        try {
            totalRecords = DatabaseManager.getTotalRecordCount(currentTableNameOrView);
            int pageCount = (totalRecords == 0) ? 1 : (int) Math.ceil((double) totalRecords / pageSize);
            pagination.setPageCount(pageCount);
            // Устанавливаем текущую страницу, но не более чем есть страниц
            int newPageIndex = Math.min(currentPage - 1, pageCount - 1);
            if (newPageIndex < 0) newPageIndex = 0;

            if (pagination.getCurrentPageIndex() != newPageIndex) {
                pagination.setCurrentPageIndex(newPageIndex);
            }
            currentPage = newPageIndex + 1;

        } catch (SQLException e) {
            AlertUtil.handleDatabaseError(e, "подсчете записей для " + currentTableNameOrView);
            totalRecords = 0;
            pagination.setPageCount(1);
            pagination.setCurrentPageIndex(0);
        }
    }

    private void loadData() {
        if (currentTableNameOrView == null) return;
        try {
            List<Map<String, Object>> data = DatabaseManager.getPaginatedData(currentTableNameOrView, currentPage, pageSize, defaultSortColumn);
            ObservableList<Map<String, Object>> observableData = FXCollections.observableArrayList(data);

            tableView.getColumns().clear();
            if (!data.isEmpty()) {
                Map<String, Object> firstRow = data.get(0);
                firstRow.keySet().forEach(key -> {
                    TableColumn<Map<String, Object>, Object> column = new TableColumn<>(formatColumnName(key));
                    column.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().get(key)));
                    // Попытка установить ширину колонки на основе содержимого или названия
                    column.setPrefWidth(Math.max(100, formatColumnName(key).length() * 7 + 20)); // Примерная оценка
                    tableView.getColumns().add(column);
                });
            } else {
                // Если данных нет, но мы знаем структуру (например, из предыдущей загрузки или схемы)
                // можно попробовать добавить колонки на основе схемы таблицы
                try {
                    List<Map<String, Object>> schema = DatabaseManager.getTableSchema(currentTableNameOrView.replaceAll("_view$", "").replaceAll("_detailed$", ""));
                    if (!schema.isEmpty()) {
                        for (Map<String, Object> colInfo : schema) {
                            String colName = (String) colInfo.get("name");
                            TableColumn<Map<String, Object>, Object> column = new TableColumn<>(formatColumnName(colName));
                            column.setCellValueFactory(param -> new SimpleObjectProperty<>(null)); // Для пустой таблицы
                            tableView.getColumns().add(column);
                        }
                    } else {
                        tableView.setPlaceholder(new Label("Нет данных или не удалось определить структуру для " + currentTableNameOrView));
                    }
                } catch (SQLException schemaEx) {
                    System.err.println("Could not fetch schema for empty table/view: " + currentTableNameOrView + ". Error: " + schemaEx.getMessage());
                    tableView.setPlaceholder(new Label("Ошибка загрузки структуры для " + currentTableNameOrView));
                }
            }
            tableView.setItems(observableData);
        } catch (SQLException e) {
            AlertUtil.handleDatabaseError(e, "загрузке данных из " + currentTableNameOrView);
            tableView.getItems().clear();
            tableView.getColumns().clear();
            tableView.setPlaceholder(new Label("Ошибка загрузки данных из " + currentTableNameOrView));
        }
    }

    private String formatColumnName(String dbColumnName) {
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


    @FXML
    private void handleAdd() {
        if (readOnlyMode) return;
        openForm(null, "Добавление новой записи в \"" + formTitle + "\"");
    }

    @FXML
    private void handleEdit() {
        if (readOnlyMode) return;
        Map<String, Object> selectedItem = tableView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            AlertUtil.showInfo("Редактирование", "Запись не выбрана", "Пожалуйста, выберите запись для изменения.");
            return;
        }
        openForm(selectedItem, "Изменение записи в \"" + formTitle + "\"");
    }

    private void openForm(Map<String, Object> itemData, String windowTitle) {
        String fxmlFile = "";
        // Для CRUD операций используем имя таблицы, а не представления
        String baseTableName = currentTableNameOrView.replaceAll("_view$", "").replaceAll("_detailed$", "");

        switch (baseTableName) {
            case "manufacturers": fxmlFile = "fxml/manufacturer_form.fxml"; break;
            case "drivers": fxmlFile = "fxml/driver_form.fxml"; break;
            case "cars": fxmlFile = "fxml/car_form.fxml"; break;
            case "arrivals": fxmlFile = "fxml/arrival_form.fxml"; break;
            default:
                AlertUtil.showError("Ошибка конфигурации", "Форма не найдена", "Форма для таблицы '" + baseTableName + "' не определена.");
                return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(MainApp.class.getResource(fxmlFile)));
            Parent root = loader.load();

            Object controller = loader.getController();
            // Передача данных в специализированные контроллеры форм
            if (itemData != null) { // Режим редактирования
                if (controller instanceof ManufacturerFormController) {
                    ((ManufacturerFormController) controller).setManufacturerData(itemData);
                } else if (controller instanceof DriverFormController) {
                    ((DriverFormController) controller).setDriverData(itemData);
                } else if (controller instanceof CarFormController) {
                    ((CarFormController) controller).setCarData(itemData);
                } else if (controller instanceof ArrivalFormController) {
                    ((ArrivalFormController) controller).setArrivalData(itemData);
                }
            }
            // Для добавления (itemData == null) форма открывается пустой

            Stage stage = new Stage();
            stage.setTitle(windowTitle);
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(tableView.getScene().getWindow());
            stage.showAndWait();

            // После закрытия формы обновляем данные
            updateTotalRecords();
            loadData();

        } catch (IOException e) {
            AlertUtil.showError("Ошибка загрузки формы", "Не удалось загрузить FXML: " + fxmlFile, e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            AlertUtil.handleDatabaseError(e, "открытии формы для " + baseTableName);
        }
    }

    @FXML
    private void handleDelete() {
        if (readOnlyMode) return;
        ObservableList<Map<String, Object>> selectedItems = tableView.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) {
            AlertUtil.showInfo("Удаление", "Записи не выбраны", "Пожалуйста, выберите одну или несколько записей для удаления.");
            return;
        }
        if (currentIdColumnName == null || currentIdColumnName.trim().isEmpty()){
            AlertUtil.showError("Ошибка конфигурации", "Не задан ключ для удаления", "Невозможно удалить записи без указания ключевого столбца.");
            return;
        }
        // Для удаления используем имя таблицы, а не представления
        String baseTableName = currentTableNameOrView.replaceAll("_view$", "").replaceAll("_detailed$", "");


        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Подтверждение удаления");
        confirmation.setHeaderText("Вы уверены, что хотите удалить выбранные записи (" + selectedItems.size() + " шт.)?");
        confirmation.setContentText("Это действие необратимо.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                List<Object> idsToDelete = selectedItems.stream()
                        .map(item -> item.get(currentIdColumnName.toLowerCase())) // Ключи в map в нижнем регистре
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                if (idsToDelete.isEmpty() && !selectedItems.isEmpty()){
                    AlertUtil.showError("Ошибка удаления", "Не удалось получить ID для удаления.", "Убедитесь, что столбец '" + currentIdColumnName + "' присутствует в данных.");
                    return;
                }

                int affectedRows = DatabaseManager.deleteRecords(baseTableName, currentIdColumnName, idsToDelete);
                AlertUtil.showInfo("Удаление успешно", "Удалено записей: " + affectedRows, "");
                updateTotalRecords();
                loadData();
            } catch (SQLException e) {
                AlertUtil.handleDatabaseError(e, "удалении записей из " + baseTableName);
            }
        }
    }

    @FXML
    private void handleSelectAndClose() {
        Map<String, Object> selectedItem = tableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            Object id = selectedItem.get(currentIdColumnName.toLowerCase());
            if (id == null && !selectedItem.isEmpty() && currentIdColumnName != null) {
                // Попытка найти ключ, если он не совпал по регистру или был назван чуть иначе
                for(String key : selectedItem.keySet()){
                    if(key.equalsIgnoreCase(currentIdColumnName)){
                        id = selectedItem.get(key);
                        break;
                    }
                }
            }
            if (id == null && !selectedItem.isEmpty()){ // Если ID все еще не найден, а запись выбрана, возможно, currentIdColumnName не тот
                AlertUtil.showError("Ошибка выбора", "Не удалось определить ID выбранной записи.", "Проверьте конфигурацию столбца ID: " + currentIdColumnName);
                return;
            }

            FkSelectionResult.setSelectedId(id);
            closeWindow();
        } else {
            AlertUtil.showInfo("Выбор", "Запись не выбрана", "Пожалуйста, выберите запись.");
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) titleLabel.getScene().getWindow();
        stage.close();
    }
}