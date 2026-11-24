package com.kelompok5.pelaporanfasilitas.model;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("MAHASISWA")
public class Mahasiswa extends Pengguna {
    @Column(length = 12, unique = true)
    private String nim;
    @Column(length = 60)
    private String fakultas;

    protected Mahasiswa() {}

    private Mahasiswa(String nama, String email, String nim) {
        super(nama, email);
        this.nim = nim;
    }

    /** Akun mahasiswa yang dibuat otomatis saat email atau NIM belum terdaftar. */
    public static Mahasiswa daftarBaru(String nama, String email, String nim) {
        if (nama == null || nama.isBlank()) throw new IllegalArgumentException("Nama mahasiswa wajib diisi.");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email mahasiswa wajib diisi.");
        return new Mahasiswa(nama.strip(), email, nim);
    }

    public String getNim() { return nim; }
    public String getFakultas() { return fakultas; }

    /** "Mahasiswa · Fakultas Teknik", keterangan di bawah nama pada sidebar. */
    public String getKeterangan() { return fakultas == null ? "Mahasiswa" : "Mahasiswa · Fakultas " + fakultas; }

    @Override
    public boolean bolehMelakukan(Aksi aksi) { return aksi == Aksi.MELAPOR; }

    public LaporanKerusakan buatLaporan(Fasilitas fasilitas, JenisKerusakan jenis, String deskripsi, LocalDateTime waktu) {
        return new LaporanKerusakan(this, fasilitas, jenis, deskripsi, waktu);
    }

    public LaporanKerusakan buatLaporan(Lokasi lokasi, String deskripsi, Berkas foto, LocalDateTime waktu) {
        return new LaporanKerusakan(this, lokasi, deskripsi, foto, waktu);
    }
}
