package com.kelompok5.pelaporanfasilitas.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.kelompok5.pelaporanfasilitas.model.*;
import com.kelompok5.pelaporanfasilitas.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/petugas")
public class PetugasController {
    private final IdentitasDemo identitas;
    private final SistemPelaporan sistem;
    private final PantauanPelaporan pantauan;

    public PetugasController(IdentitasDemo identitas, SistemPelaporan sistem, PantauanPelaporan pantauan) {
        this.identitas = identitas;
        this.sistem = sistem;
        this.pantauan = pantauan;
    }

    @GetMapping
    String dashboard(HttpSession session, Model model) {
        Petugas akun = identitas.petugas(session, model);
        var verifikasi = pantauan.daftarKasus("", StatusKasus.MENUNGGU_VERIFIKASI, null);
        var tugas = pantauan.tugasPetugas(akun.getId());
        model.addAttribute("jumlahVerifikasi", verifikasi.size());
        model.addAttribute("aktif", tugas.stream().filter(t -> t.getStatus() != StatusTugas.SELESAI).count());
        model.addAttribute("proses", tugas.stream().filter(t -> t.getStatus() == StatusTugas.DIKERJAKAN).count());
        model.addAttribute("selesai", tugas.stream().filter(t -> t.getStatus() == StatusTugas.SELESAI).count());
        model.addAttribute("daftarVerifikasi", verifikasi.stream().limit(4).toList());
        model.addAttribute("daftarTugas", tugas.stream().limit(4).toList());
        return "petugas/index";
    }

    @PostMapping("/identitas")
    String pilihIdentitas(@RequestParam long id, HttpSession session) {
        identitas.pilihPetugas(id, session);
        return "redirect:/petugas";
    }

    @GetMapping("/verifikasi")
    String daftarVerifikasi(HttpSession session, Model model) {
        identitas.petugas(session, model);
        model.addAttribute("daftarVerifikasi", pantauan.daftarKasus("", StatusKasus.MENUNGGU_VERIFIKASI, null));
        return "petugas/verifikasi";
    }

    @GetMapping("/kasus/{id}/verifikasi")
    String formVerifikasi(@PathVariable long id, HttpSession session, Model model) {
        Petugas akun = identitas.petugas(session, model);
        model.addAttribute("form", new VerifikasiForm(akun.getId(), null, null, null, ""));
        return siapkanVerifikasi(id, model);
    }

    @PostMapping("/kasus/{id}/verifikasi")
    String verifikasi(@PathVariable long id, @Valid @ModelAttribute("form") VerifikasiForm form, BindingResult hasil,
                      HttpSession session, Model model, RedirectAttributes flash) {
        Petugas akun = identitas.petugas(session, model);
        if (hasil.hasErrors()) return siapkanVerifikasi(id, model);
        try {
            identitas.pastikanSama(akun, form.petugasId());
            sistem.verifikasi(id, akun.getId(), form.bahayaKritis(), form.gangguan(), form.unitRusak(), form.catatan());
            flash.addFlashAttribute("sukses", "Verifikasi disimpan. Kasus siap ditugaskan oleh admin.");
            return "redirect:/petugas/verifikasi";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("kesalahan", e.getMessage());
            return siapkanVerifikasi(id, model);
        }
    }

    @GetMapping("/tugas")
    String daftarTugas(HttpSession session, Model model) {
        Petugas akun = identitas.petugas(session, model);
        model.addAttribute("daftarTugas", pantauan.tugasPetugas(akun.getId()));
        return "petugas/tugas";
    }

    @GetMapping("/tugas/{id}")
    String detailTugas(@PathVariable long id, HttpSession session, Model model) {
        Petugas akun = identitas.petugas(session, model);
        var tugas = pantauan.detailTugas(id, akun.getId());
        model.addAttribute("tugas", tugas);
        model.addAttribute("kasus", tugas.getKasus());
        return "petugas/detail";
    }

    @PostMapping("/tugas/{id}/mulai")
    String mulai(@PathVariable long id, HttpSession session, Model model, RedirectAttributes flash) {
        Petugas akun = identitas.petugas(session, model);
        return ubahTugas(id, flash, "Perbaikan dimulai.", () -> sistem.mulaiTugas(id, akun.getId()));
    }

    @PostMapping("/tugas/{id}/progres")
    String progres(@PathVariable long id, @RequestParam(defaultValue = "") String catatan,
                   HttpSession session, Model model, RedirectAttributes flash) {
        Petugas akun = identitas.petugas(session, model);
        return ubahTugas(id, flash, "Catatan progres disimpan.", () -> sistem.catatProgres(id, akun.getId(), catatan));
    }

    @PostMapping("/tugas/{id}/selesai")
    String selesai(@PathVariable long id, @RequestParam(defaultValue = "") String catatan,
                   HttpSession session, Model model, RedirectAttributes flash) {
        Petugas akun = identitas.petugas(session, model);
        return ubahTugas(id, flash, "Perbaikan selesai. Status kasus dan kondisi fasilitas telah diperbarui.",
                () -> sistem.selesaikanTugas(id, akun.getId(), catatan));
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
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("kesalahan", e.getMessage());
        }
        return "redirect:/petugas/tugas/" + id;
    }

    public record VerifikasiForm(
            @NotNull(message = "Pilih identitas petugas.") Long petugasId,
            @NotNull(message = "Pilih hasil pemeriksaan keselamatan.") Boolean bahayaKritis,
            @NotNull(message = "Pilih tingkat gangguan.") Gangguan gangguan,
            @NotNull(message = "Isi jumlah unit rusak.") @Min(value = 1, message = "Jumlah unit rusak minimal 1.") Integer unitRusak,
            @Size(max = 1000, message = "Catatan maksimal 1000 karakter.") String catatan) {}
}
