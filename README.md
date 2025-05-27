# Система учета автомобилей (JavaFX + PostgreSQL)

Учебный проект JavaFX-приложения для управления данными магазина по продаже автомобилей с использованием PostgreSQL.

## Основные возможности

*   Ведение справочников: производители, водители, модели автомобилей.
*   Учет поступлений автомобилей.
*   Просмотр данных с пагинацией (CRUD).
*   Добавление, редактирование, удаление записей.
*   Выбор внешних ключей из связанных таблиц.
*   Просмотр отчетов на основе представлений БД.
*   Экспорт данных в Excel (.xlsx) и HTML.
*   Логирование изменений данных (DML-триггеры).
*   Защита схемы БД от изменений (DDL-триггер).
*   Окно "О программе".

## Технологии

*   Java 21 (или совместимый JDK)
*   JavaFX (версия, совместимая с JDK)
*   PostgreSQL (версия 12+)
*   Apache Maven
*   JDBC, Apache POI

## Структура БД

Ключевые таблицы: `manufacturers`, `drivers`, `cars`, `arrivals`, `audit_log`.
Представления для отчетов: `cars_with_manufacturer`, `arrivals_detailed`.

## Установка и запуск

### Требования:
1.  JDK 21 (или совместимый).
2.  Apache Maven.
3.  PostgreSQL сервер.

### Шаги:
1.  **Клонировать репозиторий:**
    ```bash
    git clone https://github.com/ВАШ_ЛОГИН/ВАШ_РЕПОЗИТОРИЙ.git
    cd ВАШ_РЕПОЗИТОРИЙ
    ```
2.  **Настройка PostgreSQL:**
    *   Создайте базу данных `car_dealership`.
    *   Выполните SQL-скрипт из файла `database_setup.sql` для создания таблиц, представлений и триггеров.
    *   (Опционально) Активируйте DDL-триггер из `database_setup.sql` для блокировки изменений схемы.
3.  **Настройка подключения к БД в приложении:**
    *   Отредактируйте файл `src/main/java/org/example/cardealershipjavafx/db/DatabaseManager.java` (или ваш путь), указав ваши `DB_URL`, `DB_USER`, `DB_PASSWORD`.
4.  **Сборка проекта:**
    ```bash
    # Для Windows:
    mvnw.cmd clean package
    # Для macOS/Linux:
    ./mvnw clean package
    # Или если mvnw нет: mvn clean package
    ```
5.  **Запуск приложения:**
    *   **Через Maven (рекомендуется):**
        ```bash
        # Для Windows:
        mvnw.cmd javafx:run
        # Для macOS/Linux:
        ./mvnw javafx:run
        # Или если mvnw нет: mvn javafx:run
        ```
    *   **Через IDE:**
        *   Откройте проект, настройте JDK 21.
        *   Запустите `main()` метод класса `org.example.cardealershipjavafx.MainApp` (или ваш главный класс).
        *   При ошибке "JavaFX runtime components are missing", добавьте VM опции: `--add-modules javafx.controls,javafx.fxml`.
---
