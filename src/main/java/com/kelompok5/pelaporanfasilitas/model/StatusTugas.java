package com.kelompok5.pelaporanfasilitas.model;

public enum StatusTugas {
    DITUGASKAN("Ditugaskan"),
    DIKERJAKAN("Sedang diperbaiki"),
    PERLU_TINDAK_LANJUT("Perlu Tindak Lanjut"),
    SELESAI("Selesai");

    private final String label;

    StatusTugas(String label) { this.label = label; }

    public String getLabel() { return label; }
}
