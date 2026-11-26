package com.kelompok5.pelaporanfasilitas.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;

/** Foto bukti atau foto hasil perbaikan yang disimpan langsung di database SQL. */
@Entity
@Table(name = "berkas_foto")
public class Berkas {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nama_asli", nullable = false, length = 160)
    private String nama;

    @Column(name = "mime_type", nullable = false, length = 64)
    private String mimeType;

    @Column(nullable = false)
    private Long ukuran;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] data;

    @Column(name = "waktu_unggah", nullable = false)
    private LocalDateTime waktuUnggah;

    protected Berkas() {}

    public Berkas(String nama, String mimeType, byte[] data, LocalDateTime waktuUnggah) {
        this.nama = nama;
        this.mimeType = mimeType;
        this.data = data;
        this.ukuran = data == null ? 0L : (long) data.length;
        this.waktuUnggah = waktuUnggah == null ? LocalDateTime.now() : waktuUnggah;
    }

    public boolean ada() {
        return id != null || (data != null && data.length > 0);
    }

    /** "1,2 MB" atau "640 KB". */
    public String getUkuranTeks() {
        if (ukuran == null) return "";
        if (ukuran >= 1024 * 1024) return String.format(java.util.Locale.forLanguageTag("id"), "%.1f MB", ukuran / 1048576.0);
        return Math.max(1, Math.round(ukuran / 1024.0)) + " KB";
    }

    public Long getId() { return id; }
    public String getNama() { return nama; }
    public String getMimeType() { return mimeType; }
    public Long getUkuran() { return ukuran; }
    public byte[] getData() { return data; }
    public LocalDateTime getWaktuUnggah() { return waktuUnggah; }
}
