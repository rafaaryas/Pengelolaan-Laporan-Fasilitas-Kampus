package com.kelompok5.pelaporanfasilitas.security;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.kelompok5.pelaporanfasilitas.model.Mahasiswa;
import com.kelompok5.pelaporanfasilitas.model.Pengguna;
import com.kelompok5.pelaporanfasilitas.repository.PenggunaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Menentukan ruang kerja dari email atau NIM.
 * Peran admin dan petugas hanya diperoleh dari data di tabel pengguna, tidak dari teks atau domain email.
 * Masukan yang valid tetapi belum terdaftar selalu menjadi akun mahasiswa.
 */
@Service
public class PenentuPeran {
    static final String PESAN_KOSONG = "Isi email kampus atau NIM untuk masuk.";
    static final String PESAN_TIDAK_VALID =
            "Format belum sesuai. Gunakan email, misalnya nama@students.um.ac.id, atau NIM 12 digit angka.";
    private static final Pattern EMAIL = Pattern.compile("^[a-z0-9._%+-]{1,64}@[a-z0-9-]+(\\.[a-z0-9-]+)*\\.[a-z]{2,24}$");
    private static final Pattern NIM = Pattern.compile("^\\d{12}$");
    private static final String DOMAIN_NIM = "@mahasiswa.invalid";

    private final PenggunaRepository pengguna;

    public PenentuPeran(PenggunaRepository pengguna) { this.pengguna = pengguna; }

    public AkunMasuk masuk(String masukan) {
        String identitas = normalisasi(masukan);
        if (identitas.isEmpty()) throw new IllegalArgumentException(PESAN_KOSONG);
        if (NIM.matcher(identitas).matches()) {
            return AkunMasuk.dari(pengguna.cariNim(identitas).map(Pengguna.class::cast)
                    .orElseGet(() -> daftarkan(namaDariNim(identitas), "nim." + identitas + DOMAIN_NIM, identitas)), identitas);
        }
        if (identitas.length() <= 150 && EMAIL.matcher(identitas).matches()) {
            return AkunMasuk.dari(pengguna.cariEmail(identitas)
                    .orElseGet(() -> daftarkan(namaDariEmail(identitas), identitas, null)), identitas);
        }
        throw new IllegalArgumentException(PESAN_TIDAK_VALID);
    }

    static String normalisasi(String masukan) {
        if (masukan == null) return "";
        return masukan.strip().replace(" ", "").toLowerCase(Locale.ROOT);
    }

    private Pengguna daftarkan(String nama, String email, String nim) {
        try {
            return pengguna.saveAndFlush(Mahasiswa.daftarBaru(nama, email, nim));
        } catch (DataIntegrityViolationException bentrok) {
            // Dua permintaan masuk bersamaan dengan identitas yang sama: pakai akun yang sudah tersimpan.
            Optional<? extends Pengguna> ada = nim != null ? pengguna.cariNim(nim) : pengguna.cariEmail(email);
            return ada.orElseThrow(() -> bentrok);
        }
    }

    private static String namaDariEmail(String email) {
        String lokal = email.substring(0, email.indexOf('@'));
        String nama = Arrays.stream(lokal.split("[._+-]+"))
                .filter(bagian -> !bagian.isBlank() && !bagian.chars().allMatch(Character::isDigit))
                .map(bagian -> Character.toUpperCase(bagian.charAt(0)) + bagian.substring(1))
                .collect(Collectors.joining(" "));
        nama = nama.isBlank() ? "Mahasiswa " + lokal : nama;
        return nama.length() > 100 ? nama.substring(0, 100) : nama;
    }

    private static String namaDariNim(String nim) { return "Mahasiswa " + nim; }
}
