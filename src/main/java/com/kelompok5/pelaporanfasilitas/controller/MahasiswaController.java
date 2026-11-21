package com.kelompok5.pelaporanfasilitas.controller;

import java.util.EnumMap;
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
@RequestMapping("/mahasiswa")
public class MahasiswaController {
    private final IdentitasDemo identitas;
    private final SistemPelaporan sistem;
    private final PantauanPelaporan pantauan;

    public MahasiswaController(IdentitasDemo identitas, SistemPelaporan sistem, PantauanPelaporan pantauan) {
        this.identitas = identitas;
        this.sistem = sistem;
        this.pantauan = pantauan;
    }

    @GetMapping
    String dashboard(HttpSession session, Model model) {
        Mahasiswa akun = identitas.mahasiswa(session, model);
        var riwayat = pantauan.laporanMahasiswa(akun.getId(), "", null);
        var ringkasan = new EnumMap<StatusKasus, Long>(StatusKasus.class);
        for (StatusKasus status : StatusKasus.values()) ringkasan.put(status, 0L);
        riwayat.forEach(l -> ringkasan.merge(l.getKasus().getStatus(), 1L, Long::sum));
        model.addAttribute("total", riwayat.size());
        model.addAttribute("menunggu", ringkasan.get(StatusKasus.MENUNGGU_VERIFIKASI));
        model.addAttribute("terverifikasi", ringkasan.get(StatusKasus.MENUNGGU_PENUGASAN) + ringkasan.get(StatusKasus.DITUGASKAN));
        model.addAttribute("diperbaiki", ringkasan.get(StatusKasus.DIKERJAKAN));
        model.addAttribute("selesai", ringkasan.get(StatusKasus.SELESAI));
        model.addAttribute("riwayat", riwayat.stream().limit(3).toList());
        return "mahasiswa/index";
    }

    @PostMapping("/identitas")
    String pilihIdentitas(@RequestParam long id, HttpSession session) {
        identitas.pilihMahasiswa(id, session);
        return "redirect:/mahasiswa";
    }

    @GetMapping("/laporan")
    String riwayat(@RequestParam(defaultValue = "") String q, @RequestParam(required = false) StatusKasus status,
                   HttpSession session, Model model) {
        Mahasiswa akun = identitas.mahasiswa(session, model);
        model.addAttribute("riwayat", pantauan.laporanMahasiswa(akun.getId(), q, status));
        model.addAttribute("q", q);
        model.addAttribute("statusFilter", status);
        model.addAttribute("semuaStatus", StatusKasus.values());
        return "mahasiswa/riwayat";
    }

    @GetMapping("/laporan/baru")
    String form(HttpSession session, Model model) {
        Mahasiswa akun = identitas.mahasiswa(session, model);
        model.addAttribute("form", new LaporanForm(akun.getId(), null, null, ""));
        return siapkanForm(model);
    }

    @PostMapping("/laporan")
    String buat(@Valid @ModelAttribute("form") LaporanForm form, BindingResult hasil, HttpSession session,
                Model model, RedirectAttributes flash) {
        Mahasiswa akun = identitas.mahasiswa(session, model);
        if (hasil.hasErrors()) return siapkanForm(model);
        try {
            identitas.pastikanSama(akun, form.mahasiswaId());
            long id = sistem.prosesLaporan(akun.getId(), form.fasilitasId(), form.jenisKerusakan(), form.deskripsi());
            flash.addFlashAttribute("sukses", "Laporan berhasil dikirim.");
            return "redirect:/mahasiswa/laporan/" + id;
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("kesalahan", e.getMessage());
            return siapkanForm(model);
        }
    }

    @GetMapping("/laporan/{id}")
    String detail(@PathVariable long id, HttpSession session, Model model) {
        Mahasiswa akun = identitas.mahasiswa(session, model);
        var detail = pantauan.detailLaporan(id, akun.getId());
        model.addAttribute("laporan", detail.laporan());
        model.addAttribute("tugas", detail.tugas());
        return "mahasiswa/detail";
    }

    private String siapkanForm(Model model) {
        model.addAttribute("fasilitas", pantauan.daftarFasilitas());
        model.addAttribute("jenis", JenisKerusakan.values());
        return "mahasiswa/form";
    }

    public record LaporanForm(
            @NotNull(message = "Pilih identitas mahasiswa.") Long mahasiswaId,
            @NotNull(message = "Pilih fasilitas.") Long fasilitasId,
            @NotNull(message = "Pilih jenis kerusakan.") JenisKerusakan jenisKerusakan,
            @NotBlank(message = "Isi deskripsi kerusakan.") @Size(max = 1600, message = "Deskripsi maksimal 1600 karakter.") String deskripsi) {}
}
