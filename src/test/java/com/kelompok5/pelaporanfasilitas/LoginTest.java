package com.kelompok5.pelaporanfasilitas;

import java.time.Clock;
import com.kelompok5.pelaporanfasilitas.model.Mahasiswa;
import com.kelompok5.pelaporanfasilitas.repository.PenggunaRepository;
import com.kelompok5.pelaporanfasilitas.security.AkunMasuk.Peran;
import com.kelompok5.pelaporanfasilitas.security.PembatasPercobaanMasuk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginTest {
    @Autowired private MockMvc mvc;
    @Autowired private PenggunaRepository pengguna;

    @ParameterizedTest
    @CsvSource({
            "admin@example.invalid, ADMIN, /admin",
            "'  DIMAS@Example.Invalid ', PETUGAS, /petugas",
            "andi@example.invalid, MAHASISWA, /mahasiswa",
            "240535600102, MAHASISWA, /mahasiswa"})
    void peranDitentukanDariDataPengguna(String identitas, Peran peran, String beranda) throws Exception {
        var sesi = new MockHttpSession();
        mvc.perform(post("/login").session(sesi).with(csrf()).param("identitas", identitas))
                .andExpect(status().isOk()).andExpect(view().name("masuk-berhasil"))
                .andExpect(model().attribute("tujuan", beranda))
                .andExpect(content().string(containsString("Masuk sebagai")));
        assertEquals(peran, Masuk.akun(sesi).peran());
        mvc.perform(get(beranda).session(sesi)).andExpect(status().isOk());
    }

    @Test
    void nimTerdaftarMembukaAkunMahasiswaYangSama() throws Exception {
        var sesi = Masuk.sebagai(mvc, "240535600101");
        assertEquals("Andi", Masuk.akun(sesi).nama());
    }

    @Test
    void emailBerdomainAdminTanpaDataTetapMenjadiMahasiswaBaru() throws Exception {
        long sebelum = pengguna.count();
        var sesi = Masuk.sebagai(mvc, "yy@admin.com");
        var akun = Masuk.akun(sesi);
        assertEquals(Peran.MAHASISWA, akun.peran());
        assertEquals(sebelum + 1, pengguna.count());
        assertInstanceOf(Mahasiswa.class, pengguna.findById(akun.id()).orElseThrow());
        mvc.perform(get("/admin").session(sesi)).andExpect(status().isForbidden()).andExpect(forwardedUrl("/akses-ditolak"));
        mvc.perform(get("/petugas").session(Masuk.sebagai(mvc, "aa@petugas.com"))).andExpect(status().isForbidden());
    }

    @Test
    void akunMahasiswaBaruBisaMelaporDanHanyaMelihatMiliknya() throws Exception {
        var baru = Masuk.sebagai(mvc, "rafa.arya@students.um.ac.id");
        assertEquals("Rafa Arya", Masuk.akun(baru).nama());
        var hasil = mvc.perform(post("/mahasiswa/laporan").session(baru).with(csrf())
                        .param("mahasiswaId", Masuk.akun(baru).id() + "").param("fasilitasId", "2")
                        .param("jenisKerusakan", "FISIK").param("deskripsi", "Sandaran kursi baris depan patah."))
                .andExpect(status().is3xxRedirection()).andReturn();
        String detail = hasil.getResponse().getRedirectedUrl();
        mvc.perform(get(detail).session(baru)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Sandaran kursi baris depan patah.")));
        mvc.perform(get("/mahasiswa/laporan").session(baru)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Kursi kuliah")));
        mvc.perform(get(detail).session(Masuk.sebagai(mvc, "budi@example.invalid"))).andExpect(status().isNotFound());
    }

    @Test
    void nimBaruMembuatSatuAkunDanDipakaiUlang() throws Exception {
        long pertama = Masuk.akun(Masuk.sebagai(mvc, "240535699999")).id();
        long kedua = Masuk.akun(Masuk.sebagai(mvc, " 2405 3569 9999 ")).id();
        assertEquals(pertama, kedua);
        assertEquals("240535699999", ((Mahasiswa) pengguna.findById(pertama).orElseThrow()).getNim());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "bukan-email", "12345", "1234567890123", "andi@", "<script>@x.id"})
    void masukanTidakValidMenampilkanPesanDanMempertahankanIsian(String identitas) throws Exception {
        mvc.perform(post("/login").with(csrf()).param("identitas", identitas))
                .andExpect(status().is(422)).andExpect(view().name("login"))
                .andExpect(model().attributeExists("kesalahan"))
                .andExpect(content().string(containsString("aria-invalid=\"true\"")));
        mvc.perform(post("/login").with(csrf()).param("identitas", "bukan-email"))
                .andExpect(content().string(containsString("value=\"bukan-email\"")))
                .andExpect(content().string(containsString("NIM 12 digit")));
    }

    @Test
    void formLoginMembutuhkanCsrf() throws Exception {
        mvc.perform(post("/login").param("identitas", "admin@example.invalid")).andExpect(status().isForbidden());
    }

    @Test
    void sesiDigantiSetelahMasuk() throws Exception {
        var sesi = new MockHttpSession();
        String sebelum = sesi.getId();
        // Tiru browser yang mengirim cookie JSESSIONID milik sesi tersebut.
        var hasil = mvc.perform(post("/login").session(sesi).with(csrf()).param("identitas", "admin@example.invalid")
                        .with(r -> { r.setRequestedSessionId(sebelum); r.setRequestedSessionIdValid(true); r.setRequestedSessionIdFromCookie(true); return r; }))
                .andExpect(status().isOk()).andReturn();
        assertNotEquals(sebelum, hasil.getRequest().getSession(false).getId());
    }

    @Test
    void penggunaYangSudahMasukDiarahkanDariHalamanLogin() throws Exception {
        mvc.perform(get("/login").session(Masuk.sebagai(mvc, "dimas@example.invalid")))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/petugas"));
    }

    @Test
    void ruangKerjaLainDitolakDenganHalaman403() throws Exception {
        var mahasiswa = Masuk.sebagai(mvc, "andi@example.invalid");
        mvc.perform(get("/admin/kasus").session(mahasiswa)).andExpect(status().isForbidden()).andExpect(forwardedUrl("/akses-ditolak"));
        mvc.perform(get("/akses-ditolak").session(mahasiswa)).andExpect(status().isForbidden())
                .andExpect(content().string(containsString("bukan untuk ruang kerja Anda")))
                .andExpect(content().string(containsString("href=\"/mahasiswa\"")));
        var admin = Masuk.sebagai(mvc, "admin@example.invalid");
        mvc.perform(get("/petugas/tugas").session(admin)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/kasus/1/penugasan").session(Masuk.sebagai(mvc, "dimas@example.invalid")).with(csrf())
                .param("petugasId", "5")).andExpect(status().isForbidden());
    }

    @Test
    void kembaliKeTujuanSemulaSetelahMasuk() throws Exception {
        var sesi = new MockHttpSession();
        mvc.perform(get("/admin/kasus?status=SELESAI").session(sesi)).andExpect(status().is3xxRedirection());
        mvc.perform(post("/login").session(sesi).with(csrf()).param("identitas", "admin@example.invalid"))
                .andExpect(model().attribute("tujuan", "/admin/kasus?status=SELESAI"));

        var lain = new MockHttpSession();
        mvc.perform(get("/admin/kasus").session(lain)).andExpect(status().is3xxRedirection());
        mvc.perform(post("/login").session(lain).with(csrf()).param("identitas", "andi@example.invalid"))
                .andExpect(model().attribute("tujuan", "/mahasiswa"));
    }

    @Test
    void keluarMenghapusSesi() throws Exception {
        var sesi = Masuk.sebagai(mvc, "admin@example.invalid");
        mvc.perform(post("/logout").session(sesi)).andExpect(status().isForbidden());
        mvc.perform(post("/logout").session(sesi).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/?keluar"));
        assertTrue(sesi.isInvalid());
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/?keluar")).andExpect(content().string(containsString("Anda sudah keluar")));
    }

    @Test
    void pembatasMenolakPercobaanBerlebih() {
        var pembatas = new PembatasPercobaanMasuk(3, 5, Clock.systemUTC());
        assertTrue(pembatas.izinkan("10.0.0.1"));
        assertTrue(pembatas.izinkan("10.0.0.1"));
        assertTrue(pembatas.izinkan("10.0.0.1"));
        assertFalse(pembatas.izinkan("10.0.0.1"));
        assertTrue(pembatas.izinkan("10.0.0.2"));
    }
}
