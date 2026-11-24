package com.kelompok5.pelaporanfasilitas.controller;

import java.util.List;
import com.kelompok5.pelaporanfasilitas.model.Mahasiswa;
import com.kelompok5.pelaporanfasilitas.model.Petugas;
import com.kelompok5.pelaporanfasilitas.repository.PenggunaRepository;
import com.kelompok5.pelaporanfasilitas.security.AkunMasuk;
import com.kelompok5.pelaporanfasilitas.service.PantauanPelaporan;
import com.kelompok5.pelaporanfasilitas.service.PantauanPelaporan.Notifikasi;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Data shell ruang kerja: akun yang masuk, profil sidebar, badge laporan, dan notifikasi lonceng. */
@ControllerAdvice(assignableTypes = {MahasiswaController.class, PetugasController.class, AdminController.class,
        LoginController.class, PageController.class})
public class AkunGlobal {
    private final PenggunaRepository pengguna;
    private final PantauanPelaporan pantauan;

    public AkunGlobal(PenggunaRepository pengguna, PantauanPelaporan pantauan) {
        this.pengguna = pengguna;
        this.pantauan = pantauan;
    }

    @ModelAttribute
    void shell(@AuthenticationPrincipal AkunMasuk akun, Model model) {
        model.addAttribute("akunMasuk", akun);
        if (akun == null) return;
        var entitas = pengguna.findById(akun.id()).orElse(null);
        String keterangan = switch (entitas) {
            case Mahasiswa m -> m.getKeterangan();
            case Petugas p -> p.getKeterangan();
            case null -> "";
            default -> "Pengelola fasilitas";
        };
        String peran = switch (akun.peran()) {
            case MAHASISWA -> "Civitas akademika";
            case PETUGAS -> "Petugas";
            case ADMIN -> "Admin";
        };
        model.addAttribute("profil", new Profil(akun.nama(), keterangan, peran));
        List<Notifikasi> notifikasi = switch (akun.peran()) {
            case MAHASISWA -> pantauan.notifikasiMahasiswa(akun.id());
            case PETUGAS -> pantauan.notifikasiPetugas(akun.id());
            case ADMIN -> pantauan.notifikasiAdmin();
        };
        model.addAttribute("notifikasi", notifikasi);
        if (akun.peran() == AkunMasuk.Peran.ADMIN) model.addAttribute("badgeLaporan", pantauan.jumlahMenungguVerifikasi());
    }

    public record Profil(String nama, String keterangan, String peran) {}
}
