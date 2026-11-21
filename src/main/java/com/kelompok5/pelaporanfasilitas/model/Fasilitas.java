package com.kelompok5.pelaporanfasilitas.model;

import jakarta.persistence.*;

@Entity
public class Fasilitas {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String nama;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Keahlian jenis;
    @Column(nullable = false, length = 40)
    private String gedung;
    @Column(nullable = false, length = 80)
    private String ruang;
    private int jumlahUnit;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Kondisi kondisi;

    protected Fasilitas() {}

    public String deskripsiLokasi() { return gedung + " · " + ruang; }
    public String getLokasi() { return deskripsiLokasi(); }
    public Keahlian keahlianDibutuhkan() { return jenis; }
    public void perbaruiKondisi(Kondisi kondisi) { this.kondisi = kondisi; }
    public Long getId() { return id; }
    public String getNama() { return nama; }
    public Keahlian getJenis() { return jenis; }
    public String getGedung() { return gedung; }
    public String getRuang() { return ruang; }
    public int getJumlahUnit() { return jumlahUnit; }
    public Kondisi getKondisi() { return kondisi; }
}
