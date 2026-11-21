package com.kelompok5.pelaporanfasilitas.model;

import java.time.LocalDateTime;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("MAHASISWA")
public class Mahasiswa extends Pengguna {
    protected Mahasiswa() {}

    @Override
    public boolean bolehMelakukan(Aksi aksi) { return aksi == Aksi.MELAPOR; }

    public LaporanKerusakan buatLaporan(Fasilitas fasilitas, JenisKerusakan jenis, String deskripsi, LocalDateTime waktu) {
        return new LaporanKerusakan(this, fasilitas, jenis, deskripsi, waktu);
    }
}
