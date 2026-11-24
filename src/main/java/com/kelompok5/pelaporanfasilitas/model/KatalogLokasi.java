package com.kelompok5.pelaporanfasilitas.model;

import java.util.List;

/**
 * Daftar fakultas dan gedung yang bisa dipilih di form laporan (data demo).
 * Ubah di sini bila katalog lokasi kampus berubah.
 */
public final class KatalogLokasi {
    public record Fakultas(String nama, List<String> gedung) {
        public String getLabel() { return "Fakultas " + nama; }
        public String getNama() { return nama; }
        public List<String> getGedung() { return gedung; }
    }

    public static final List<Fakultas> FAKULTAS = List.of(
            new Fakultas("Teknik", List.of("B11", "B12", "B13")),
            new Fakultas("Ekonomi dan Bisnis", List.of("D10")),
            new Fakultas("Ilmu Pendidikan", List.of("D2")),
            new Fakultas("Sastra", List.of("E7")),
            new Fakultas("Matematika dan IPA", List.of("B20")),
            new Fakultas("Ilmu Sosial", List.of("I1")));

    public static final int LANTAI_TERTINGGI = 5;

    private KatalogLokasi() {}

    public static boolean sah(String fakultas, String gedung) {
        return FAKULTAS.stream().anyMatch(f -> f.nama().equals(fakultas) && f.gedung().contains(gedung));
    }
}
