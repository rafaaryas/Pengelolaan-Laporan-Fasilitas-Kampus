package com.kelompok5.pelaporanfasilitas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PageSecurityTest {
    @Autowired private MockMvc mvc;

    @Test
    void landingMengarahkanKeLoginTanpaPemilihPeran() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Masuk dan laporkan kerusakan")))
                .andExpect(content().string(containsString("href=\"/login\"")))
                .andExpect(content().string(not(containsString("Pilih ruang kerja"))));
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"identitas\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(not(containsString("type=\"password\""))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/mahasiswa", "/mahasiswa/laporan", "/mahasiswa/laporan/baru", "/admin", "/admin/kasus",
            "/admin/petugas", "/petugas", "/petugas/verifikasi", "/petugas/tugas"})
    void ruangKerjaMemintaMasukTerlebihDahulu(String route) throws Exception {
        mvc.perform(get(route)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/mahasiswa", "/mahasiswa/laporan", "/mahasiswa/laporan/baru"})
    void halamanMahasiswaTerbukaSetelahMasuk(String route) throws Exception {
        var sesi = Masuk.sebagai(mvc, "andi@example.invalid");
        mvc.perform(get(route).session(sesi)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Andi")))
                .andExpect(content().string(containsString("action=\"/logout\"")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/petugas", "/petugas/verifikasi", "/petugas/tugas"})
    void halamanPetugasTerbukaSetelahMasuk(String route) throws Exception {
        var sesi = Masuk.sebagai(mvc, "dimas@example.invalid");
        mvc.perform(get(route).session(sesi)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Dimas Pratama")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin", "/admin/kasus", "/admin/petugas"})
    void halamanAdminTerbukaSetelahMasuk(String route) throws Exception {
        var sesi = Masuk.sebagai(mvc, "admin@example.invalid");
        mvc.perform(get(route).session(sesi)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Administrator UM")));
    }

    @Test
    void inputLaporanKosongMenghasilkanValidasiDanDataTidakDitemukanTampilRamah() throws Exception {
        MockHttpSession mahasiswa = Masuk.sebagai(mvc, "andi@example.invalid");
        String id = Masuk.akun(mahasiswa).id() + "";
        mvc.perform(post("/mahasiswa/laporan").session(mahasiswa).with(csrf()).param("mahasiswaId", id))
                .andExpect(status().isOk()).andExpect(view().name("mahasiswa/form"))
                .andExpect(content().string(containsString("Isi deskripsi kerusakan.")))
                .andExpect(content().string(containsString("Pilih fasilitas.")));
        MockHttpSession admin = Masuk.sebagai(mvc, "admin@example.invalid");
        mvc.perform(get("/admin/kasus/9999999").session(admin)).andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Kasus tidak ditemukan.")))
                .andExpect(content().string(containsString("href=\"/admin\"")));
    }

    @Test
    void routeIdentitasDemoLamaSudahDihapus() throws Exception {
        var sesi = Masuk.sebagai(mvc, "andi@example.invalid");
        mvc.perform(post("/mahasiswa/identitas").session(sesi).with(csrf()).param("id", "2"))
                .andExpect(status().isNotFound());
        var petugas = Masuk.sebagai(mvc, "dimas@example.invalid");
        mvc.perform(post("/petugas/identitas").session(petugas).with(csrf()).param("id", "6"))
                .andExpect(status().isNotFound());
    }
}
