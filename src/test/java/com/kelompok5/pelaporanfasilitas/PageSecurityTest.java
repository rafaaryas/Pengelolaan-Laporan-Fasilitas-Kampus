package com.kelompok5.pelaporanfasilitas;

import com.kelompok5.pelaporanfasilitas.repository.PenggunaRepository;
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
    @Autowired private PenggunaRepository pengguna;

    @ParameterizedTest
    @ValueSource(strings = {"/", "/mahasiswa", "/mahasiswa/laporan", "/mahasiswa/laporan/baru", "/admin",
            "/admin/kasus", "/admin/petugas", "/petugas", "/petugas/verifikasi", "/petugas/tugas"})
    void halamanDemoTerbukaLangsung(String route) throws Exception {
        mvc.perform(get(route)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Demo Mode")))
                .andExpect(content().string(not(containsString("type=\"password\""))));
    }

    @Test
    void identitasDemoPersistenDalamSesiDenganCsrf() throws Exception {
        var session = new MockHttpSession();
        String budi = pengguna.mahasiswaDemo().stream().filter(a -> a.getNama().equals("Budi"))
                .findFirst().orElseThrow().getId().toString();
        mvc.perform(post("/mahasiswa/identitas").session(session).param("id", budi))
                .andExpect(status().isForbidden());
        mvc.perform(post("/mahasiswa/identitas").session(session).with(csrf()).param("id", budi))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/mahasiswa"));
        mvc.perform(get("/mahasiswa/laporan/baru").session(session))
                .andExpect(content().string(containsString("name=\"mahasiswaId\" value=\"" + budi + "\"")));
    }

    @Test
    void inputLaporanKosongMenghasilkanValidasiDanDataTidakDitemukanTampilRamah() throws Exception {
        var mahasiswa = pengguna.mahasiswaDemo().getFirst();
        mvc.perform(post("/mahasiswa/laporan").with(csrf()).param("mahasiswaId", mahasiswa.getId().toString()))
                .andExpect(status().isOk()).andExpect(view().name("mahasiswa/form"))
                .andExpect(content().string(containsString("Isi deskripsi kerusakan.")));
        mvc.perform(get("/admin/kasus/9999999")).andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Kasus tidak ditemukan.")));
        mvc.perform(get("/login")).andExpect(status().isNotFound());
    }
}
