package com.kelompok5.pelaporanfasilitas.model;

public enum Keahlian {
    PENDINGIN("Pendingin & ventilasi"),
    LISTRIK("Kelistrikan"),
    PERABOT("Perabot & furnitur"),
    SANITASI("Air & sanitasi");

    private final String label;

    Keahlian(String label) { this.label = label; }

    public String getLabel() { return label; }
}
