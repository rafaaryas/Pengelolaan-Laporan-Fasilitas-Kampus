package com.kelompok5.pelaporanfasilitas.model;

public enum JenisKerusakan {
    TIDAK_BERFUNGSI("Tidak berfungsi"),
    FISIK("Rusak fisik"),
    KEBOCORAN("Kebocoran"),
    BAHAYA_LISTRIK("Kabel terbuka / bahaya listrik"),
    LAINNYA("Lainnya");

    private final String label;

    JenisKerusakan(String label) { this.label = label; }

    public String getLabel() { return label; }
}
