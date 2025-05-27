-- Удаление существующих таблиц, если они есть (для чистого старта)
DROP TABLE IF EXISTS arrivals CASCADE;
DROP TABLE IF EXISTS cars CASCADE;
DROP TABLE IF EXISTS manufacturers CASCADE;
DROP TABLE IF EXISTS drivers CASCADE;
DROP TABLE IF EXISTS audit_log CASCADE;

-- Таблица: Производители (Справочник)
CREATE TABLE manufacturers (
                               manufacturer_id SERIAL PRIMARY KEY,
                               name VARCHAR(100) NOT NULL UNIQUE,
                               country VARCHAR(50)
);

-- Таблица: Водители (Справочник)
CREATE TABLE drivers (
                         driver_id SERIAL PRIMARY KEY,
                         full_name VARCHAR(150) NOT NULL,
                         license_number VARCHAR(50) UNIQUE
);

-- Таблица: Автомобили (Справочник/Список)
CREATE TABLE cars (
                      car_id SERIAL PRIMARY KEY,
                      manufacturer_id INT NOT NULL REFERENCES manufacturers(manufacturer_id) ON DELETE RESTRICT,
                      model VARCHAR(100) NOT NULL,
                      year_of_manufacture INT CHECK (year_of_manufacture > 1900 AND year_of_manufacture < 2100),
                      color VARCHAR(50),
                      base_price NUMERIC(10, 2) CHECK (base_price > 0)
);

-- Таблица: Журнал поступления автомобилей (Оперативные данные)
CREATE TABLE arrivals (
                          arrival_id SERIAL PRIMARY KEY,
                          car_id INT NOT NULL REFERENCES cars(car_id) ON DELETE RESTRICT,
                          driver_id INT REFERENCES drivers(driver_id) ON DELETE SET NULL, -- Водитель может быть удален, поступление останется
                          arrival_date DATE NOT NULL DEFAULT CURRENT_DATE,
                          purchase_price NUMERIC(10, 2) NOT NULL CHECK (purchase_price > 0),
                          vin_number VARCHAR(17) NOT NULL UNIQUE,
                          notes TEXT
);

-- Представления для отчетов
CREATE OR REPLACE VIEW cars_with_manufacturer AS
SELECT
    c.car_id,
    m.name AS manufacturer_name,
    c.model,
    c.year_of_manufacture,
    c.color,
    c.base_price
FROM
    cars c
        JOIN
    manufacturers m ON c.manufacturer_id = m.manufacturer_id;

CREATE OR REPLACE VIEW arrivals_detailed AS
SELECT
    a.arrival_id,
    a.arrival_date,
    m.name AS manufacturer_name,
    c.model AS car_model,
    c.year_of_manufacture,
    c.color AS car_color,
    a.vin_number,
    d.full_name AS driver_name,
    d.license_number AS driver_license,
    a.purchase_price,
    a.notes
FROM
    arrivals a
        JOIN
    cars c ON a.car_id = c.car_id
        JOIN
    manufacturers m ON c.manufacturer_id = m.manufacturer_id
        LEFT JOIN
    drivers d ON a.driver_id = d.driver_id;

-- Таблица для аудита (DML триггер)
CREATE TABLE audit_log (
                           log_id SERIAL PRIMARY KEY,
                           table_name VARCHAR(50) NOT NULL,
                           operation_type VARCHAR(10) NOT NULL, -- INSERT, UPDATE, DELETE
                           old_data JSONB,
                           new_data JSONB,
                           changed_by NAME DEFAULT CURRENT_USER,
                           changed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Функция для DML триггера
CREATE OR REPLACE FUNCTION record_audit()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        INSERT INTO audit_log (table_name, operation_type, new_data)
        VALUES (TG_TABLE_NAME, TG_OP, row_to_json(NEW)::jsonb);
RETURN NEW;
ELSIF (TG_OP = 'UPDATE') THEN
        INSERT INTO audit_log (table_name, operation_type, old_data, new_data)
        VALUES (TG_TABLE_NAME, TG_OP, row_to_json(OLD)::jsonb, row_to_json(NEW)::jsonb);
RETURN NEW;
ELSIF (TG_OP = 'DELETE') THEN
        INSERT INTO audit_log (table_name, operation_type, old_data)
        VALUES (TG_TABLE_NAME, TG_OP, row_to_json(OLD)::jsonb);
RETURN OLD;
END IF;
RETURN NULL; -- Никогда не должно случиться
END;
$$ LANGUAGE plpgsql;

-- DML триггеры на основные таблицы
CREATE TRIGGER manufacturers_audit_trigger
    AFTER INSERT OR UPDATE OR DELETE ON manufacturers
    FOR EACH ROW EXECUTE FUNCTION record_audit();

CREATE TRIGGER cars_audit_trigger
    AFTER INSERT OR UPDATE OR DELETE ON cars
    FOR EACH ROW EXECUTE FUNCTION record_audit();

CREATE TRIGGER drivers_audit_trigger
    AFTER INSERT OR UPDATE OR DELETE ON drivers
    FOR EACH ROW EXECUTE FUNCTION record_audit();

CREATE TRIGGER arrivals_audit_trigger
    AFTER INSERT OR UPDATE OR DELETE ON arrivals
    FOR EACH ROW EXECUTE FUNCTION record_audit();


-- Функция для DDL event триггера
CREATE OR REPLACE FUNCTION block_ddl_changes()
RETURNS event_trigger AS $$
BEGIN
    RAISE EXCEPTION 'DDL operations (like ALTER, DROP, CREATE TABLE) are blocked on database % by an event trigger.', current_database();
END;
$$ LANGUAGE plpgsql;

-- DDL event триггер для блокировки DDL команд (кроме создания триггеров)
-- Чтобы не блокировать создание самого себя или других event триггеров,
-- можно добавить условие на TG_TAG, но для простоты блокируем основные DDL
-- Для реального использования: FILTER ON TAG IN ('ALTER TABLE', 'DROP TABLE', 'CREATE TABLE', ...)
-- Пока что закомментируем, чтобы можно было создать таблицы. Раскомментируйте после создания таблиц.
/*
CREATE EVENT TRIGGER block_ddl_trigger
ON ddl_command_start
WHEN TAG IN ('ALTER TABLE', 'DROP TABLE', 'CREATE TABLE', 'ALTER INDEX', 'DROP INDEX', 'CREATE INDEX', 'ALTER VIEW', 'DROP VIEW', 'CREATE VIEW')
EXECUTE FUNCTION block_ddl_changes();
*/
-- Для тестирования триггера, раскомментируйте и выполните. Затем попробуйте, например, ALTER TABLE manufacturers ADD COLUMN test_col INT;

-- Пример данных (опционально, можно вводить через приложение)
INSERT INTO manufacturers (name, country) VALUES
                                              ('Toyota', 'Japan'),
                                              ('Ford', 'USA'),
                                              ('Volkswagen', 'Germany'),
                                              ('Лада', 'Россия');

INSERT INTO drivers (full_name, license_number) VALUES
                                                    ('Иванов Иван Иванович', '1234567890'),
                                                    ('Петров Петр Петрович', '0987654321'),
                                                    ('Сидорова Мария Алексеевна', '1122334455');

INSERT INTO cars (manufacturer_id, model, year_of_manufacture, color, base_price) VALUES
                                                                                      ((SELECT manufacturer_id FROM manufacturers WHERE name = 'Toyota'), 'Camry', 2021, 'Black', 25000.00),
                                                                                      ((SELECT manufacturer_id FROM manufacturers WHERE name = 'Ford'), 'Focus', 2020, 'Blue', 20000.00),
                                                                                      ((SELECT manufacturer_id FROM manufacturers WHERE name = 'Volkswagen'), 'Golf', 2022, 'Silver', 22000.00),
                                                                                      ((SELECT manufacturer_id FROM manufacturers WHERE name = 'Лада'), 'Vesta', 2023, 'Белый', 15000.00);

INSERT INTO arrivals (car_id, driver_id, arrival_date, purchase_price, vin_number, notes) VALUES
                                                                                              ((SELECT car_id FROM cars WHERE model = 'Camry' LIMIT 1), (SELECT driver_id FROM drivers WHERE full_name = 'Иванов Иван Иванович'), '2023-01-15', 23000.00, 'VIN123CAMRYBLA001', 'Новое поступление'),
((SELECT car_id FROM cars WHERE model = 'Focus' LIMIT 1), (SELECT driver_id FROM drivers WHERE full_name = 'Петров Петр Петрович'), '2023-02-20', 18500.00, 'VIN456FOCUSBLU002', 'Небольшие царапины на бампере'),
((SELECT car_id FROM cars WHERE model = 'Vesta' LIMIT 1), (SELECT driver_id FROM drivers WHERE full_name = 'Иванов Иван Иванович'), '2023-08-10', 14000.00, 'VIN789VESTAWHI003', 'Комплектация Люкс');

SELECT * FROM cars_with_manufacturer;
SELECT * FROM arrivals_detailed;

-- После создания всех таблиц, вы можете раскомментировать и выполнить этот DDL триггер
-- CREATE EVENT TRIGGER block_ddl_trigger
-- ON ddl_command_start
-- WHEN TAG IN ('ALTER TABLE', 'DROP TABLE', 'CREATE TABLE', 'ALTER INDEX', 'DROP INDEX', 'CREATE INDEX', 'ALTER VIEW', 'DROP VIEW', 'CREATE VIEW')
-- EXECUTE FUNCTION block_ddl_changes();