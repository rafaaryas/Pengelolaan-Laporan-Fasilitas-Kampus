package com.kelompok5.pelaporanfasilitas.model;

public enum Tahap {
    MENUNGGU("Menunggu Verifikasi", "Menunggu", "menunggu"),
    TERVERIFIKASI("Menunggu Penugasan", "Menunggu Penugasan", "verifikasi"),
    DITUGASKAN("Ditugaskan", "Ditugaskan", "verifikasi"),
    DIPERBAIKI("Sedang Diperbaiki", "Diperbaiki", "diperbaiki"),
    TINDAK_LANJUT("Perlu Tindak Lanjut", "Tindak Lanjut", "tindak-lanjut"),
    SELESAI("Selesai", "Selesai", "selesai");

    private final String label;
    private final String labelPendek;
    private final String kelas;

    Tahap(String label, String labelPendek, String kelas) {
        this.label = label;
        this.labelPendek = labelPendek;
        this.kelas = kelas;
    }

    public static Tahap dari(StatusKasus status) {
        return switch (status) {
            case MENUNGGU_VERIFIKASI -> MENUNGGU;
            case MENUNGGU_PENUGASAN -> TERVERIFIKASI;
            case DITUGASKAN -> DITUGASKAN;
            case DIKERJAKAN -> DIPERBAIKI;
            case PERLU_TINDAK_LANJUT -> TINDAK_LANJUT;
            case SELESAI -> SELESAI;
        };
    }

    public boolean mencakup(StatusKasus status) { return dari(status) == this; }

    public String getLabel() { return label; }
    public String getLabelPendek() { return labelPendek; }
    /** Akhiran kelas CSS untuk pill dan titik status. */
    public String getKelas() { return kelas; }
}
