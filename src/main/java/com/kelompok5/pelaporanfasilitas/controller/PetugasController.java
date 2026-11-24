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
        // Layar menulis "Pak Budi"; sapaan bergender tidak ditebak dari nama, jadi dipakai nama depan.
        model.addAttribute("sapaanNama", akun.getNama().split(" ")[0]);
        return "petugas/index";
    }

    @GetMapping("/tugas")
    String daftarTugas(@AuthenticationPrincipal AkunMasuk masuk, Model model) {
        Petugas akun = identitas.petugas(masuk, model);
        model.addAttribute("daftarTugas", pantauan.tugasPetugas(akun.getId()));
        return "petugas/tugas";
    }

    @GetMapping("/tugas/{id}")
    String detailTugas(@PathVariable long id, @AuthenticationPrincipal AkunMasuk masuk, Model model) {
        Petugas akun = identitas.petugas(masuk, model);
        var tugas = pantauan.detailTugas(id, akun.getId());
        model.addAttribute("tugas", tugas);
        model.addAttribute("kasus", tugas.getKasus());
        model.addAttribute("laporan", tugas.getKasus().getLaporanUtama());
        return "petugas/detail";
    }

    /** "Simpan pembaruan": memulai perbaikan bila belum dimulai dan menyimpan catatan penanganan. */
    @PostMapping("/tugas/{id}/progres")
    String progres(@PathVariable long id, @RequestParam(defaultValue = "") String catatan,
                   @RequestParam(defaultValue = "DIKERJAKAN") String status,
                   @RequestParam(value = "foto", required = false) MultipartFile foto,
                   @AuthenticationPrincipal AkunMasuk masuk, Model model, RedirectAttributes flash) {
        if ("SELESAI".equals(status)) return selesai(id, catatan, foto, masuk, model, flash);
        Petugas akun = identitas.petugas(masuk, model);
        return ubahTugas(id, flash, "Pembaruan tersimpan. Status laporan: Sedang Diperbaiki.",
                () -> sistem.perbaruiProgres(id, akun.getId(), catatan));
    }

    /** "Tandai selesai": catatan penanganan dan foto hasil perbaikan wajib. */
    @PostMapping("/tugas/{id}/selesai")
    String selesai(@PathVariable long id, @RequestParam(defaultValue = "") String catatan,
                   @RequestParam(value = "foto", required = false) MultipartFile foto,
                   @AuthenticationPrincipal AkunMasuk masuk, Model model, RedirectAttributes flash) {
        Petugas akun = identitas.petugas(masuk, model);
        return ubahTugas(id, flash, "Tugas ditandai selesai. Pelapor bisa melihat hasilnya.", () -> {
            if (catatan.isBlank()) throw new IllegalArgumentException("Isi catatan penanganan sebelum menandai selesai.");
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

    // ----- Pemeriksaan di lokasi (alur lama, tidak tampil di menu) -----

    @GetMapping("/verifikasi")
    String daftarVerifikasi(@AuthenticationPrincipal AkunMasuk masuk, Model model) {
        identitas.petugas(masuk, model);
        model.addAttribute("daftarVerifikasi", pantauan.daftarKasus("", StatusKasus.MENUNGGU_VERIFIKASI, null));
        return "petugas/verifikasi";
    }

    @GetMapping("/kasus/{id}/verifikasi")
    String formVerifikasi(@PathVariable long id, @AuthenticationPrincipal AkunMasuk masuk, Model model) {
        Petugas akun = identitas.petugas(masuk, model);
        model.addAttribute("form", new VerifikasiForm(akun.getId(), null, null, null, ""));
        return siapkanVerifikasi(id, model);
    }

    @PostMapping("/kasus/{id}/verifikasi")
    String verifikasi(@PathVariable long id, @Valid @ModelAttribute("form") VerifikasiForm form, BindingResult hasil,
                      @AuthenticationPrincipal AkunMasuk masuk, Model model, RedirectAttributes flash) {
        Petugas akun = identitas.petugas(masuk, model);
        if (hasil.hasErrors()) return siapkanVerifikasi(id, model);
        try {
            identitas.pastikanSama(akun, form.petugasId());
            sistem.verifikasi(id, akun.getId(), form.bahayaKritis(), form.gangguan(), form.unitRusak(), form.catatan());
            flash.addFlashAttribute("sukses", "Hasil pemeriksaan tersimpan. Laporan siap ditugaskan admin.");
            return "redirect:/petugas/verifikasi";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("kesalahan", e.getMessage());
            return siapkanVerifikasi(id, model);
        }
    }

    private String siapkanVerifikasi(long id, Model model) {
        model.addAttribute("kasus", pantauan.detailKasus(id).kasus());
        model.addAttribute("tingkatGangguan", Gangguan.values());
        return "petugas/periksa";
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

    public record VerifikasiForm(
            @NotNull(message = "Sesi petugas tidak ditemukan.") Long petugasId,
            @NotNull(message = "Pilih hasil pemeriksaan keselamatan.") Boolean bahayaKritis,
            @NotNull(message = "Pilih tingkat gangguan.") Gangguan gangguan,
            @NotNull(message = "Isi jumlah unit rusak.") @Min(value = 1, message = "Jumlah unit rusak minimal 1.") Integer unitRusak,
            @Size(max = 1000, message = "Catatan maksimal 1000 karakter.") String catatan) {}
}
