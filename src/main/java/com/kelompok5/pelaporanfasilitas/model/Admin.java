package com.kelompok5.pelaporanfasilitas.model;

import java.time.LocalDateTime;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("ADMIN")
public class Admin extends Pengguna {
    protected Admin() {}

    @Override
    public boolean bolehMelakukan(Aksi aksi) { return aksi == Aksi.KELOLA_KASUS; }

    public void verifikasi(KasusKerusakan kasus, LocalDateTime waktu) { kasus.verifikasiOleh(this, waktu); }

    public TugasPerbaikan tetapkanPenugasan(KasusKerusakan kasus, Petugas petugas,
                                          LocalDateTime waktu, String catatan) {
        if (!kasus.isTerverifikasi() || kasus.getStatus() != StatusKasus.MENUNGGU_PENUGASAN) {
            throw new IllegalStateException("Kasus harus terverifikasi dan sedang menunggu penugasan.");
        }
        pastikanPetugasSesuai(kasus, petugas, waktu);
        kasus.ubahStatus(StatusKasus.DITUGASKAN);
        petugas.tandaiSibuk(true);
        return new TugasPerbaikan(kasus, petugas, this, waktu, catatan);
    }

    public void putuskanTindakLanjut(TugasPerbaikan tugas, Petugas petugas, LocalDateTime waktu, String keputusan) {
        if (!tugas.getPetugas().getId().equals(petugas.getId())) pastikanPetugasSesuai(tugas.getKasus(), petugas, waktu);
        tugas.lanjutkan(this, petugas, keputusan, waktu);
        petugas.tandaiSibuk(true);
    }

    private void pastikanPetugasSesuai(KasusKerusakan kasus, Petugas petugas, LocalDateTime waktu) {
        if (!petugas.mampuMenangani(kasus.getFasilitas())) {
            throw new IllegalStateException("Keahlian petugas tidak sesuai dengan fasilitas.");
        }
        if (!petugas.dalamJamKerja(waktu.toLocalTime())) {
            throw new IllegalStateException("Petugas sedang di luar jam kerja.");
        }
    }
}
