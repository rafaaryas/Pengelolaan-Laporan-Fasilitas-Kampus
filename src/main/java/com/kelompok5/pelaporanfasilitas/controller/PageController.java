package com.kelompok5.pelaporanfasilitas.controller;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/mahasiswa")
    public String mahasiswa(Model model, Principal principal) {
        return rolePage(model, principal, "Mahasiswa", "Pelaporan dan riwayat laporan fasilitas.", "mahasiswa/index");
    }

    @GetMapping("/admin")
    public String admin(Model model, Principal principal) {
        return rolePage(model, principal, "Admin", "Verifikasi laporan, penugasan, dan monitoring perbaikan.", "admin/index");
    }

    @GetMapping("/petugas")
    public String petugas(Model model, Principal principal) {
        return rolePage(model, principal, "Petugas", "Daftar tugas dan pembaruan progres perbaikan.", "petugas/index");
    }

    private String rolePage(Model model, Principal principal, String role, String description, String view) {
        model.addAttribute("role", role);
        model.addAttribute("description", description);
        model.addAttribute("username", principal.getName());
        return view;
    }
}
