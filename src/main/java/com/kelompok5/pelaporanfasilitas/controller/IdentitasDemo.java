package com.kelompok5.pelaporanfasilitas.controller;

import java.util.List;
import jakarta.servlet.http.HttpSession;
import com.kelompok5.pelaporanfasilitas.exception.DataTidakDitemukan;
import com.kelompok5.pelaporanfasilitas.model.*;
import com.kelompok5.pelaporanfasilitas.repository.PenggunaRepository;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
public class IdentitasDemo {
    private final PenggunaRepository pengguna;

    public IdentitasDemo(PenggunaRepository pengguna) { this.pengguna = pengguna; }

    public Mahasiswa mahasiswa(HttpSession session, Model model) {
        return aktif("mahasiswa", pengguna.mahasiswaDemo(), session, model);
    }

    public Petugas petugas(HttpSession session, Model model) {
        return aktif("petugas", pengguna.petugasDemo(), session, model);
    }

    public void pilihMahasiswa(long id, HttpSession session) {
        pilih("mahasiswa", id, pengguna.mahasiswaDemo(), session);
    }

    public void pilihPetugas(long id, HttpSession session) {
        pilih("petugas", id, pengguna.petugasDemo(), session);
    }

    public void pastikanSama(Pengguna dipilih, Long idForm) {
        if (!dipilih.getId().equals(idForm)) {
            throw new IllegalStateException("Identitas demo berubah. Periksa form sebelum mengirim ulang.");
        }
    }

    private <T extends Pengguna> T aktif(String peran, List<T> daftar, HttpSession session, Model model) {
        Object id = session.getAttribute(peran + "Id");
        T dipilih = daftar.stream().filter(p -> p.getId().equals(id)).findFirst()
                .orElseGet(() -> daftar.stream().findFirst()
                        .orElseThrow(() -> new DataTidakDitemukan("Akun demo belum tersedia.")));
        model.addAttribute("akun", dipilih);
        model.addAttribute("pilihanAkun", daftar);
        return dipilih;
    }

    private void pilih(String peran, long id, List<? extends Pengguna> daftar, HttpSession session) {
        if (daftar.stream().noneMatch(p -> p.getId().equals(id))) {
            throw new DataTidakDitemukan("Akun demo yang dipilih tidak tersedia.");
        }
        session.setAttribute(peran + "Id", id);
    }
}
