package org.example.cardealershipjavafx.model;

public class Driver {
    private int driverId;
    private String fullName;
    private String licenseNumber;

    public Driver(int driverId, String fullName, String licenseNumber) {
        this.driverId = driverId;
        this.fullName = fullName;
        this.licenseNumber = licenseNumber;
    }

    public Driver() {}

    public int getDriverId() { return driverId; }
    public void setDriverId(int driverId) { this.driverId = driverId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
}