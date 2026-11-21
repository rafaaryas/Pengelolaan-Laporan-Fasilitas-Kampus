package com.kelompok5.pelaporanfasilitas.controller;

import java.time.LocalTime;
import com.kelompok5.pelaporanfasilitas.model.*;
import com.kelompok5.pelaporanfasilitas.repository.PenggunaRepository;
import com.kelompok5.pelaporanfasilitas.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final SistemPelaporan sistem;
    private final PantauanPelaporan pantauan;
    private final PenggunaRepository pengguna;

    public AdminController(SistemPelaporan sistem, PantauanPelaporan pantauan, PenggunaRepository pengguna) {
        this.sistem = sistem;
        this.pantauan = pantauan;
        this.pengguna = pengguna;
    }

    @GetMapping
    String dashboard(Model model) {
        model.addAttribute("ringkasan", pantauan.rangkumanAdmin());
        return "admin/index";
    }

    @GetMapping("/kasus")
    String daftar(@RequestParam(defaultValue = "") String q, @RequestParam(required = false) StatusKasus status,
                  @RequestParam(required = false) Prioritas prioritas, Model model) {
        model.addAttribute("kasus", pantauan.daftarKasus(q, status, prioritas));
        model.addAttribute("q", q);
        model.addAttribute("statusFilter", status);
        model.addAttribute("prioritasFilter", prioritas);
        model.addAttribute("semuaStatus", StatusKasus.values());
        model.addAttribute("semuaPrioritas", Prioritas.values());
        return "admin/kasus";
    }

    @GetMapping("/kasus/{id}")
    String detail(@PathVariable long id, Model model) {
        var detail = pantauan.detailKasus(id);
        KasusKerusakan kasus = detail.kasus();
        model.addAttribute("kasus", kasus);
        model.addAttribute("tugas", detail.tugas());
        if (kasus.getStatus() == StatusKasus.MENUNGGU_PENUGASAN) {
            LocalTime waktu = sistem.waktuKampus();
            model.addAttribute("jadwal", pengguna.petugasDemo().stream()
                    .filter(p -> p.mampuMenangani(kasus.getFasilitas()))
                    .map(p -> new Jadwal(p, p.tersediaPada(waktu))).toList());
        }
        return "admin/detail";
    }

    @GetMapping("/petugas")
    String petugas(Model model) {
        var daftar = pengguna.petugasDemo();
        LocalTime waktu = sistem.waktuKampus();
        model.addAttribute("jadwal", daftar.stream().map(p -> new Jadwal(p, p.tersediaPada(waktu))).toList());
        model.addAttribute("tersedia", daftar.stream().filter(p -> p.tersediaPada(waktu)).count());
        model.addAttribute("sibuk", daftar.stream().filter(Petugas::isSibuk).count());
        return "admin/petugas";
    }

    @PostMapping("/kasus/{id}/penugasan")
    String tugaskan(@PathVariable long id, @RequestParam long petugasId, @RequestParam(defaultValue = "") String catatan,
                    RedirectAttributes flash) {
        try {
            sistem.tugaskanPetugas(id, petugasId, catatan);
            flash.addFlashAttribute("sukses", "Petugas berhasil ditugaskan.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("kesalahan", e.getMessage());
        }
        return "redirect:/admin/kasus/" + id;
    }

    @PostMapping("/kasus/{id}/laporan/{laporanId}/pisahkan")
    String pisahkan(@PathVariable long id, @PathVariable long laporanId, RedirectAttributes flash) {
        try {
            long baru = sistem.pisahkanLaporan(id, laporanId);
            flash.addFlashAttribute("sukses", "Laporan dipindahkan ke kasus baru untuk diverifikasi secara terpisah.");
            return "redirect:/admin/kasus/" + baru;
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("kesalahan", e.getMessage());
            return "redirect:/admin/kasus/" + id;
        }
    }

    public record Jadwal(Petugas petugas, boolean tersedia) {
        public String status() {
            if (tersedia) return "Tersedia";
            return petugas.isSibuk() ? "Bertugas" : "Di luar jam kerja";
        }

        public String jam() {
            if (petugas.getJamMulai().equals(petugas.getJamSelesai())) return "24 jam";
            return petugas.getJamMulai() + "–" + petugas.getJamSelesai();
        }
    }
}
