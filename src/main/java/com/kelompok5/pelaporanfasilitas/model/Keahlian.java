package com.kelompok5.pelaporanfasilitas.model;

/** Bidang petugas sarana dan prasarana. */
public enum Keahlian {
    KELISTRIKAN_AC("Kelistrikan & AC"),
    BANGUNAN("Bangunan & sipil"),
    SANITASI("Air & sanitasi"),
    PERABOT("Perabot & furnitur"),
    JARINGAN("Jaringan & perangkat");

    private final String label;

    Keahlian(String label) { this.label = label; }

    public String getLabel() { return label; }
}
