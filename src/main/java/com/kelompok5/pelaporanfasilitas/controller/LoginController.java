package com.kelompok5.pelaporanfasilitas.controller;

import java.net.URI;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.kelompok5.pelaporanfasilitas.security.AkunMasuk;
import com.kelompok5.pelaporanfasilitas.security.PembatasPercobaanMasuk;
import com.kelompok5.pelaporanfasilitas.security.PenentuPeran;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Alur masuk hanya-email/NIM untuk mode demo akademik. Lihat catatan keterbatasan di README. */
@Controller
public class LoginController {
    private final PenentuPeran penentu;
    private final PembatasPercobaanMasuk pembatas;
    private final SessionAuthenticationStrategy strategiSesi;
    private final SecurityContextRepository konteks;
    private final RequestCache tujuanAwal;
    private final long durasiMinimumMs;

    public LoginController(PenentuPeran penentu, PembatasPercobaanMasuk pembatas, SessionAuthenticationStrategy strategiSesi,
                           SecurityContextRepository konteks, RequestCache tujuanAwal,
                           @Value("${umpeduli.masuk.durasi-minimum-ms:350}") long durasiMinimumMs) {
        this.penentu = penentu;
        this.pembatas = pembatas;
        this.strategiSesi = strategiSesi;
        this.konteks = konteks;
        this.tujuanAwal = tujuanAwal;
        this.durasiMinimumMs = durasiMinimumMs;
    }

    @GetMapping("/login")
    String halaman(@AuthenticationPrincipal AkunMasuk akun, Model model) {
        if (akun != null) return "redirect:" + akun.beranda();
        model.addAttribute("identitas", "");
        return "login";
    }

    @PostMapping("/login")
    String masuk(@RequestParam(defaultValue = "") String identitas, HttpServletRequest request,
                 HttpServletResponse response, Model model) {
        long mulai = System.nanoTime();
        try {
            if (!pembatas.izinkan(request.getRemoteAddr())) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                return gagal(model, identitas, "Terlalu banyak percobaan masuk dari perangkat ini. Tunggu lima menit, lalu coba lagi.");
            }
            AkunMasuk akun;
            try {
                akun = penentu.masuk(identitas);
            } catch (IllegalArgumentException e) {
                response.setStatus(422);
                return gagal(model, identitas, e.getMessage());
            }
            var autentikasi = UsernamePasswordAuthenticationToken.authenticated(
                    akun, null, List.of(new SimpleGrantedAuthority(akun.peran().authority())));
            strategiSesi.onAuthentication(autentikasi, request, response);
            SecurityContext isi = SecurityContextHolder.createEmptyContext();
            isi.setAuthentication(autentikasi);
            SecurityContextHolder.setContext(isi);
            konteks.saveContext(isi, request, response);
            model.addAttribute("akun", akun);
            model.addAttribute("tujuan", tujuan(akun, request, response));
            return "masuk-berhasil";
        } finally {
            seragamkanWaktu(mulai);
        }
    }

    @GetMapping("/akses-ditolak")
    String aksesDitolak(@AuthenticationPrincipal AkunMasuk akun, HttpServletResponse response, Model model) {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        model.addAttribute("akunMasuk", akun);
        return "akses-ditolak";
    }

    private String gagal(Model model, String identitas, String pesan) {
        model.addAttribute("identitas", identitas.length() > 150 ? identitas.substring(0, 150) : identitas);
        model.addAttribute("kesalahan", pesan);
        return "login";
    }

    /** Kembali ke halaman yang diminta sebelum masuk bila masih di ruang kerja yang sama. */
    private String tujuan(AkunMasuk akun, HttpServletRequest request, HttpServletResponse response) {
        SavedRequest tersimpan = tujuanAwal.getRequest(request, response);
        tujuanAwal.removeRequest(request, response);
        if (tersimpan == null) return akun.beranda();
        try {
            URI alamat = URI.create(tersimpan.getRedirectUrl());
            String jalur = alamat.getRawPath();
            if (jalur == null || jalur.contains("..")) return akun.beranda();
            if (jalur.equals(akun.beranda()) || jalur.startsWith(akun.beranda() + "/")) {
                String kueri = alamat.getRawQuery() == null ? "" : java.util.Arrays.stream(alamat.getRawQuery().split("&"))
                        .filter(p -> !p.isEmpty() && !p.equals("continue") && !p.startsWith("continue="))
                        .collect(java.util.stream.Collectors.joining("&"));
                return kueri.isEmpty() ? jalur : jalur + "?" + kueri;
            }
        } catch (IllegalArgumentException ignored) {
            // Alamat tersimpan tidak valid: pakai beranda peran.
        }
        return akun.beranda();
    }

    /** Waktu respons POST /login dibuat seragam agar tidak membedakan akun terdaftar dan tidak. */
    private void seragamkanWaktu(long mulai) {
        long sisa = durasiMinimumMs - (System.nanoTime() - mulai) / 1_000_000;
        if (sisa <= 0) return;
        try {
            Thread.sleep(sisa);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
