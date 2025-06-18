package com.example.rizqi_elektronik;

import java.io.Serializable;

public class Product implements Serializable {
    private String kode;
    private String merk;
    private double hargajual;
    private int stok;
    private String foto;
    private String deskripsi;
    private String kategori;

    // Getters and Setters

    public String getKategori() {
        return kategori;
    }

    public String getKode() {
        return kode;
    }

    public String getMerk() {
        return merk;
    }

    public double getHargajual() {
        return hargajual;
    }

    public int getStok() {
        return stok;
    }


    public String getFoto() {
        return foto;
    }

    public String getDeskripsi() {
        return deskripsi;
    }

    private int viewCount;

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

}
