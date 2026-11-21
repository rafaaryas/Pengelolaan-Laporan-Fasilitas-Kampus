package com.kelompok5.pelaporanfasilitas.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
public class TugasPerbaikan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(unique = true)
    private KasusKerusakan kasus;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Petugas petugas;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Admin penugas;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StatusTugas status = StatusTugas.DITUGASKAN;
    @Column(nullable = false)
    private LocalDateTime waktuPenugasan;
    private LocalDateTime waktuMulai;
    private LocalDateTime waktuSelesai;
    @Column(length = 1000)
    private String catatanPenugasan;
    @Column(length = 1000)
    private String catatan;

    protected TugasPerbaikan() {}

    TugasPerbaikan(KasusKerusakan kasus, Petugas petugas, Admin penugas, LocalDateTime waktu, String catatanPenugasan) {
        this.kasus = kasus;
        this.petugas = petugas;
        this.penugas = penugas;
        this.waktuPenugasan = waktu;
        this.catatanPenugasan = catatanPenugasan;
    }

    public void mulai(LocalDateTime waktu) {
        if (status != StatusTugas.DITUGASKAN) throw new IllegalStateException("Tugas ini sudah dimulai.");
        kasus.ubahStatus(StatusKasus.DIKERJAKAN);
        status = StatusTugas.DIKERJAKAN;
        waktuMulai = waktu;
    }

    public void catatProgres(String catatan) {
        if (status != StatusTugas.DIKERJAKAN) throw new IllegalStateException("Mulai tugas untuk mencatat progres atau menyelesaikan pekerjaan.");
        if (catatan == null || catatan.isBlank() || catatan.length() > 1000) {
            throw new IllegalArgumentException("Isi catatan pekerjaan antara 1–1000 karakter.");
        }
        this.catatan = catatan.strip();
    }

    public void selesaikan(LocalDateTime waktu, String catatan) {
        catatProgres(catatan);
        kasus.ubahStatus(StatusKasus.SELESAI);
        status = StatusTugas.SELESAI;
        waktuSelesai = waktu;
        petugas.tandaiSibuk(false);
    }

    public Long getId() { return id; }
    public KasusKerusakan getKasus() { return kasus; }
    public Petugas getPetugas() { return petugas; }
    public Admin getPenugas() { return penugas; }
    public StatusTugas getStatus() { return status; }
    public LocalDateTime getWaktuPenugasan() { return waktuPenugasan; }
    public LocalDateTime getWaktuMulai() { return waktuMulai; }
    public LocalDateTime getWaktuSelesai() { return waktuSelesai; }
    public String getCatatanPenugasan() { return catatanPenugasan; }
    public String getCatatan() { return catatan; }
}
