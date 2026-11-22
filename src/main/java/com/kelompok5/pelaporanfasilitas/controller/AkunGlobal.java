package com.kelompok5.pelaporanfasilitas.controller;

import com.kelompok5.pelaporanfasilitas.security.AkunMasuk;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Menyediakan akun yang sedang masuk untuk header setiap ruang kerja. */
@ControllerAdvice
public class AkunGlobal {
    @ModelAttribute("akunMasuk")
    AkunMasuk akunMasuk(@AuthenticationPrincipal AkunMasuk akun) { return akun; }
}
