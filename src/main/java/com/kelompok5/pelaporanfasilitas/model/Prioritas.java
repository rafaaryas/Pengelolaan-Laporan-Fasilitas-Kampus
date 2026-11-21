package com.kelompok5.pelaporanfasilitas.model;

public enum Prioritas {
    RENDAH("Rendah"),
    SEDANG("Sedang"),
    TINGGI("Tinggi"),
    KRITIS("Kritis");

    private final String label;

    Prioritas(String label) { this.label = label; }

    public String getLabel() { return label; }
}
