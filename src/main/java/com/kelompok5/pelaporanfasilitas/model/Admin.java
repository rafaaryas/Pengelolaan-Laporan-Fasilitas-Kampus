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

    public TugasPerbaikan tetapkanPenugasan(KasusKerusakan kasus, Petugas petugas,
                                          LocalDateTime waktu, String catatan) {
        if (!kasus.isTerverifikasi() || kasus.getPrioritas() == null
                || kasus.getStatus() != StatusKasus.MENUNGGU_PENUGASAN) {
            throw new IllegalStateException("Kasus harus terverifikasi dan sedang menunggu penugasan.");
        }
        if (!petugas.mampuMenangani(kasus.getFasilitas())) {
            throw new IllegalStateException("Keahlian petugas tidak sesuai dengan fasilitas.");
        }
        if (!petugas.tersediaPada(waktu.toLocalTime())) {
            throw new IllegalStateException("Petugas sedang bertugas atau di luar jam kerja.");
        }
        kasus.ubahStatus(StatusKasus.DITUGASKAN);
        petugas.tandaiSibuk(true);
        return new TugasPerbaikan(kasus, petugas, this, waktu, catatan);
    }
}
