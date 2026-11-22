package com.kelompok5.pelaporanfasilitas.controller;

import com.kelompok5.pelaporanfasilitas.exception.DataTidakDitemukan;
import com.kelompok5.pelaporanfasilitas.model.*;
import com.kelompok5.pelaporanfasilitas.repository.PenggunaRepository;
import com.kelompok5.pelaporanfasilitas.security.AkunMasuk;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/** Memuat entitas pengguna dari akun yang sedang masuk (menggantikan pemilih akun demo). */
@Component
public class IdentitasMasuk {
    private final PenggunaRepository pengguna;

    public IdentitasMasuk(PenggunaRepository pengguna) { this.pengguna = pengguna; }

    public Mahasiswa mahasiswa(AkunMasuk akun, Model model) { return muat(akun, Mahasiswa.class, model); }

    public Petugas petugas(AkunMasuk akun, Model model) { return muat(akun, Petugas.class, model); }

    public void pastikanSama(Pengguna aktif, Long idForm) {
        if (!aktif.getId().equals(idForm)) {
            throw new IllegalStateException("Akun yang masuk berubah sejak formulir dibuka. Periksa isian, lalu kirim lagi.");
        }
    }

    private <T extends Pengguna> T muat(AkunMasuk akun, Class<T> tipe, Model model) {
        if (akun == null) throw new DataTidakDitemukan("Sesi masuk tidak ditemukan. Masuk kembali untuk melanjutkan.");
        T entitas = pengguna.findById(akun.id()).filter(tipe::isInstance).map(tipe::cast)
                .orElseThrow(() -> new DataTidakDitemukan("Akun ini tidak lagi tersedia. Masuk kembali untuk melanjutkan."));
        model.addAttribute("akun", entitas);
        return entitas;
    }
}
