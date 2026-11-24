package com.kelompok5.pelaporanfasilitas.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import com.kelompok5.pelaporanfasilitas.model.Berkas;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Menyimpan foto bukti dan foto hasil perbaikan di folder unggahan (di luar folder statis),
 * sehingga hanya bisa dibuka lewat controller yang memeriksa hak akses.
 */
@Service
public class PenyimpananBerkas {
    public static final long UKURAN_MAKS = 5L * 1024 * 1024;
    static final String PESAN_FORMAT = "Foto maksimal 5 MB, format JPG atau PNG.";
    private static final String AWALAN_CONTOH = "contoh/";

    private final Path folder;

    public PenyimpananBerkas(@Value("${umpeduli.unggahan.folder:data/unggahan}") String folder) {
        this.folder = Path.of(folder).toAbsolutePath().normalize();
    }

    /** Memvalidasi lalu menyimpan foto. Mengembalikan kosong bila tidak ada berkas yang dipilih. */
    public Optional<Berkas> simpan(MultipartFile foto) {
        if (foto == null || foto.isEmpty()) return Optional.empty();
        if (foto.getSize() > UKURAN_MAKS) throw new IllegalArgumentException(PESAN_FORMAT);
        String ekstensi = jenis(foto).orElseThrow(() -> new IllegalArgumentException(PESAN_FORMAT));
        String nama = UUID.randomUUID() + ekstensi;
        try (InputStream isi = foto.getInputStream()) {
            Files.createDirectories(folder);
            Files.copy(isi, folder.resolve(nama), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Foto gagal disimpan. Coba unggah lagi.", e);
        }
        return Optional.of(new Berkas(nama, namaAsli(foto.getOriginalFilename()), foto.getSize()));
    }

    /** Berkas contoh untuk data demo ada di classpath, unggahan pengguna di folder unggahan. */
    public Optional<Resource> buka(Berkas berkas) {
        if (berkas == null || !berkas.ada()) return Optional.empty();
        String simpan = berkas.getSimpan();
        if (simpan.startsWith(AWALAN_CONTOH)) {
            Resource contoh = new ClassPathResource("demo-unggahan/" + simpan.substring(AWALAN_CONTOH.length()));
            return contoh.exists() ? Optional.of(contoh) : Optional.empty();
        }
        Path jalur = folder.resolve(simpan).normalize();
        if (!jalur.startsWith(folder) || !Files.isRegularFile(jalur)) return Optional.empty();
        return Optional.of(new PathResource(jalur));
    }

    public static String tipeKonten(Berkas berkas) {
        return berkas.getSimpan().endsWith(".png") ? "image/png" : "image/jpeg";
    }

    /** Tipe ditentukan dari tanda tangan biner berkas, bukan dari nama atau header browser. */
    private static Optional<String> jenis(MultipartFile foto) {
        byte[] awal = new byte[8];
        try (InputStream isi = foto.getInputStream()) {
            int dibaca = isi.readNBytes(awal, 0, awal.length);
            if (dibaca >= 3 && (awal[0] & 0xFF) == 0xFF && (awal[1] & 0xFF) == 0xD8 && (awal[2] & 0xFF) == 0xFF) {
                return Optional.of(".jpg");
            }
            byte[] png = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
            if (dibaca == 8 && Arrays.equals(awal, png)) return Optional.of(".png");
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
