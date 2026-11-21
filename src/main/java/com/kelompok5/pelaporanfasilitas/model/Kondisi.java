package com.kelompok5.pelaporanfasilitas.model;

public enum Kondisi {
    BAIK("Baik"),
    PERLU_DIPERIKSA("Perlu diperiksa"),
    RUSAK("Rusak");

    private final String label;

    Kondisi(String label) { this.label = label; }

    public String getLabel() { return label; }
}
