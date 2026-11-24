package com.kelompok5.pelaporanfasilitas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Lokasi kerusakan seperti yang diisi pelapor. */
@Embeddable
public class Lokasi {
    @Column(length = 60)
    private String fakultas;
    @Column(length = 40)
    private String gedung;
    private Integer lantai;
    @Column(length = 40)
    private String ruang;

    protected Lokasi() {}

    public Lokasi(String fakultas, String gedung, Integer lantai, String ruang) {
        if (fakultas == null || fakultas.isBlank()) throw new IllegalArgumentException("Pilih fakultas.");
        if (gedung == null || gedung.isBlank()) throw new IllegalArgumentException("Pilih gedung.");
        if (lantai == null || lantai < 1) throw new IllegalArgumentException("Pilih lantai.");
        this.fakultas = fakultas.strip();
        this.gedung = gedung.strip();
        this.lantai = lantai;
        this.ruang = ruang == null || ruang.isBlank() ? null : ruang.strip();
    }

    public boolean lengkap() { return fakultas != null && gedung != null && lantai != null; }

    /** "Teknik · B11 · Lantai 2", seperti daftar laporan pada layar. */
    public String ringkas() { return fakultas + " · " + gedung + " · Lantai " + lantai; }

    /** "B11 · Ruang 204" atau "B11" bila ruang tidak diisi. */
    public String gedungRuang() { return ruang == null ? gedung : gedung + " · Ruang " + ruang; }

    /** "B11 · Lantai 2" untuk kolom Gedung pada tabel admin. */
    public String gedungLantai() { return gedung + " · Lantai " + lantai; }

    public String getFakultas() { return fakultas; }
    public String getGedung() { return gedung; }
    public Integer getLantai() { return lantai; }
    public String getRuang() { return ruang; }
}
