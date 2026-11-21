package com.kelompok5.pelaporanfasilitas.model;

import jakarta.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "peran")
public abstract class Pengguna {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String nama;
    @Column(nullable = false, length = 150)
    private String kontak;

    protected Pengguna() {}

    public Long getId() { return id; }
    public String getNama() { return nama; }
    public String getKontak() { return kontak; }
    public abstract boolean bolehMelakukan(Aksi aksi);
}
