package com.kelompok5.pelaporanfasilitas.model;

public enum StatusKasus {
    MENUNGGU_VERIFIKASI("Menunggu verifikasi"),
    MENUNGGU_PENUGASAN("Menunggu penugasan"),
    DITUGASKAN("Ditugaskan"),
    DIKERJAKAN("Sedang diperbaiki"),
    SELESAI("Selesai");

    private final String label;

    StatusKasus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
