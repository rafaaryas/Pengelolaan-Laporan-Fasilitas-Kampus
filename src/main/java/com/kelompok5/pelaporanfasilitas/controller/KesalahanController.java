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
}
