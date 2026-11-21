package com.kelompok5.pelaporanfasilitas.model;

public enum Gangguan {
    RINGAN("Gangguan terbatas"),
    SEBAGIAN("Fungsi terganggu sebagian"),
    BERAT("Fasilitas tidak dapat digunakan");

    private final String label;

    Gangguan(String label) { this.label = label; }

    public String getLabel() { return label; }
}
