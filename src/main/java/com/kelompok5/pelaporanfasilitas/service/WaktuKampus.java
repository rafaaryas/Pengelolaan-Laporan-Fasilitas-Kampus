package com.kelompok5.pelaporanfasilitas.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Sapaan dan format tanggal Indonesia untuk template (dipanggil sebagai ${@waktu...}). */
@Component("waktu")
public class WaktuKampus {
    private static final Locale ID = Locale.forLanguageTag("id-ID");
    private static final DateTimeFormatter HARI_TANGGAL = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", ID);
    private static final DateTimeFormatter TANGGAL_PANJANG = DateTimeFormatter.ofPattern("d MMMM yyyy", ID);
    private static final DateTimeFormatter TANGGAL = DateTimeFormatter.ofPattern("d MMM yyyy", ID);
    private static final DateTimeFormatter TANGGAL_JAM = DateTimeFormatter.ofPattern("d MMM yyyy, HH.mm", ID);
    private static final DateTimeFormatter TANGGAL_PANJANG_JAM = DateTimeFormatter.ofPattern("d MMMM yyyy, HH.mm", ID);
    private static final DateTimeFormatter TANGGAL_TITIK_JAM = DateTimeFormatter.ofPattern("d MMM yyyy · HH.mm", ID);

    private final Clock jam;

    public WaktuKampus(Clock jam) { this.jam = jam; }

    /** "Selamat pagi" sampai "Selamat malam" menurut jam kampus (WIB). */
    public String sapaan() {
        int j = LocalTime.now(jam).getHour();
        if (j >= 4 && j < 11) return "Selamat pagi";
        if (j >= 11 && j < 15) return "Selamat siang";
        if (j >= 15 && j < 18) return "Selamat sore";
        return "Selamat malam";
    }

    public String hariIni() { return LocalDate.now(jam).format(HARI_TANGGAL); }
    public String tanggalHariIni() { return LocalDate.now(jam).format(TANGGAL_PANJANG); }
    public String isoHariIni() { return LocalDate.now(jam).toString(); }

    public String tanggal(LocalDateTime w) { return w == null ? "" : w.format(TANGGAL); }
    public String tanggalJam(LocalDateTime w) { return w == null ? "" : w.format(TANGGAL_JAM); }
    public String tanggalPanjangJam(LocalDateTime w) { return w == null ? "" : w.format(TANGGAL_PANJANG_JAM); }
    public String tanggalTitikJam(LocalDateTime w) { return w == null ? "" : w.format(TANGGAL_TITIK_JAM); }
    public String iso(LocalDateTime w) { return w == null ? "" : w.toString(); }

    /** "5 menit lalu", "2 jam lalu", atau tanggal bila lebih dari seminggu. */
    public String relatif(LocalDateTime w) {
        if (w == null) return "";
        long menit = java.time.Duration.between(w, LocalDateTime.now(jam)).toMinutes();
        if (menit < 1) return "Baru saja";
        if (menit < 60) return menit + " menit lalu";
        if (menit < 60 * 24) return (menit / 60) + " jam lalu";
        if (menit < 60 * 24 * 7) return (menit / (60 * 24)) + " hari lalu";
        return tanggal(w);
    }
}
