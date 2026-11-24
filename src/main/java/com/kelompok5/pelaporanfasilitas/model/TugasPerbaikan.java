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
    @Column(nullable = false, length = 24)
    private StatusTugas status = StatusTugas.DITUGASKAN;
    @Column(nullable = false)
    private LocalDateTime waktuPenugasan;
    private LocalDateTime waktuMulai;
    private LocalDateTime waktuSelesai;
    @Column(length = 1000)
    private String catatanPenugasan;
    @Column(length = 1000)
    private String catatan;
    @Column(length = 1000)
    private String alasanTindakLanjut;
    @Column(length = 1000)
    private String kebutuhanBerikutnya;
    private LocalDateTime waktuTindakLanjut;
    @Column(length = 1000)
    private String keputusanAdmin;
    private LocalDateTime waktuKeputusan;
    @Column(length = 120)
    private String namaPetugasSebelumnya;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "simpan", column = @Column(name = "foto_hasil", length = 120)),
            @AttributeOverride(name = "nama", column = @Column(name = "foto_hasil_nama", length = 160)),
            @AttributeOverride(name = "ukuran", column = @Column(name = "foto_hasil_ukuran"))})
    private Berkas fotoHasil;

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
        pastikanSudahDiperiksa();
        kasus.ubahStatus(StatusKasus.DIKERJAKAN);
        status = StatusTugas.DIKERJAKAN;
        waktuMulai = waktu;
    }

    public void ajukanTindakLanjut(String alasan, String kebutuhan, LocalDateTime waktu) {
        if (status != StatusTugas.DITUGASKAN && status != StatusTugas.DIKERJAKAN) {
            throw new IllegalStateException("Tugas ini tidak dapat diajukan untuk tindak lanjut.");
        }
        pastikanSudahDiperiksa();
        String alasanValid = wajibDiisi(alasan, "Alasan tindak lanjut");
        String kebutuhanValid = wajibDiisi(kebutuhan, "Kebutuhan berikutnya");
        kasus.ubahStatus(StatusKasus.PERLU_TINDAK_LANJUT);
        status = StatusTugas.PERLU_TINDAK_LANJUT;
        alasanTindakLanjut = alasanValid;
        kebutuhanBerikutnya = kebutuhanValid;
        waktuTindakLanjut = waktu;
    }

    void lanjutkan(Admin admin, Petugas pelaksana, String keputusan, LocalDateTime waktu) {
        if (status != StatusTugas.PERLU_TINDAK_LANJUT) {
            throw new IllegalStateException("Kasus sedang tidak menunggu keputusan admin.");
        }
        boolean dialihkan = !petugas.getId().equals(pelaksana.getId());
        String instruksi = dialihkan ? wajibDiisi(keputusan, "Alasan pengalihan") : keputusan;
        kasus.ubahStatus(StatusKasus.DIKERJAKAN);
        status = StatusTugas.DIKERJAKAN;
        if (waktuMulai == null) waktuMulai = waktu;
        if (dialihkan) {
            namaPetugasSebelumnya = petugas.getNama();
            petugas = pelaksana;
            penugas = admin;
            waktuPenugasan = waktu;
        }
        keputusanAdmin = instruksi;
        waktuKeputusan = waktu;
    }

    private void pastikanSudahDiperiksa() {
        if (!kasus.isSudahDiperiksa() || kasus.getPrioritas() == null) {
            throw new IllegalStateException("Simpan hasil pemeriksaan sebelum melanjutkan penanganan.");
        }
    }

    private static String wajibDiisi(String nilai, String label) {
        if (nilai == null || nilai.isBlank() || nilai.length() > 1000) {
            throw new IllegalArgumentException(label + " harus diisi, maksimal 1000 karakter.");
        }
        return nilai.strip();
    }

    public void catatProgres(String catatan) {
        if (status != StatusTugas.DIKERJAKAN) throw new IllegalStateException("Mulai tugas untuk mencatat progres atau menyelesaikan pekerjaan.");
        pastikanSudahDiperiksa();
        this.catatan = wajibDiisi(catatan, "Catatan pekerjaan");
    }

    private void tutup(LocalDateTime waktu, String catatan) {
        catatProgres(catatan);
        kasus.ubahStatus(StatusKasus.SELESAI);
        status = StatusTugas.SELESAI;
        waktuSelesai = waktu;
        petugas.tandaiSibuk(false);
    }

    /** Penyelesaian dari layar petugas: catatan dan foto hasil perbaikan wajib. */
    public void selesaikan(LocalDateTime waktu, String catatan, Berkas foto) {
        if (foto == null || !foto.ada()) throw new IllegalArgumentException("Unggah foto hasil perbaikan sebelum menandai selesai.");
        tutup(waktu, catatan);
        this.fotoHasil = foto;
    }

    public boolean isAdaFotoHasil() { return fotoHasil != null && fotoHasil.ada(); }
    public Berkas getFotoHasil() { return fotoHasil; }
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
    public String getAlasanTindakLanjut() { return alasanTindakLanjut; }
    public String getKebutuhanBerikutnya() { return kebutuhanBerikutnya; }
    public LocalDateTime getWaktuTindakLanjut() { return waktuTindakLanjut; }
    public String getKeputusanAdmin() { return keputusanAdmin; }
    public LocalDateTime getWaktuKeputusan() { return waktuKeputusan; }
    public String getNamaPetugasSebelumnya() { return namaPetugasSebelumnya; }
}
