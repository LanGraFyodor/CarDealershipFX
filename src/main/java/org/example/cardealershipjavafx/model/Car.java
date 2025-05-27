package org.example.cardealershipjavafx.model;

import java.math.BigDecimal;

public class Car {
    private int carId;
    private int manufacturerId; // FK
    private String model;
    private Integer yearOfManufacture;
    private String color;
    private BigDecimal basePrice;

    // Constructors, Getters, Setters
    public Car() {}

    public Car(int carId, int manufacturerId, String model, Integer yearOfManufacture, String color, BigDecimal basePrice) {
        this.carId = carId;
        this.manufacturerId = manufacturerId;
        this.model = model;
        this.yearOfManufacture = yearOfManufacture;
        this.color = color;
        this.basePrice = basePrice;
    }

    public int getCarId() { return carId; }
    public void setCarId(int carId) { this.carId = carId; }
    public int getManufacturerId() { return manufacturerId; }
    public void setManufacturerId(int manufacturerId) { this.manufacturerId = manufacturerId; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Integer getYearOfManufacture() { return yearOfManufacture; }
    public void setYearOfManufacture(Integer yearOfManufacture) { this.yearOfManufacture = yearOfManufacture; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
}