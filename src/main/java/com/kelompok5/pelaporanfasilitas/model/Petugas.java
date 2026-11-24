package com.kelompok5.pelaporanfasilitas.model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import jakarta.persistence.*;

@Entity
@DiscriminatorValue("PETUGAS")
public class Petugas extends Pengguna {
    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private Keahlian keahlian;
    private LocalTime jamMulai;
    private LocalTime jamSelesai;
    private boolean sibuk;
    @Column(length = 12)
    private String kode;

    protected Petugas() {}

    private Petugas(String nama, String email, Keahlian keahlian, String kode) {
        super(nama, email);
        this.keahlian = keahlian;
        this.kode = kode;
        this.jamMulai = LocalTime.MIDNIGHT;
        this.jamSelesai = LocalTime.MIDNIGHT;
    }

    /** Petugas baru dari halaman admin. Jam kerja awal 24 jam. */
    public static Petugas baru(String nama, String email, Keahlian keahlian, String kode) {
        if (nama == null || nama.isBlank()) throw new IllegalArgumentException("Isi nama petugas.");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Isi email petugas.");
        if (keahlian == null) throw new IllegalArgumentException("Pilih bidang petugas.");
        return new Petugas(nama.strip(), email, keahlian, kode);
    }

    @Override
    public boolean bolehMelakukan(Aksi aksi) { return aksi == Aksi.VERIFIKASI || aksi == Aksi.PERBAIKAN; }

    public boolean mampuMenangani(Fasilitas fasilitas) { return fasilitas == null || keahlian == fasilitas.keahlianDibutuhkan(); }

    public boolean tersediaPada(LocalTime waktu) {
        return !sibuk && dalamJamKerja(waktu);
    }

    public boolean dalamJamKerja(LocalTime waktu) {
        if (jamMulai.equals(jamSelesai)) return true;
        if (jamMulai.isBefore(jamSelesai)) return !waktu.isBefore(jamMulai) && waktu.isBefore(jamSelesai);
        return !waktu.isBefore(jamMulai) || waktu.isBefore(jamSelesai);
    }

    public void periksa(KasusKerusakan kasus, boolean bahaya, Gangguan gangguan, int unit, String catatan, LocalDateTime waktu) {
        kasus.catatVerifikasi(this, bahaya, gangguan, unit, catatan, waktu);
    }

    void tandaiSibuk(boolean sibuk) { this.sibuk = sibuk; }

    /** Status "Bertugas" mengikuti ada tidaknya tugas yang belum selesai. */
    public void perbaruiKetersediaan(boolean adaTugasAktif) { this.sibuk = adaTugasAktif; }

    public Keahlian getKeahlian() { return keahlian; }
    public LocalTime getJamMulai() { return jamMulai; }
    public LocalTime getJamSelesai() { return jamSelesai; }
    public boolean isSibuk() { return sibuk; }
    public String getKode() { return kode == null ? String.format("PTG-%03d", getId()) : kode; }
    public String getKeterangan() { return "Teknisi sarana & prasarana"; }
}
