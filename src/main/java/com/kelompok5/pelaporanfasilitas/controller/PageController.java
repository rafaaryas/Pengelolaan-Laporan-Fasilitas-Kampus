package com.kelompok5.pelaporanfasilitas.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/bantuan")
    public String bantuan() {
        return "bantuan";
    }
}
