package com.kelompok5.pelaporanfasilitas.model;

import java.time.LocalDateTime;
import java.util.Objects;
import jakarta.persistence.*;

@Entity
public class LaporanKerusakan {
    static final int PANJANG_JUDUL = 80;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Mahasiswa pelapor;
    /** Hanya untuk laporan lama yang memilih fasilitas terdaftar. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Fasilitas fasilitas;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private KasusKerusakan kasus;
    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private JenisKerusakan jenisKerusakan;
    @Column(length = 120)
    private String judul;
    @Embedded
    private Lokasi lokasi;
    @Column(nullable = false, length = 1600)
    private String deskripsi;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "simpan", column = @Column(name = "foto_bukti", length = 120)),
            @AttributeOverride(name = "nama", column = @Column(name = "foto_bukti_nama", length = 160)),
            @AttributeOverride(name = "ukuran", column = @Column(name = "foto_bukti_ukuran"))})
    private Berkas fotoBukti;
    @Column(length = 200)
    private String fotoKredit;
    @Column(nullable = false)
    private LocalDateTime waktuLaporan;

    protected LaporanKerusakan() {}

    /** Laporan untuk fasilitas terdaftar (alur pengelompokan kasus). */
    LaporanKerusakan(Mahasiswa pelapor, Fasilitas fasilitas, JenisKerusakan jenis, String deskripsi, LocalDateTime waktu) {
        this.pelapor = pelapor;
        this.fasilitas = fasilitas;
        this.jenisKerusakan = jenis;
        this.deskripsi = deskripsi == null ? "" : deskripsi.strip();
        this.waktuLaporan = waktu;
        if (pelapor == null || fasilitas == null || jenis == null) {
            throw new IllegalArgumentException("Pilih mahasiswa, fasilitas, dan jenis kerusakan.");
        }
        validasiIsi();
    }

    /** Laporan berbasis lokasi dengan foto bukti, seperti form "Buat laporan kerusakan". */
    LaporanKerusakan(Mahasiswa pelapor, Lokasi lokasi, String deskripsi, Berkas foto, LocalDateTime waktu) {
        if (pelapor == null || lokasi == null) throw new IllegalArgumentException("Lengkapi lokasi kerusakan.");
        if (foto != null && !foto.ada()) throw new IllegalArgumentException("Foto bukti kerusakan tidak valid.");
        this.pelapor = pelapor;
        this.lokasi = lokasi;
        this.deskripsi = deskripsi == null ? "" : deskripsi.strip();
        this.fotoBukti = foto;
        this.waktuLaporan = waktu;
        validasiIsi();
        this.judul = judulDari(this.deskripsi);
    }

    public void validasiIsi() {
        if (deskripsi.isBlank() || deskripsi.length() > 1600) {
            throw new IllegalArgumentException("Deskripsi harus berisi 1–1600 karakter.");
        }
    }

    /** Judul diambil dari kalimat pertama deskripsi, dipendekkan pada batas kata. */
    static String judulDari(String deskripsi) {
        String kalimat = deskripsi.strip().split("(?<=[.!?])\\s+|\\R", 2)[0].replaceAll("[.!?]+$", "").strip();
        if (kalimat.length() <= PANJANG_JUDUL) return kalimat;
        int potong = kalimat.lastIndexOf(' ', PANJANG_JUDUL - 1);
        return kalimat.substring(0, potong > 40 ? potong : PANJANG_JUDUL - 1).strip() + "…";
    }

    public boolean mengacuPada(Fasilitas fasilitas, JenisKerusakan jenis) {
        return this.fasilitas != null && fasilitas != null
                && Objects.equals(this.fasilitas.getId(), fasilitas.getId()) && jenisKerusakan == jenis;
    }

    void hubungkanDengan(KasusKerusakan kasus) { this.kasus = kasus; }

    /** "UM-0248". */
    public String getKode() { return String.format("UM-%04d", id); }

    public String getJudul() {
        if (judul != null) return judul;
        return fasilitas.getNama() + " · " + jenisKerusakan.getLabel();
    }

    public boolean isBerlokasi() { return lokasi != null && lokasi.lengkap(); }

    /** "Teknik · B11 · Lantai 2". */
    public String getLokasiRingkas() { return isBerlokasi() ? lokasi.ringkas() : fasilitas.getLokasi(); }

    public String getFakultas() { return isBerlokasi() ? lokasi.getFakultas() : "–"; }

    public String getGedungRuang() { return isBerlokasi() ? lokasi.gedungRuang() : fasilitas.getLokasi(); }

    public String getGedungLantai() { return isBerlokasi() ? lokasi.gedungLantai() : fasilitas.getGedung(); }

    public String getLantai() { return isBerlokasi() ? String.valueOf(lokasi.getLantai()) : "–"; }

    public Tahap getTahap() { return Tahap.dari(kasus.getStatus()); }

    public boolean isAdaFoto() { return fotoBukti != null && fotoBukti.ada(); }

    public Long getId() { return id; }
    public Mahasiswa getPelapor() { return pelapor; }
    public Fasilitas getFasilitas() { return fasilitas; }
    public KasusKerusakan getKasus() { return kasus; }
    public JenisKerusakan getJenisKerusakan() { return jenisKerusakan; }
    public Lokasi getLokasi() { return lokasi; }
    public String getDeskripsi() { return deskripsi; }
    public Berkas getFotoBukti() { return fotoBukti; }
    public String getFotoKredit() { return fotoKredit; }
    public LocalDateTime getWaktuLaporan() { return waktuLaporan; }
}
