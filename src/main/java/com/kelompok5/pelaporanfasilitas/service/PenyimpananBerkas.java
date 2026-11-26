package com.kelompok5.pelaporanfasilitas.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import com.kelompok5.pelaporanfasilitas.model.Berkas;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Memvalidasi dan mengonversi foto bukti serta foto hasil perbaikan menjadi entity {@link Berkas}
 * untuk disimpan langsung ke database SQL.
 */
@Service
public class PenyimpananBerkas {
    public static final long UKURAN_MAKS = 5L * 1024 * 1024;
    static final String PESAN_FORMAT = "Foto maksimal 5 MB, format JPG atau PNG.";

    private final Clock jam;

    public PenyimpananBerkas(Clock jam) {
        this.jam = jam;
    }

    /** Memvalidasi dan membaca foto ke dalam entity Berkas jika ada berkas yang dipilih. */
    public Optional<Berkas> simpan(MultipartFile foto) {
        if (foto == null || foto.isEmpty()) return Optional.empty();
        if (foto.getSize() > UKURAN_MAKS) throw new IllegalArgumentException(PESAN_FORMAT);
        String mimeType = jenis(foto).orElseThrow(() -> new IllegalArgumentException(PESAN_FORMAT));
        try {
            byte[] data = foto.getBytes();
            return Optional.of(new Berkas(namaAsli(foto.getOriginalFilename()), mimeType, data, LocalDateTime.now(jam)));
        } catch (IOException e) {
            throw new UncheckedIOException("Foto gagal dibaca. Coba unggah lagi.", e);
        }
    }

    /** Tipe ditentukan dari tanda tangan biner berkas, bukan dari nama atau header browser. */
    private static Optional<String> jenis(MultipartFile foto) {
        byte[] awal = new byte[8];
        try (InputStream isi = foto.getInputStream()) {
            int dibaca = isi.readNBytes(awal, 0, awal.length);
            if (dibaca >= 3 && (awal[0] & 0xFF) == 0xFF && (awal[1] & 0xFF) == 0xD8 && (awal[2] & 0xFF) == 0xFF) {
                return Optional.of("image/jpeg");
            }
            byte[] png = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
            if (dibaca == 8 && Arrays.equals(awal, png)) return Optional.of("image/png");
            return Optional.empty();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static String namaAsli(String nama) {
        if (nama == null || nama.isBlank()) return "foto";
        String dasar = nama.replace('\\', '/');
        dasar = dasar.substring(dasar.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "");
        return dasar.length() > 150 ? dasar.substring(dasar.length() - 150) : dasar;
    }
}
