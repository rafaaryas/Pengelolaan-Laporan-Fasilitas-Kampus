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

    protected Petugas() {}

    @Override
    public boolean bolehMelakukan(Aksi aksi) { return aksi == Aksi.VERIFIKASI || aksi == Aksi.PERBAIKAN; }

    public boolean mampuMenangani(Fasilitas fasilitas) { return keahlian == fasilitas.keahlianDibutuhkan(); }

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

    public Keahlian getKeahlian() { return keahlian; }
    public LocalTime getJamMulai() { return jamMulai; }
    public LocalTime getJamSelesai() { return jamSelesai; }
    public boolean isSibuk() { return sibuk; }
}
