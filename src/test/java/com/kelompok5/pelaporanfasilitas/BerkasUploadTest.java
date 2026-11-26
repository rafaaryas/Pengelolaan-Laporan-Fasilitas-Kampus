package com.kelompok5.pelaporanfasilitas;

import com.kelompok5.pelaporanfasilitas.model.Gangguan;
import com.kelompok5.pelaporanfasilitas.repository.LaporanRepository;
import com.kelompok5.pelaporanfasilitas.repository.TugasRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:berkas;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BerkasUploadTest {
    private static final byte[] PNG_MINIMAL = new byte[]{
            (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n',
            0, 0, 0, 13, 'I', 'H', 'D', 'R', 0, 0, 0, 1, 0, 0, 0, 1, 8, 2, 0, 0, 0, (byte) 0x90, 0x77, 0x53, (byte) 0xDE
    };
    private static final byte[] JPEG_MINIMAL = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F', 0
    };

    @Autowired private MockMvc mvc;
    @Autowired private LaporanRepository laporanRepo;
    @Autowired private TugasRepository tugasRepo;

    @Test
    void alurUploadDanTampilFotoBuktiSertaFotoHasilBerjalanEndToEnd() throws Exception {
        MockHttpSession sesiAndi = Masuk.sebagai(mvc, "andi@example.invalid");
        MockHttpSession sesiBudi = Masuk.sebagai(mvc, "budi@example.invalid");
        MockHttpSession sesiAdmin = Masuk.sebagai(mvc, "admin@example.invalid");
        MockHttpSession sesiPetugas = Masuk.sebagai(mvc, "dimas@example.invalid");
        MockHttpSession sesiPetugasLain = Masuk.sebagai(mvc, "agus@example.invalid");

        long andiId = Masuk.akun(sesiAndi).id();
        long petugasId = Masuk.akun(sesiPetugas).id();

        // 1. Mahasiswa upload laporan dengan foto bukti PNG
        var fotoBukti = new MockMultipartFile("foto", "ac-rusak.png", "image/png", PNG_MINIMAL);
        var hasilBuat = mvc.perform(multipart("/mahasiswa/laporan")
                        .file(fotoBukti)
                        .session(sesiAndi)
                        .with(csrf())
                        .param("mahasiswaId", String.valueOf(andiId))
                        .param("fakultas", "Teknik")
                        .param("gedung", "B11")
                        .param("lantai", "2")
                        .param("ruang", "204")
                        .param("deskripsi", "AC ruang 204 tidak dingin."))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String redirectUrl = hasilBuat.getResponse().getRedirectedUrl();
        assertNotNull(redirectUrl);
        long laporanId = Long.parseLong(redirectUrl.substring(redirectUrl.lastIndexOf('/') + 1));
        var laporan = laporanRepo.findById(laporanId).orElseThrow();
        assertTrue(laporan.isAdaFoto());
        long kasusId = laporan.getKasus().getId();

        // Mahasiswa pemilik & Admin bisa membuka /berkas/bukti/{id}, mahasiswa lain ditolak (404)
        mvc.perform(get("/berkas/bukti/" + laporanId).session(sesiAndi))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(PNG_MINIMAL));
        mvc.perform(get("/berkas/bukti/" + laporanId).session(sesiAdmin))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
        mvc.perform(get("/berkas/bukti/" + laporanId).session(sesiBudi))
                .andExpect(status().isNotFound());
        mvc.perform(get("/berkas/bukti/" + laporanId).session(sesiPetugas))
                .andExpect(status().isNotFound());

        // Halaman detail mahasiswa merender <img> foto bukti
        mvc.perform(get("/mahasiswa/laporan/" + laporanId).session(sesiAndi))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/berkas/bukti/" + laporanId)));

        // 2. Admin verifikasi dan tugaskan ke Dimas
        mvc.perform(post("/admin/kasus/" + kasusId + "/verifikasi")
                        .session(sesiAdmin)
                        .with(csrf())
                        .param("petugasId", String.valueOf(petugasId))
                        .param("catatan", "Segera cek"))
                .andExpect(status().is3xxRedirection());

        long tugasId = tugasRepo.findByKasusId(kasusId).orElseThrow().getId();

        // Sekarang petugas yang ditugaskan bisa membuka foto bukti, petugas lain tidak
        mvc.perform(get("/berkas/bukti/" + laporanId).session(sesiPetugas))
                .andExpect(status().isOk());
        mvc.perform(get("/berkas/bukti/" + laporanId).session(sesiPetugasLain))
                .andExpect(status().isNotFound());

        // 3. Petugas periksa, mulai, lalu selesaikan dengan foto hasil JPEG
        mvc.perform(post("/petugas/tugas/" + tugasId + "/pemeriksaan")
                        .session(sesiPetugas)
                        .with(csrf())
                        .param("bahayaKritis", "false")
                        .param("gangguan", Gangguan.RINGAN.name())
                        .param("unitRusak", "1")
                        .param("catatan", "Filter kotor"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/petugas/tugas/" + tugasId + "/mulai")
                        .session(sesiPetugas)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        var fotoHasil = new MockMultipartFile("foto", "selesai.jpg", "image/jpeg", JPEG_MINIMAL);
        mvc.perform(multipart("/petugas/tugas/" + tugasId + "/selesai")
                        .file(fotoHasil)
                        .session(sesiPetugas)
                        .with(csrf())
                        .param("catatan", "Filter sudah dibersihkan."))
                .andExpect(status().is3xxRedirection());

        // Foto hasil bisa dibuka oleh Petugas, Admin, dan Mahasiswa pelapor; ditolak untuk user lain
        mvc.perform(get("/berkas/hasil/" + tugasId).session(sesiPetugas))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(JPEG_MINIMAL));
        mvc.perform(get("/berkas/hasil/" + tugasId).session(sesiAdmin))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"));
        mvc.perform(get("/berkas/hasil/" + tugasId).session(sesiAndi))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"));
        mvc.perform(get("/berkas/hasil/" + tugasId).session(sesiBudi))
                .andExpect(status().isNotFound());

        // Halaman detail petugas & mahasiswa merender tautan/gambar hasil perbaikan
        mvc.perform(get("/petugas/tugas/" + tugasId).session(sesiPetugas))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/berkas/hasil/" + tugasId)));
        mvc.perform(get("/mahasiswa/laporan/" + laporanId).session(sesiAndi))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/berkas/hasil/" + laporanId)));
    }
}
