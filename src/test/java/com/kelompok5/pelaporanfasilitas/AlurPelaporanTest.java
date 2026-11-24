package com.kelompok5.pelaporanfasilitas;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.kelompok5.pelaporanfasilitas.exception.DataTidakDitemukan;
import com.kelompok5.pelaporanfasilitas.model.*;
import com.kelompok5.pelaporanfasilitas.repository.PenggunaRepository;
import com.kelompok5.pelaporanfasilitas.service.PantauanPelaporan;
import com.kelompok5.pelaporanfasilitas.service.SistemPelaporan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:alur;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class AlurPelaporanTest {
    @Autowired private SistemPelaporan sistem;
    @Autowired private PantauanPelaporan pantauan;
    @Autowired private PenggunaRepository pengguna;

    @TestConfiguration
    static class WaktuDemo {
        @Bean @Primary
        Clock jamTetap() {
            return Clock.fixed(Instant.parse("2026-11-20T13:00:00Z"), ZoneId.of("Asia/Jakarta"));
        }
    }

    @Test
    void alurKasusDariLaporanHinggaPerbaikanDanKejadianBerulang() {
        var mahasiswa = pengguna.mahasiswaDemo();
        long andi = mahasiswa.get(0).getId();
        long budi = mahasiswa.get(1).getId();
        long sinta = mahasiswa.get(2).getId();
        long dimas = petugas("Dimas Pratama");
        long agus = petugas("Agus Setiawan");
        long raka = petugas("Raka Santoso");
        long ac = fasilitas("AC B13");
        long kabel = fasilitas("Instalasi listrik");

        long l1 = sistem.prosesLaporan(andi, ac, JenisKerusakan.TIDAK_BERFUNGSI, "AC 1 tidak dingin.");
        long k1 = kasusLaporan(l1, andi);
        long l2 = sistem.prosesLaporan(budi, ac, JenisKerusakan.TIDAK_BERFUNGSI, "AC 1 hanya meniup angin.");
        long l3 = sistem.prosesLaporan(andi, ac, JenisKerusakan.TIDAK_BERFUNGSI, "AC 2 tidak menyala.");
        assertEquals(k1, kasusLaporan(l2, budi));
        assertEquals(k1, kasusLaporan(l3, andi));
        assertEquals(2, kasus(k1).hitungPelaporUnik());
        assertNull(kasus(k1).getPrioritas());
        assertEquals(StatusKasus.MENUNGGU_VERIFIKASI, kasus(k1).getStatus());
        assertThrows(IllegalStateException.class, () -> sistem.tugaskanPetugas(k1, dimas, ""));
        assertThrows(DataTidakDitemukan.class, () -> pantauan.detailLaporan(l1, budi));

        long beda = sistem.prosesLaporan(sinta, ac, JenisKerusakan.KEBOCORAN, "AC bocor.");
        assertNotEquals(k1, kasusLaporan(beda, sinta));
        long terpisah = sistem.pisahkanLaporan(k1, l3);
        assertNotEquals(k1, terpisah);
        assertEquals(terpisah, kasusLaporan(l3, andi));
        assertEquals(2, kasus(k1).getDaftarLaporan().size());
        assertEquals(1, kasus(terpisah).hitungPelaporUnik());
        assertThrows(IllegalStateException.class, () -> sistem.pisahkanLaporan(terpisah, l3));

        long lKabel = sistem.prosesLaporan(sinta, kabel, JenisKerusakan.BAHAYA_LISTRIK, "Ada kabel terbuka.");
        long kKabel = kasusLaporan(lKabel, sinta);
        assertThrows(IllegalArgumentException.class, () -> sistem.verifikasi(k1, dimas, false, Gangguan.BERAT, 5, ""));
        assertEquals(StatusKasus.MENUNGGU_VERIFIKASI, kasus(k1).getStatus());
        sistem.verifikasi(k1, dimas, false, Gangguan.BERAT, 1, "Kompresor gagal.");
        sistem.verifikasi(kKabel, raka, true, Gangguan.RINGAN, 1, "Konduktor terbuka.");
        assertEquals(Prioritas.TINGGI, kasus(k1).getPrioritas());
        assertEquals(dimas, kasus(k1).getPemeriksa().getId());
        assertEquals(Prioritas.KRITIS, kasus(kKabel).getPrioritas());
        assertEquals(kKabel, pantauan.daftarKasus("", StatusKasus.MENUNGGU_PENUGASAN, null).getFirst().getId());
        assertThrows(IllegalStateException.class, () -> sistem.pisahkanLaporan(k1, l2));
        assertThrows(IllegalStateException.class, () -> sistem.tugaskanPetugas(k1, petugas("Rudi Hartono"), ""));
        assertThrows(IllegalStateException.class, () -> sistem.tugaskanPetugas(k1, agus, ""));

        long pekerjaan = sistem.tugaskanPetugas(k1, dimas, "Periksa kompresor.");
        assertTrue(pengguna.petugasDemo().stream().filter(p -> p.getId() == dimas).findFirst().orElseThrow().isSibuk());
        assertThrows(IllegalStateException.class, () -> sistem.tugaskanPetugas(k1, dimas, ""));
        sistem.verifikasi(terpisah, dimas, false, Gangguan.SEBAGIAN, 1, "");
        // Sesuai layar Admin Petugas, satu petugas boleh memegang beberapa tugas aktif (Budi: "2 tugas aktif").
        long kedua = sistem.tugaskanPetugas(terpisah, dimas, "");
        assertThrows(DataTidakDitemukan.class, () -> sistem.mulaiTugas(pekerjaan, raka));
        assertThrows(IllegalStateException.class, () -> sistem.selesaikanTugas(pekerjaan, dimas, "Terlalu awal."));

        sistem.mulaiTugas(pekerjaan, dimas);
        assertEquals(StatusKasus.DIKERJAKAN, kasus(k1).getStatus());
        assertThrows(IllegalStateException.class, () -> sistem.mulaiTugas(pekerjaan, dimas));
        sistem.catatProgres(pekerjaan, dimas, "Suku cadang tiba.");
        assertEquals("Suku cadang tiba.", pantauan.detailTugas(pekerjaan, dimas).getCatatan());
        assertThrows(IllegalArgumentException.class, () -> sistem.selesaikanTugas(pekerjaan, dimas, " "));
        sistem.selesaikanTugas(pekerjaan, dimas, "Kompresor diganti.");
        var hasil = pantauan.detailTugas(pekerjaan, dimas);
        assertEquals(StatusTugas.SELESAI, hasil.getStatus());
        assertEquals("Kompresor diganti.", hasil.getCatatan());
        assertNotNull(hasil.getWaktuMulai());
        assertNotNull(hasil.getWaktuSelesai());
        assertEquals(StatusKasus.SELESAI, kasus(k1).getStatus());
        assertEquals(Kondisi.RUSAK, kasus(k1).getFasilitas().getKondisi());
        assertTrue(pengguna.petugasDemo().stream().filter(p -> p.getId() == dimas).findFirst().orElseThrow().isSibuk(),
                "Masih ada tugas kedua yang aktif, jadi petugas tetap Bertugas");
        sistem.mulaiTugas(kedua, dimas);
        sistem.selesaikanTugas(kedua, dimas, "Sambungan pipa AC dikencangkan.");
        assertFalse(pengguna.petugasDemo().stream().filter(p -> p.getId() == dimas).findFirst().orElseThrow().isSibuk());
        assertThrows(IllegalStateException.class, () -> sistem.tugaskanPetugas(k1, dimas, ""));

        long terbaru = sistem.prosesLaporan(andi, ac, JenisKerusakan.TIDAK_BERFUNGSI, "AC 1 kembali rusak.");
        assertNotEquals(k1, kasusLaporan(terbaru, andi));
        assertNotEquals(terpisah, kasusLaporan(terbaru, andi));
        assertEquals(k1, kasusLaporan(l2, budi));
    }

    @Test
    void alurLayarCivitasAdminPetugasHinggaSelesaiDenganFoto() {
        long nadia = pengguna.mahasiswaDemo().get(0).getId();
        long budi = petugas("Raka Santoso");
        var lokasi = new Lokasi("Teknik", "B11", 2, "204");
        var bukti = new Berkas("bukti-uji.jpg", "ac-ruang-204.jpg", 1_258_291);

        assertThrows(IllegalArgumentException.class, () -> sistem.laporkan(nadia, lokasi, "AC tidak dingin.", null));
        long id = sistem.laporkan(nadia, lokasi,
                "AC di ruang kuliah 204 tidak terasa dingin meskipun sudah dinyalakan. Mohon diperiksa.", bukti);
        var laporan = pantauan.detailLaporan(id, nadia).laporan();
        assertEquals("AC di ruang kuliah 204 tidak terasa dingin meskipun sudah dinyalakan", laporan.getJudul());
        assertEquals("Teknik · B11 · Lantai 2", laporan.getLokasiRingkas());
        assertEquals("B11 · Ruang 204", laporan.getGedungRuang());
        assertEquals(Tahap.MENUNGGU, laporan.getTahap());
        assertEquals(String.format("UM-%04d", id), laporan.getKode());
        long kasusId = laporan.getKasus().getId();
        assertTrue(pantauan.hitungMahasiswa(nadia, "").menunggu() >= 1);

        sistem.verifikasiAdmin(kasusId, budi, "Periksa kompresor.");
        assertEquals(Tahap.TERVERIFIKASI, pantauan.detailLaporan(id, nadia).laporan().getTahap());
        assertThrows(IllegalStateException.class, () -> sistem.verifikasiAdmin(kasusId, null, ""));
        long tugasId = pantauan.detailLaporan(id, nadia).tugas().getId();
        assertEquals(1, pantauan.ringkasanPetugas(budi).perluDiperiksa());

        assertThrows(IllegalArgumentException.class, () -> sistem.perbaruiProgres(tugasId, budi, " "));
        sistem.perbaruiProgres(tugasId, budi, "Filter AC kotor, sedang dibersihkan.");
        assertEquals(Tahap.DIPERBAIKI, pantauan.detailLaporan(id, nadia).laporan().getTahap());
        assertEquals(1, pantauan.ringkasanPetugas(budi).diperbaiki());

        assertThrows(IllegalArgumentException.class, () -> sistem.selesaikanTugas(tugasId, budi, "Selesai.", null));
        assertEquals(Tahap.DIPERBAIKI, pantauan.detailLaporan(id, nadia).laporan().getTahap());
        sistem.selesaikanTugas(tugasId, budi, "AC dingin kembali.", new Berkas("hasil-uji.jpg", "hasil.jpg", 900_000));
        var akhir = pantauan.detailLaporan(id, nadia);
        assertEquals(Tahap.SELESAI, akhir.laporan().getTahap());
        assertTrue(akhir.tugas().isAdaFotoHasil());
        assertEquals(1, pantauan.ringkasanPetugas(budi).selesaiBulanIni());
        assertFalse(pengguna.petugasDemo().stream().filter(p -> p.getId() == budi).findFirst().orElseThrow().isSibuk());
    }

    @Test
    void laporanBersamaanMasukKeSatuKasusAktif() throws Exception {
        var mahasiswa = pengguna.mahasiswaDemo();
        long lokasi = fasilitas("Keran wastafel");
        CyclicBarrier mulai = new CyclicBarrier(2);
        try (var worker = Executors.newFixedThreadPool(2)) {
            var satu = worker.submit(() -> {
                mulai.await(5, TimeUnit.SECONDS);
                return sistem.prosesLaporan(mahasiswa.get(0).getId(), lokasi, JenisKerusakan.KEBOCORAN, "Keran terus mengalir.");
            });
            var dua = worker.submit(() -> {
                mulai.await(5, TimeUnit.SECONDS);
                return sistem.prosesLaporan(mahasiswa.get(1).getId(), lokasi, JenisKerusakan.KEBOCORAN, "Keran tidak bisa ditutup.");
            });
            long id = kasusLaporan(satu.get(15, TimeUnit.SECONDS), mahasiswa.get(0).getId());
            assertEquals(id, kasusLaporan(dua.get(15, TimeUnit.SECONDS), mahasiswa.get(1).getId()));
            assertEquals(2, kasus(id).hitungPelaporUnik());
        }
    }

    private long kasusLaporan(long laporanId, long mahasiswaId) {
        return pantauan.detailLaporan(laporanId, mahasiswaId).laporan().getKasus().getId();
    }

    private KasusKerusakan kasus(long id) { return pantauan.detailKasus(id).kasus(); }

    private long fasilitas(String nama) {
        return pantauan.daftarFasilitas().stream().filter(f -> f.getNama().equals(nama)).findFirst().orElseThrow().getId();
    }

    private long petugas(String nama) {
        return pengguna.petugasDemo().stream().filter(p -> p.getNama().equals(nama)).findFirst().orElseThrow().getId();
    }
}
