package org.example.cardealershipjavafx.model;

import java.math.BigDecimal;
import java.sql.Date; // Используем java.sql.Date для соответствия типу DATE в SQL

public class Arrival {
    private int arrivalId;
    private int carId; // FK
    private Integer driverId; // FK, может быть NULL
    private Date arrivalDate;
    private BigDecimal purchasePrice;
    private String vinNumber;
    private String notes;

    // Constructors, Getters, Setters
    public Arrival() {}

    public Arrival(int arrivalId, int carId, Integer driverId, Date arrivalDate, BigDecimal purchasePrice, String vinNumber, String notes) {
        this.arrivalId = arrivalId;
        this.carId = carId;
        this.driverId = driverId;
        this.arrivalDate = arrivalDate;
        this.purchasePrice = purchasePrice;
        this.vinNumber = vinNumber;
        this.notes = notes;
    }

    public int getArrivalId() { return arrivalId; }
    public void setArrivalId(int arrivalId) { this.arrivalId = arrivalId; }
    public int getCarId() { return carId; }
    public void setCarId(int carId) { this.carId = carId; }
    public Integer getDriverId() { return driverId; }
    public void setDriverId(Integer driverId) { this.driverId = driverId; }
    public Date getArrivalDate() { return arrivalDate; }
    public void setArrivalDate(Date arrivalDate) { this.arrivalDate = arrivalDate; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
    public String getVinNumber() { return vinNumber; }
    public void setVinNumber(String vinNumber) { this.vinNumber = vinNumber; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
