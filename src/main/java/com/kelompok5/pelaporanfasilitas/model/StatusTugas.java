package com.kelompok5.pelaporanfasilitas.model;

public enum StatusTugas {
    DITUGASKAN("Ditugaskan"),
    DIKERJAKAN("Sedang diperbaiki"),
    SELESAI("Selesai");

    private final String label;

    StatusTugas(String label) { this.label = label; }

    public String getLabel() { return label; }
}
