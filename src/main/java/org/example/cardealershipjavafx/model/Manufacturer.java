package org.example.cardealershipjavafx.model;

public class Manufacturer {
    private int manufacturerId;
    private String name;
    private String country;

    public Manufacturer(int manufacturerId, String name, String country) {
        this.manufacturerId = manufacturerId;
        this.name = name;
        this.country = country;
    }

    public Manufacturer() {}

    public int getManufacturerId() { return manufacturerId; }
    public void setManufacturerId(int manufacturerId) { this.manufacturerId = manufacturerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}
