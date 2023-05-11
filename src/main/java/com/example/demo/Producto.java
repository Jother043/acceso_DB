package com.example.demo;

public class Producto {
    private String productCode;
    private String productName;
    private Double MSRP;

    public Producto(String productCode, String productName, Double MSRP) {
        this.productCode = productCode;
        this.productName = productName;
        this.MSRP = MSRP;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getProductName() {
        return productName;
    }

    public Double getMSRP() {
        return MSRP;
    }
}
