package com.kelompok5.pelaporanfasilitas.model;

import java.time.LocalDateTime;
import java.util.Objects;
import jakarta.persistence.*;

@Entity
public class LaporanKerusakan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Mahasiswa pelapor;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Fasilitas fasilitas;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private KasusKerusakan kasus;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private JenisKerusakan jenisKerusakan;
    @Column(nullable = false, length = 1600)
    private String deskripsi;
    @Column(nullable = false)
    private LocalDateTime waktuLaporan;

    protected LaporanKerusakan() {}

    LaporanKerusakan(Mahasiswa pelapor, Fasilitas fasilitas, JenisKerusakan jenis, String deskripsi, LocalDateTime waktu) {
        this.pelapor = pelapor;
        this.fasilitas = fasilitas;
        this.jenisKerusakan = jenis;
        this.deskripsi = deskripsi == null ? "" : deskripsi.strip();
        this.waktuLaporan = waktu;
        validasiIsi();
    }

    public void validasiIsi() {
        if (pelapor == null || fasilitas == null || jenisKerusakan == null) {
            throw new IllegalArgumentException("Pilih mahasiswa, fasilitas, dan jenis kerusakan.");
        }
        if (deskripsi.isBlank() || deskripsi.length() > 1600) {
            throw new IllegalArgumentException("Deskripsi harus berisi 1–1600 karakter.");
        }
    }

    public boolean mengacuPada(Fasilitas fasilitas, JenisKerusakan jenis) {
        return Objects.equals(this.fasilitas.getId(), fasilitas.getId()) && jenisKerusakan == jenis;
    }

    void hubungkanDengan(KasusKerusakan kasus) { this.kasus = kasus; }
    public Long getId() { return id; }
    public Mahasiswa getPelapor() { return pelapor; }
    public Fasilitas getFasilitas() { return fasilitas; }
    public KasusKerusakan getKasus() { return kasus; }
    public JenisKerusakan getJenisKerusakan() { return jenisKerusakan; }
    public String getDeskripsi() { return deskripsi; }
    public LocalDateTime getWaktuLaporan() { return waktuLaporan; }
}
