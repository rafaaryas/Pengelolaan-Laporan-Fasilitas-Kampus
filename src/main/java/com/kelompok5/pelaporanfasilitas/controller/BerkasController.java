package com.kelompok5.pelaporanfasilitas.controller;

import java.time.Duration;
import com.kelompok5.pelaporanfasilitas.exception.DataTidakDitemukan;
import com.kelompok5.pelaporanfasilitas.model.Berkas;
import com.kelompok5.pelaporanfasilitas.model.KasusKerusakan;
import com.kelompok5.pelaporanfasilitas.security.AkunMasuk;
import com.kelompok5.pelaporanfasilitas.service.PantauanPelaporan;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/** Foto bukti dan foto hasil perbaikan dari database SQL. Hanya pihak terkait yang bisa membukanya. */
@Controller
@RequestMapping("/berkas")
public class BerkasController {
    private final PantauanPelaporan pantauan;

    public BerkasController(PantauanPelaporan pantauan) {
        this.pantauan = pantauan;
    }

    @GetMapping("/bukti/{laporanId}")
    ResponseEntity<byte[]> bukti(@PathVariable long laporanId, @AuthenticationPrincipal AkunMasuk akun) {
        var laporan = pantauan.laporanUntukBerkas(laporanId);
        boolean boleh = switch (akun.peran()) {
            case ADMIN -> true;
            case MAHASISWA -> laporan.getPelapor().getId() == akun.id();
            case PETUGAS -> ditugaskanKe(laporan.getKasus(), akun);
        };
        return kirim(boleh, laporan.getFotoBukti());
    }

    @GetMapping("/hasil/{tugasId}")
    ResponseEntity<byte[]> hasil(@PathVariable long tugasId, @AuthenticationPrincipal AkunMasuk akun) {
        var tugas = pantauan.tugasUntukBerkas(tugasId);
        boolean boleh = switch (akun.peran()) {
            case ADMIN -> true;
            case PETUGAS -> tugas.getPetugas().getId() == akun.id();
            case MAHASISWA -> tugas.getKasus().getDaftarLaporan().stream().anyMatch(l -> l.getPelapor().getId() == akun.id());
        };
        return kirim(boleh, tugas.getFotoHasil());
    }

    private boolean ditugaskanKe(KasusKerusakan kasus, AkunMasuk akun) {
        return pantauan.tugasKasus(kasus.getId()).map(t -> t.getPetugas().getId() == akun.id()).orElse(false);
    }

    private ResponseEntity<byte[]> kirim(boolean boleh, Berkas berkas) {
        if (!boleh || berkas == null || !berkas.ada()) throw new DataTidakDitemukan("Foto tidak ditemukan.");
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(berkas.getMimeType()))
                .body(berkas.getData());
    }
}
