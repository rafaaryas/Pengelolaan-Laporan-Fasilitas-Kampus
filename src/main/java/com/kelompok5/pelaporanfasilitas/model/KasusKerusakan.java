package com.kelompok5.pelaporanfasilitas.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import jakarta.persistence.*;

@Entity
public class KasusKerusakan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    private Fasilitas fasilitas;
    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private JenisKerusakan jenisKerusakan;
    @OneToMany(mappedBy = "kasus", cascade = CascadeType.PERSIST)
    @OrderBy("waktuLaporan ASC, id ASC")
    private List<LaporanKerusakan> daftarLaporan = new ArrayList<>();
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StatusKasus status = StatusKasus.MENUNGGU_VERIFIKASI;
    private boolean pengelompokanOtomatis;
    private boolean terverifikasi;
    private boolean bahayaKritis;
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Gangguan gangguan;
    private Integer unitRusak;
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Prioritas prioritas;
    @Column(length = 500)
    private String alasanPrioritas;
    @ManyToOne(fetch = FetchType.LAZY)
    private Petugas pemeriksa;
    @ManyToOne(fetch = FetchType.LAZY)
    private Admin verifikator;
    private LocalDateTime waktuVerifikasi;
    private LocalDateTime waktuPemeriksaan;
    @Column(length = 1000)
    private String catatanPemeriksaan;

    protected KasusKerusakan() {}

    public KasusKerusakan(Fasilitas fasilitas, JenisKerusakan jenis, boolean pengelompokanOtomatis) {
        this.fasilitas = fasilitas;
        this.jenisKerusakan = jenis;
        this.pengelompokanOtomatis = pengelompokanOtomatis;
    }

    /** Kasus untuk satu laporan berbasis lokasi; tidak digabung otomatis dengan laporan lain. */
    public static KasusKerusakan untukLaporan() { return new KasusKerusakan(null, null, false); }

    public void tambahLaporan(LaporanKerusakan laporan) {
        boolean cocok = fasilitas == null ? daftarLaporan.isEmpty() : laporan.mengacuPada(fasilitas, jenisKerusakan);
        if (status == StatusKasus.SELESAI || !cocok) {
            throw new IllegalStateException("Laporan tidak cocok dengan kasus aktif ini.");
        }
        laporan.hubungkanDengan(this);
        daftarLaporan.add(laporan);
    }

    public void pindahkanLaporan(LaporanKerusakan laporan, KasusKerusakan tujuan) {
        if (terverifikasi || tujuan.terverifikasi || daftarLaporan.size() < 2 || !daftarLaporan.contains(laporan)) {
            throw new IllegalStateException("Koreksi pengelompokan hanya tersedia sebelum verifikasi dan harus menyisakan satu laporan pada kasus asal.");
        }
        tujuan.tambahLaporan(laporan);
        daftarLaporan.remove(laporan);
    }

    public long hitungPelaporUnik() {
        return daftarLaporan.stream().map(laporan -> laporan.getPelapor().getId()).distinct().count();
    }

    void catatVerifikasi(Petugas petugas, boolean bahaya, Gangguan gangguan, int unit, String catatan, LocalDateTime waktu) {
        if (status != StatusKasus.MENUNGGU_VERIFIKASI) {
            throw new IllegalStateException("Kasus ini sudah diverifikasi.");
        }
        isiPemeriksaan(petugas, bahaya, gangguan, unit, catatan, waktu);
        this.waktuVerifikasi = waktu;
        this.terverifikasi = true;
        ubahStatus(StatusKasus.MENUNGGU_PENUGASAN);
    }

    public void catatPemeriksaanTugas(Petugas petugas, boolean bahaya, Gangguan gangguan, int unit,
                                     String catatan, LocalDateTime waktu) {
        if (isSudahDiperiksa() || (status != StatusKasus.DITUGASKAN && status != StatusKasus.DIKERJAKAN)) {
            throw new IllegalStateException("Pemeriksaan hanya dapat dicatat sekali pada tugas yang sedang ditangani.");
        }
        isiPemeriksaan(petugas, bahaya, gangguan, unit, catatan, waktu);
    }

    private void isiPemeriksaan(Petugas petugas, boolean bahaya, Gangguan gangguan, int unit,
                                String catatan, LocalDateTime waktu) {
        int batas = fasilitas == null ? Integer.MAX_VALUE : fasilitas.getJumlahUnit();
        if (gangguan == null || unit < 1 || unit > batas) {
            throw new IllegalArgumentException("Pilih tingkat gangguan dan masukkan jumlah unit rusak antara 1 dan " + batas + ".");
        }
        this.pemeriksa = petugas;
        this.bahayaKritis = bahaya;
        this.gangguan = gangguan;
        this.unitRusak = unit;
        this.catatanPemeriksaan = catatan;
        this.waktuPemeriksaan = waktu;
    }

    /** Admin memeriksa kelengkapan laporan; kasus siap ditugaskan. */
    void verifikasiOleh(Admin admin, LocalDateTime waktu) {
        if (status != StatusKasus.MENUNGGU_VERIFIKASI) {
            throw new IllegalStateException("Laporan ini sudah diverifikasi.");
        }
        this.verifikator = admin;
        this.waktuVerifikasi = waktu;
        this.terverifikasi = true;
        ubahStatus(StatusKasus.MENUNGGU_PENUGASAN);
    }

    public void tetapkanPrioritas(Prioritas prioritas, String alasan) {
        if (!isSudahDiperiksa()) throw new IllegalStateException("Catat pemeriksaan sebelum menentukan prioritas.");
        this.prioritas = prioritas;
        this.alasanPrioritas = alasan;
    }

    void ubahStatus(StatusKasus berikutnya) {
        boolean sah = switch (status) {
            case MENUNGGU_VERIFIKASI -> berikutnya == StatusKasus.MENUNGGU_PENUGASAN;
            case MENUNGGU_PENUGASAN -> berikutnya == StatusKasus.DITUGASKAN;
            case DITUGASKAN -> berikutnya == StatusKasus.DIKERJAKAN || berikutnya == StatusKasus.PERLU_TINDAK_LANJUT;
            case DIKERJAKAN -> berikutnya == StatusKasus.SELESAI || berikutnya == StatusKasus.PERLU_TINDAK_LANJUT;
            case PERLU_TINDAK_LANJUT -> berikutnya == StatusKasus.DIKERJAKAN;
            case SELESAI -> false;
        };
        if (!sah) {
            throw new IllegalStateException("Urutan perubahan status kasus tidak valid.");
        }
        status = berikutnya;
    }

    public LocalDateTime getLaporanTerbaru() {
        return daftarLaporan.stream().map(LaporanKerusakan::getWaktuLaporan)
                .max(Comparator.naturalOrder()).orElseThrow();
    }

    public long getPelaporUnik() { return hitungPelaporUnik(); }
    public Tahap getTahap() { return Tahap.dari(status); }
    /** Laporan pertama pada kasus; kasus berbasis lokasi hanya berisi satu laporan. */
    public LaporanKerusakan getLaporanUtama() { return daftarLaporan.getFirst(); }
    public Admin getVerifikator() { return verifikator; }
    public Long getId() { return id; }
    public Fasilitas getFasilitas() { return fasilitas; }
    public JenisKerusakan getJenisKerusakan() { return jenisKerusakan; }
    public List<LaporanKerusakan> getDaftarLaporan() { return Collections.unmodifiableList(daftarLaporan); }
    public StatusKasus getStatus() { return status; }
    public boolean isTerverifikasi() { return terverifikasi; }
    public boolean isBahayaKritis() { return bahayaKritis; }
    public Gangguan getGangguan() { return gangguan; }
    public Integer getUnitRusak() { return unitRusak; }
    public Prioritas getPrioritas() { return prioritas; }
    public String getAlasanPrioritas() { return alasanPrioritas; }
    public Petugas getPemeriksa() { return pemeriksa; }
    public LocalDateTime getWaktuVerifikasi() { return waktuVerifikasi; }
    public boolean isSudahDiperiksa() { return pemeriksa != null && gangguan != null && unitRusak != null; }
    public LocalDateTime getWaktuPemeriksaan() {
        return waktuPemeriksaan != null ? waktuPemeriksaan : (isSudahDiperiksa() ? waktuVerifikasi : null);
    }
    public String getCatatanPemeriksaan() { return catatanPemeriksaan; }
}
