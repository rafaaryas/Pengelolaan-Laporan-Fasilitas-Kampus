package com.kelompok5.pelaporanfasilitas.model;

public enum StatusKasus {
    MENUNGGU_VERIFIKASI("Menunggu verifikasi"),
    MENUNGGU_PENUGASAN("Menunggu penugasan"),
    DITUGASKAN("Ditugaskan"),
    DIKERJAKAN("Sedang diperbaiki"),
    PERLU_TINDAK_LANJUT("Perlu Tindak Lanjut"),
    SELESAI("Selesai");

    private final String label;

    StatusKasus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
