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
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Fasilitas fasilitas;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
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
    private LocalDateTime waktuVerifikasi;
    @Column(length = 1000)
    private String catatanPemeriksaan;

    protected KasusKerusakan() {}

    public KasusKerusakan(Fasilitas fasilitas, JenisKerusakan jenis, boolean pengelompokanOtomatis) {
        this.fasilitas = fasilitas;
        this.jenisKerusakan = jenis;
        this.pengelompokanOtomatis = pengelompokanOtomatis;
    }

    public void tambahLaporan(LaporanKerusakan laporan) {
        if (status == StatusKasus.SELESAI || !laporan.mengacuPada(fasilitas, jenisKerusakan)) {
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
        if (gangguan == null || unit < 1 || unit > fasilitas.getJumlahUnit()) {
            throw new IllegalArgumentException("Pilih tingkat gangguan dan masukkan jumlah unit rusak antara 1 dan " + fasilitas.getJumlahUnit() + ".");
        }
        this.pemeriksa = petugas;
        this.bahayaKritis = bahaya;
        this.gangguan = gangguan;
        this.unitRusak = unit;
        this.catatanPemeriksaan = catatan;
        this.waktuVerifikasi = waktu;
        this.terverifikasi = true;
        ubahStatus(StatusKasus.MENUNGGU_PENUGASAN);
    }

    public void tetapkanPrioritas(Prioritas prioritas, String alasan) {
        if (!terverifikasi) throw new IllegalStateException("Verifikasi kasus sebelum menentukan prioritas.");
        this.prioritas = prioritas;
        this.alasanPrioritas = alasan;
    }

    void ubahStatus(StatusKasus berikutnya) {
        if (berikutnya.ordinal() != status.ordinal() + 1) {
            throw new IllegalStateException("Urutan perubahan status kasus tidak valid.");
        }
        status = berikutnya;
    }

    public LocalDateTime getLaporanTerbaru() {
        return daftarLaporan.stream().map(LaporanKerusakan::getWaktuLaporan)
                .max(Comparator.naturalOrder()).orElseThrow();
    }

    public long getPelaporUnik() { return hitungPelaporUnik(); }
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
    public String getCatatanPemeriksaan() { return catatanPemeriksaan; }
}
