package com.kelompok5.pelaporanfasilitas.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.kelompok5.pelaporanfasilitas.model.*;
import com.kelompok5.pelaporanfasilitas.security.AkunMasuk;
import com.kelompok5.pelaporanfasilitas.service.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/petugas")
public class PetugasController {
    private final IdentitasMasuk identitas;
    private final SistemPelaporan sistem;
    private final PantauanPelaporan pantauan;
    private final PenyimpananBerkas penyimpanan;

    public PetugasController(IdentitasMasuk identitas, SistemPelaporan sistem, PantauanPelaporan pantauan,
                             PenyimpananBerkas penyimpanan) {
        this.identitas = identitas;
        this.sistem = sistem;
        this.pantauan = pantauan;
        this.penyimpanan = penyimpanan;
    }

    @GetMapping
    String dashboard(@AuthenticationPrincipal AkunMasuk masuk, Model model) {
        Petugas akun = identitas.petugas(masuk, model);
        model.addAttribute("ringkasan", pantauan.ringkasanPetugas(akun.getId()));
        return "petugas/index";
    }

    @GetMapping("/tugas")
    String daftarTugas(@RequestParam(required = false) StatusTugas status,
                       @RequestParam(defaultValue = "false") boolean perluDiperiksa,
                       @RequestParam(defaultValue = "false") boolean aktif,
                       @AuthenticationPrincipal AkunMasuk masuk, Model model) {
        Petugas akun = identitas.petugas(masuk, model);
        var daftar = pantauan.tugasPetugas(akun.getId()).stream()
                .filter(t -> !aktif || t.getStatus() != StatusTugas.SELESAI)
                .filter(t -> !perluDiperiksa || (t.getStatus() != StatusTugas.SELESAI && !t.getKasus().isSudahDiperiksa()));
        model.addAttribute("daftarTugas", daftar.filter(t -> status == null || t.getStatus() == status).toList());
        model.addAttribute("ringkasan", pantauan.ringkasanPetugas(akun.getId()));
        model.addAttribute("statusAktif", status);
        model.addAttribute("hanyaBelumDiperiksa", perluDiperiksa);
        model.addAttribute("hanyaAktif", aktif);
        return "petugas/tugas";
    }

    @GetMapping("/tugas/{id}")
    String detailTugas(@PathVariable long id, @AuthenticationPrincipal AkunMasuk masuk, Model model) {
        Petugas akun = identitas.petugas(masuk, model);
        var tugas = pantauan.detailTugas(id, akun.getId());
        model.addAttribute("tugas", tugas);
        model.addAttribute("kasus", tugas.getKasus());
        model.addAttribute("laporan", tugas.getKasus().getLaporanUtama());
        model.addAttribute("tingkatGangguan", Gangguan.values());
        if (!model.containsAttribute("pemeriksaan")) {
            model.addAttribute("pemeriksaan", new PemeriksaanForm(null, null, null, ""));
        }
        return "petugas/detail";
    }

    @PostMapping("/tugas/{id}/progres")
    String progres(@PathVariable long id, @RequestParam(defaultValue = "") String catatan,
                   @AuthenticationPrincipal AkunMasuk masuk, Model model, RedirectAttributes flash) {
        Petugas akun = identitas.petugas(masuk, model);
        return ubahTugas(id, flash, "Catatan perkembangan tersimpan.",
                () -> sistem.perbaruiProgres(id, akun.getId(), catatan));
    }

    /** "Tandai selesai": catatan penanganan dan foto hasil perbaikan wajib. */
    @PostMapping("/tugas/{id}/selesai")
    String selesai(@PathVariable long id, @RequestParam(defaultValue = "") String catatan,
                   @RequestParam(value = "foto", required = false) MultipartFile foto,
                   @AuthenticationPrincipal AkunMasuk masuk, Model model, RedirectAttributes flash) {
        Petugas akun = identitas.petugas(masuk, model);
        return ubahTugas(id, flash, "Tugas ditandai selesai. Pelapor bisa melihat hasilnya.", () -> {
            var pekerjaan = pantauan.detailTugas(id, akun.getId());
            if (pekerjaan.getStatus() != StatusTugas.DIKERJAKAN || !pekerjaan.getKasus().isSudahDiperiksa()) {
                throw new IllegalStateException("Kasus harus diperiksa dan sedang diperbaiki sebelum ditutup.");
            }
            if (catatan.isBlank() || catatan.length() > 1000) {
                throw new IllegalArgumentException("Isi catatan perbaikan, maksimal 1000 karakter.");
            }
            Berkas berkas = penyimpanan.simpan(foto)
                    .orElseThrow(() -> new IllegalArgumentException("Unggah foto hasil perbaikan sebelum menandai selesai."));
            sistem.selesaikanTugas(id, akun.getId(), catatan, berkas);
        });
    }

    @PostMapping("/tugas/{id}/mulai")
    String mulai(@PathVariable long id, @AuthenticationPrincipal AkunMasuk masuk, Model model, RedirectAttributes flash) {
        Petugas akun = identitas.petugas(masuk, model);
        return ubahTugas(id, flash, "Perbaikan dimulai.", () -> sistem.mulaiTugas(id, akun.getId()));
    }

    @PostMapping("/tugas/{id}/pemeriksaan")
    String periksa(@PathVariable long id, @Valid @ModelAttribute("pemeriksaan") PemeriksaanForm pemeriksaan,
                   BindingResult hasil, @AuthenticationPrincipal AkunMasuk masuk, Model model, RedirectAttributes flash) {
        Petugas akun = identitas.petugas(masuk, model);
        if (hasil.hasErrors()) return detailTugas(id, masuk, model);
        try {
            sistem.periksaTugas(id, akun.getId(), pemeriksaan.bahayaKritis(), pemeriksaan.gangguan(),
                    pemeriksaan.unitRusak(), pemeriksaan.catatan());
            flash.addFlashAttribute("sukses", "Hasil pemeriksaan dan prioritas tersimpan. Pilih langkah berikutnya.");
            return "redirect:/petugas/tugas/" + id;
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("kesalahan", e.getMessage());
            return detailTugas(id, masuk, model);
        }
    }

    @PostMapping("/tugas/{id}/tindak-lanjut")
    String tindakLanjut(@PathVariable long id, @RequestParam(defaultValue = "") String alasan,
                        @RequestParam(defaultValue = "") String kebutuhan, @AuthenticationPrincipal AkunMasuk masuk,
                        Model model, RedirectAttributes flash) {
        Petugas akun = identitas.petugas(masuk, model);
        try {
            sistem.ajukanTindakLanjut(id, akun.getId(), alasan, kebutuhan);
            flash.addFlashAttribute("sukses", "Pengajuan dikirim. Tunggu keputusan admin.");
            return "redirect:/petugas/tugas/" + id;
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("kesalahan", e.getMessage());
            model.addAttribute("alasanDraf", alasan);
            model.addAttribute("kebutuhanDraf", kebutuhan);
            return detailTugas(id, masuk, model);
        }
    }

    @GetMapping("/verifikasi")
    String antreanLama() {
        return "redirect:/petugas/tugas?perluDiperiksa=true";
    }

    @GetMapping("/kasus/{id}/verifikasi")
    String pemeriksaanLama(@PathVariable long id, @AuthenticationPrincipal AkunMasuk masuk, Model model) {
        Petugas akun = identitas.petugas(masuk, model);
        return pantauan.tugasKasus(id).filter(t -> t.getPetugas().getId().equals(akun.getId()))
                .map(t -> "redirect:/petugas/tugas/" + t.getId()).orElse("redirect:/petugas/tugas");
    }

    private String ubahTugas(long id, RedirectAttributes flash, String sukses, Runnable aksi) {
        try {
            aksi.run();
            flash.addFlashAttribute("sukses", sukses);
            flash.addFlashAttribute("berubah", true);
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("kesalahan", e.getMessage());
        }
        return "redirect:/petugas/tugas/" + id;
    }

    public record PemeriksaanForm(
            @NotNull(message = "Pilih hasil pemeriksaan keselamatan.") Boolean bahayaKritis,
            @NotNull(message = "Pilih tingkat gangguan.") Gangguan gangguan,
            @NotNull(message = "Isi jumlah unit rusak.") @Min(value = 1, message = "Jumlah unit rusak minimal 1.") Integer unitRusak,
            @Size(max = 1000, message = "Catatan maksimal 1000 karakter.") String catatan) {}
}
