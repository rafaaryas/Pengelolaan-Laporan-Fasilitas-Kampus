package com.kelompok5.pelaporanfasilitas.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.kelompok5.pelaporanfasilitas.model.*;
import com.kelompok5.pelaporanfasilitas.security.AkunMasuk;
import com.kelompok5.pelaporanfasilitas.service.*;
import com.kelompok5.pelaporanfasilitas.service.PantauanPelaporan.Halaman;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/mahasiswa")
public class MahasiswaController {
    static final int PER_HALAMAN = 4;

    private final IdentitasMasuk identitas;
    private final SistemPelaporan sistem;
    private final PantauanPelaporan pantauan;
    private final PenyimpananBerkas penyimpanan;

    public MahasiswaController(IdentitasMasuk identitas, SistemPelaporan sistem, PantauanPelaporan pantauan,
                               PenyimpananBerkas penyimpanan) {
        this.identitas = identitas;
        this.sistem = sistem;
        this.pantauan = pantauan;
        this.penyimpanan = penyimpanan;
    }

    @GetMapping
    String dashboard(@AuthenticationPrincipal AkunMasuk masuk, Model model) {
        Mahasiswa akun = identitas.mahasiswa(masuk, model);
        var riwayat = pantauan.laporanMahasiswa(akun.getId(), "", null);
        model.addAttribute("hitung", pantauan.hitungMahasiswa(akun.getId(), ""));
        model.addAttribute("riwayat", riwayat.stream().limit(3).toList());
        model.addAttribute("semuaTahap", Tahap.values());
        return "mahasiswa/index";
    }

    @GetMapping("/laporan")
    String riwayat(@RequestParam(defaultValue = "") String q, @RequestParam(required = false) Tahap tahap,
                   @RequestParam(defaultValue = "1") int halaman, @AuthenticationPrincipal AkunMasuk masuk, Model model) {
        Mahasiswa akun = identitas.mahasiswa(masuk, model);
        var daftar = pantauan.laporanMahasiswa(akun.getId(), q, tahap);
        model.addAttribute("halaman", Halaman.dari(daftar, halaman, PER_HALAMAN));
        model.addAttribute("hitung", pantauan.hitungMahasiswa(akun.getId(), q));
        model.addAttribute("q", q);
        model.addAttribute("tahapAktif", tahap);
        model.addAttribute("semuaTahap", Tahap.values());
        return "mahasiswa/riwayat";
    }

    @GetMapping("/laporan/baru")
    String form(@AuthenticationPrincipal AkunMasuk masuk, Model model) {
        Mahasiswa akun = identitas.mahasiswa(masuk, model);
        model.addAttribute("form", new LaporanForm(akun.getId(), akun.getFakultas(), null, null, "", ""));
        return siapkanForm(model);
    }

    @PostMapping("/laporan")
    String buat(@Valid @ModelAttribute("form") LaporanForm form, BindingResult hasil,
                @RequestParam(value = "foto", required = false) MultipartFile foto,
                @AuthenticationPrincipal AkunMasuk masuk, Model model, RedirectAttributes flash) {
        Mahasiswa akun = identitas.mahasiswa(masuk, model);
        if (form.fakultas() != null && form.gedung() != null && !form.fakultas().isBlank() && !form.gedung().isBlank()
                && !KatalogLokasi.sah(form.fakultas(), form.gedung())) {
            hasil.rejectValue("gedung", "lokasi", "Pilih gedung yang ada di fakultas tersebut.");
        }
        if (hasil.hasErrors()) return siapkanForm(model);
        try {
            identitas.pastikanSama(akun, form.mahasiswaId());
            Berkas berkas = penyimpanan.simpan(foto).orElse(null);
            long id = sistem.laporkan(akun.getId(), new Lokasi(form.fakultas(), form.gedung(), form.lantai(), form.ruang()),
                    form.deskripsi(), berkas);
            flash.addFlashAttribute("sukses", "Laporan terkirim. Admin akan memverifikasinya.");
            return "redirect:/mahasiswa/laporan/" + id;
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Foto")) model.addAttribute("galatFoto", e.getMessage());
            else model.addAttribute("kesalahan", e.getMessage());
            return siapkanForm(model);
        } catch (IllegalStateException e) {
            model.addAttribute("kesalahan", e.getMessage());
            return siapkanForm(model);
        }
    }

    @GetMapping("/laporan/{id}")
    String detail(@PathVariable long id, @AuthenticationPrincipal AkunMasuk masuk, Model model) {
        Mahasiswa akun = identitas.mahasiswa(masuk, model);
        var detail = pantauan.detailLaporan(id, akun.getId());
        model.addAttribute("laporan", detail.laporan());
        model.addAttribute("tugas", detail.tugas());
        return "mahasiswa/detail";
    }

    private String siapkanForm(Model model) {
        model.addAttribute("katalog", KatalogLokasi.FAKULTAS);
        model.addAttribute("lantaiTertinggi", KatalogLokasi.LANTAI_TERTINGGI);
        return "mahasiswa/form";
    }

    public record LaporanForm(
            @NotNull(message = "Sesi mahasiswa tidak ditemukan.") Long mahasiswaId,
            @NotBlank(message = "Pilih fakultas.") String fakultas,
            @NotBlank(message = "Pilih gedung.") String gedung,
            @NotNull(message = "Pilih lantai.") @Min(value = 1, message = "Pilih lantai.") @Max(value = 5, message = "Pilih lantai.") Integer lantai,
            @Size(max = 40, message = "Nama ruang maksimal 40 karakter.") String ruang,
            @NotBlank(message = "Isi deskripsi kerusakan.") @Size(max = 1600, message = "Deskripsi maksimal 1600 karakter.") String deskripsi) {}
}
