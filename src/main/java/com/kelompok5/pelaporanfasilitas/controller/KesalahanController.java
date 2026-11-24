package com.kelompok5.pelaporanfasilitas.controller;

import com.kelompok5.pelaporanfasilitas.exception.DataTidakDitemukan;
import com.kelompok5.pelaporanfasilitas.security.AkunMasuk;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class KesalahanController {
    @ExceptionHandler(DataTidakDitemukan.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String tidakDitemukan(DataTidakDitemukan kesalahan, Model model) {
        model.addAttribute("pesan", kesalahan.getMessage());
        model.addAttribute("status", 404);
        Authentication masuk = SecurityContextHolder.getContext().getAuthentication();
        if (masuk != null && masuk.getPrincipal() instanceof AkunMasuk akun) model.addAttribute("akunMasuk", akun);
        return "error";
    }

    /** Foto lebih dari 5 MB ditolak sebelum sampai ke controller: kembali ke form dengan pesan perbaikan. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    String fotoTerlaluBesar(HttpServletRequest request, RedirectAttributes flash) {
        flash.addFlashAttribute("kesalahan", "Foto maksimal 5 MB, format JPG atau PNG. Pilih foto yang lebih kecil, lalu kirim lagi.");
        String asal = request.getRequestURI().replaceAll("/(progres|selesai)$", "");
        if (asal.equals("/mahasiswa/laporan")) asal = "/mahasiswa/laporan/baru";
        if (!asal.matches("^/(mahasiswa|petugas|admin)(/[A-Za-z0-9-]+)*$")) asal = "/";
        return "redirect:" + asal;
    }
}
