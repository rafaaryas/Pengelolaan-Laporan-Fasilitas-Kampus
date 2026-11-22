package com.kelompok5.pelaporanfasilitas.security;

import java.io.Serializable;
import java.security.Principal;
import com.kelompok5.pelaporanfasilitas.model.*;

/**
 * Identitas yang disimpan di konteks Spring Security setelah masuk.
 * Hanya berisi data ringkas agar aman diserialisasi ke sesi.
 */
public record AkunMasuk(long id, String nama, Peran peran, String identitas) implements Principal, Serializable {

    public enum Peran {
        MAHASISWA("Mahasiswa", "/mahasiswa"),
        PETUGAS("Petugas", "/petugas"),
        ADMIN("Admin", "/admin");

        private final String label;
        private final String beranda;

        Peran(String label, String beranda) {
            this.label = label;
            this.beranda = beranda;
        }

        public String getLabel() { return label; }
        public String getBeranda() { return beranda; }
        public String authority() { return "ROLE_" + name(); }
    }

    public static AkunMasuk dari(Pengguna pengguna, String identitas) {
        Peran peran = switch (pengguna) {
            case Admin ignored -> Peran.ADMIN;
            case Petugas ignored -> Peran.PETUGAS;
            case Mahasiswa ignored -> Peran.MAHASISWA;
            default -> throw new IllegalStateException("Peran pengguna tidak dikenal.");
        };
        return new AkunMasuk(pengguna.getId(), pengguna.getNama(), peran, identitas);
    }

    public String beranda() { return peran.getBeranda(); }

    @Override
    public String getName() { return identitas; }
}
