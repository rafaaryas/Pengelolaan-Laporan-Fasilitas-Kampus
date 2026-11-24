package com.kelompok5.pelaporanfasilitas.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import com.kelompok5.pelaporanfasilitas.model.*;
import com.kelompok5.pelaporanfasilitas.service.*;
import com.kelompok5.pelaporanfasilitas.service.PantauanPelaporan.SaringanLaporan;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {
    static final int PER_HALAMAN = 7;
    static final int PETUGAS_RINGKAS = 5;

    private final SistemPelaporan sistem;
    private final PantauanPelaporan pantauan;
    private final WaktuKampus waktu;

    public AdminController(SistemPelaporan sistem, PantauanPelaporan pantauan, WaktuKampus waktu) {
        this.sistem = sistem;
        this.pantauan = pantauan;
        this.waktu = waktu;
    }

    @GetMapping
    String dashboard(@RequestParam(defaultValue = "6") int rentang, Model model) {
        int bulan = rentang == 12 ? 12 : 6;
        model.addAttribute("ringkasan", pantauan.ringkasanAdmin(bulan));
        model.addAttribute("rentang", bulan);
        return "admin/index";
    }

    @GetMapping("/kasus")
    String daftar(@RequestParam(defaultValue = "") String q, @RequestParam(required = false) Tahap tahap,
                  @RequestParam(required = false) StatusKasus status, @RequestParam(defaultValue = "") String tanggal,
                  @RequestParam(required = false) Long petugas, @RequestParam(defaultValue = "1") int halaman,
                  jakarta.servlet.http.HttpServletRequest request, Model model) {
        Tahap aktif = tahap != null ? tahap : (status == null ? null : Tahap.dari(status));
        var saring = new SaringanLaporan(q, aktif, tanggal, petugas, halaman);
        model.addAttribute("halaman", pantauan.daftarLaporan(saring, PER_HALAMAN));
        model.addAttribute("hitung", pantauan.hitungLaporan(saring));
        model.addAttribute("q", q);
        model.addAttribute("tahapAktif", aktif);
        model.addAttribute("tanggal", tanggal);
        model.addAttribute("petugasFilter", petugas);
        model.addAttribute("semuaTahap", Tahap.values());
        model.addAttribute("kembali", request.getRequestURI() + (request.getQueryString() == null ? "" : "?" + request.getQueryString()));
        if (petugas != null) {
            pantauan.daftarPetugas("").stream().filter(b -> b.petugas().getId().equals(petugas)).findFirst()
                    .ifPresent(b -> model.addAttribute("petugasNama", b.petugas().getNama()));
        }
        return "admin/kasus";
    }

    @GetMapping("/kasus/ekspor")
    ResponseEntity<byte[]> ekspor(@RequestParam(defaultValue = "") String q, @RequestParam(required = false) Tahap tahap,
                                  @RequestParam(defaultValue = "") String tanggal, @RequestParam(required = false) Long petugas) {
        var daftar = pantauan.semuaLaporanTersaring(new SaringanLaporan(q, tahap, tanggal, petugas, 1));
        StringBuilder csv = new StringBuilder("﻿ID;Judul;Nama pelapor;Fakultas;Gedung;Lantai;Ruang;Tanggal;Status\r\n");
        for (LaporanKerusakan l : daftar) {
            var lokasi = l.getLokasi();
            csv.append(String.join(";", List.of(l.getKode(), sel(l.getJudul()), sel(l.getPelapor().getNama()), sel(l.getFakultas()),
                    sel(lokasi == null ? l.getGedungLantai() : lokasi.getGedung()), l.getLantai(),
                    sel(lokasi == null || lokasi.getRuang() == null ? "" : lokasi.getRuang()),
                    waktu.tanggalJam(l.getWaktuLaporan()), l.getTahap().getLabel()))).append("\r\n");
        }
        String nama = "laporan-um-peduli-" + LocalDate.parse(waktu.isoHariIni()) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nama).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Sel CSV aman: dikutip, dan diawali apostrof bila bisa dibaca sebagai rumus spreadsheet. */
    private static String sel(String nilai) {
        String isi = nilai == null ? "" : nilai;
        if (!isi.isEmpty() && "=+-@\t\r".indexOf(isi.charAt(0)) >= 0) isi = "'" + isi;
        return "\"" + isi.replace("\"", "\"\"") + "\"";
    }

    @GetMapping("/kasus/{id}")
    String detail(@PathVariable long id, @RequestParam(required = false) String modal,
                  @RequestParam(required = false) String kembali, Model model) {
        var detail = pantauan.detailKasus(id);
        model.addAttribute("kasus", detail.kasus());
        model.addAttribute("laporan", detail.kasus().getLaporanUtama());
        model.addAttribute("tugas", detail.tugas());
        model.addAttribute("petugasPilihan", pantauan.daftarPetugas(""));
        model.addAttribute("kembali", kembaliAman(kembali, "/admin/kasus"));
        return modal != null ? "admin/detail :: modal" : "admin/detail";
    }

    @PostMapping("/kasus/{id}/verifikasi")
    String verifikasi(@PathVariable long id, @RequestParam(required = false) Long petugasId,
                      @RequestParam(defaultValue = "TERVERIFIKASI") Tahap status, @RequestParam(defaultValue = "") String catatan,
                      @RequestParam(required = false) String kembali, RedirectAttributes flash) {
        String tujuan = kembaliAman(kembali, "/admin/kasus/" + id);
        if (status != Tahap.TERVERIFIKASI) {
            flash.addFlashAttribute("kesalahan", "Ubah status ke Terverifikasi untuk memverifikasi laporan.");
            return "redirect:" + tujuan;
        }
        try {
            sistem.verifikasiAdmin(id, petugasId, catatan);
            flash.addFlashAttribute("sukses", petugasId == null ? "Laporan terverifikasi." : "Laporan terverifikasi dan petugas ditugaskan.");
            flash.addFlashAttribute("berubahKasus", id);
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("kesalahan", e.getMessage());
        }
        return "redirect:" + tujuan;
    }

    @GetMapping("/petugas")
    String petugas(@RequestParam(defaultValue = "") String q, @RequestParam(required = false) String semua,
                   @RequestParam(required = false) Long kasus, Model model) {
        var daftar = pantauan.daftarPetugas(q);
        var seluruh = q.isBlank() ? daftar : pantauan.daftarPetugas("");
        model.addAttribute("daftar", semua != null ? daftar : daftar.stream().limit(PETUGAS_RINGKAS).toList());
        model.addAttribute("jumlahCocok", daftar.size());
        model.addAttribute("terdaftar", seluruh.size());
        model.addAttribute("tersedia", seluruh.stream().filter(b -> !b.bertugas() && b.dalamJamKerja()).count());
        model.addAttribute("bertugas", seluruh.stream().filter(PantauanPelaporan.BarisPetugas::bertugas).count());
        model.addAttribute("petugasPilihan", seluruh);
        model.addAttribute("siapDitugaskan", pantauan.kasusSiapDitugaskan());
        model.addAttribute("kasusTerpilih", kasus);
        model.addAttribute("q", q);
        model.addAttribute("semua", semua != null);
        model.addAttribute("bidang", Keahlian.values());
        return "admin/petugas";
    }

    @PostMapping("/petugas")
    String tambahPetugas(@RequestParam(defaultValue = "") String nama, @RequestParam(defaultValue = "") String email,
                         @RequestParam(required = false) Keahlian bidang, RedirectAttributes flash) {
        try {
            sistem.tambahPetugas(nama, email, bidang);
            flash.addFlashAttribute("sukses", "Petugas " + nama.strip() + " ditambahkan.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("kesalahan", e.getMessage());
            flash.addFlashAttribute("formPetugas", new FormPetugas(nama, email, bidang));
        }
        return "redirect:/admin/petugas";
    }

    @PostMapping("/kasus/{id}/penugasan")
    String tugaskan(@PathVariable long id, @RequestParam long petugasId, @RequestParam(defaultValue = "") String catatan,
                    @RequestParam(required = false) String kembali, RedirectAttributes flash) {
        try {
            sistem.tugaskanPetugas(id, petugasId, catatan);
            flash.addFlashAttribute("sukses", "Petugas berhasil ditugaskan.");
            flash.addFlashAttribute("berubahKasus", id);
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("kesalahan", e.getMessage());
        }
        return "redirect:" + kembaliAman(kembali, "/admin/kasus/" + id);
    }

    /** Panel "Tugaskan laporan" di halaman Petugas. */
    @PostMapping("/penugasan")
    String tugaskanDariPanel(@RequestParam(required = false) Long kasusId, @RequestParam(required = false) Long petugasId,
                             @RequestParam(defaultValue = "") String catatan, RedirectAttributes flash) {
        if (kasusId == null || petugasId == null) {
            flash.addFlashAttribute("kesalahan", kasusId == null ? "Pilih laporan yang akan ditugaskan." : "Pilih petugas.");
            return "redirect:/admin/petugas";
        }
        return tugaskan(kasusId, petugasId, catatan, "/admin/petugas", flash);
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

    /** Hanya alamat di dalam ruang admin yang boleh menjadi tujuan kembali. */
    static String kembaliAman(String kembali, String bawaan) {
        if (kembali == null || !kembali.startsWith("/admin") || kembali.contains("//") || kembali.contains("\\")
                || kembali.contains("..") || kembali.chars().anyMatch(Character::isISOControl)) return bawaan;
        return kembali;
    }

    public record FormPetugas(String nama, String email, Keahlian bidang) {}
}
