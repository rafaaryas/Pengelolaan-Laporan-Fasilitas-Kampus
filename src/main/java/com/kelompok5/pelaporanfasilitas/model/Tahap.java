package com.kelompok5.pelaporanfasilitas.model;

/**
 * Empat tahap yang dilihat pengguna. Status kasus internal punya lima nilai;
 * "Menunggu penugasan" dan "Ditugaskan" sama-sama tampil sebagai Terverifikasi.
 */
public enum Tahap {
    MENUNGGU("Menunggu Verifikasi", "Menunggu", "menunggu"),
    TERVERIFIKASI("Terverifikasi", "Terverifikasi", "verifikasi"),
    DIPERBAIKI("Sedang Diperbaiki", "Diperbaiki", "diperbaiki"),
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
            case MENUNGGU_PENUGASAN, DITUGASKAN -> TERVERIFIKASI;
            case DIKERJAKAN -> DIPERBAIKI;
            case SELESAI -> SELESAI;
        };
    }

    public boolean mencakup(StatusKasus status) { return dari(status) == this; }

    public String getLabel() { return label; }
    public String getLabelPendek() { return labelPendek; }
    /** Akhiran kelas CSS untuk pill dan titik status. */
    public String getKelas() { return kelas; }
    public int getUrutan() { return ordinal() + 1; }
}
