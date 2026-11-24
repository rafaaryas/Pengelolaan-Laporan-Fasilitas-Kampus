package com.kelompok5.pelaporanfasilitas.model;

import jakarta.persistence.Embeddable;

/** Foto yang tersimpan di penyimpanan unggahan. Nama simpan berbeda dari nama asli pengunggah. */
@Embeddable
public class Berkas {
    private String simpan;
    private String nama;
    private Long ukuran;

    protected Berkas() {}

    public Berkas(String simpan, String nama, long ukuran) {
        this.simpan = simpan;
        this.nama = nama;
        this.ukuran = ukuran;
    }

    public boolean ada() { return simpan != null; }

    /** "1,2 MB" atau "640 KB". */
    public String getUkuranTeks() {
        if (ukuran == null) return "";
        if (ukuran >= 1024 * 1024) return String.format(java.util.Locale.forLanguageTag("id"), "%.1f MB", ukuran / 1048576.0);
        return Math.max(1, Math.round(ukuran / 1024.0)) + " KB";
    }

    public String getSimpan() { return simpan; }
    public String getNama() { return nama; }
    public Long getUkuran() { return ukuran; }
}
