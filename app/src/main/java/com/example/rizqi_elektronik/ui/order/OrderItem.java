package com.example.rizqi_elektronik.ui.order;

public class OrderItem {
    private String foto;
    private String merk;
    private double hargajual;
    private int stok;
    private int qty;

    // Constructor
    public OrderItem(String foto, String merk, double hargajual, int stok, int qty) {
        this.foto = foto;
        this.merk = merk;
        this.hargajual = hargajual;
        this.stok = stok;
        this.qty = qty;
    }

    // Getters and Setters
    public String getFoto() { return foto; }
    public String getMerk() { return merk; }
    public double getHargajual() { return hargajual; }
    public int getStok() { return stok; }
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    // Calculate total price based on quantity
    public double getTotal() {
        return hargajual * qty;
    }
}
