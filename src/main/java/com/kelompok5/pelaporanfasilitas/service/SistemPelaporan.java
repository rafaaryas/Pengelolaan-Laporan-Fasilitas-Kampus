package com.kelompok5.pelaporanfasilitas.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import com.kelompok5.pelaporanfasilitas.exception.DataTidakDitemukan;
import com.kelompok5.pelaporanfasilitas.model.*;
import com.kelompok5.pelaporanfasilitas.repository.*;
import com.kelompok5.pelaporanfasilitas.service.prioritas.StrategiPrioritas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class SistemPelaporan {
    private final PenggunaRepository pengguna;
    private final FasilitasRepository fasilitas;
    private final KasusRepository kasus;
    private final LaporanRepository laporan;
    private final TugasRepository tugas;
    private final StrategiPrioritas strategi;
    private final Clock jam;

    public SistemPelaporan(PenggunaRepository pengguna, FasilitasRepository fasilitas, KasusRepository kasus,
                           LaporanRepository laporan, TugasRepository tugas, StrategiPrioritas strategi, Clock jam) {
        this.pengguna = pengguna;
        this.fasilitas = fasilitas;
        this.kasus = kasus;
        this.laporan = laporan;
        this.tugas = tugas;
        this.strategi = strategi;
        this.jam = jam;
    }

    public long prosesLaporan(long mahasiswaId, long fasilitasId, JenisKerusakan jenis, String deskripsi) {
        Fasilitas lokasi = kunciFasilitas(fasilitasId);
        Mahasiswa pelapor = peran(mahasiswaId, Mahasiswa.class, Aksi.MELAPOR);
        LaporanKerusakan kiriman = pelapor.buatLaporan(lokasi, jenis, deskripsi, LocalDateTime.now(jam));
        KasusKerusakan induk = cariKasusAktif(fasilitasId, jenis)
                .orElseGet(() -> kasus.save(new KasusKerusakan(lokasi, jenis, true)));
        induk.tambahLaporan(kiriman);
        laporan.save(kiriman);
        if (lokasi.getKondisi() == Kondisi.BAIK) lokasi.perbaruiKondisi(Kondisi.PERLU_DIPERIKSA);
        return kiriman.getId();
    }

    @Transactional(readOnly = true)
    public Optional<KasusKerusakan> cariKasusAktif(long fasilitasId, JenisKerusakan jenis) {
        return kasus.findFirstByFasilitasIdAndJenisKerusakanAndPengelompokanOtomatisTrueAndStatusNotOrderById(
                fasilitasId, jenis, StatusKasus.SELESAI);
    }

    public void verifikasi(long kasusId, long petugasId, boolean bahaya, Gangguan gangguan, int unit, String catatan) {
        KasusKerusakan temuan = kunciKasus(kasusId);
        Petugas pemeriksa = peran(petugasId, Petugas.class, Aksi.VERIFIKASI);
        pemeriksa.periksa(temuan, bahaya, gangguan, unit, catatanOpsional(catatan), LocalDateTime.now(jam));
        prioritaskan(temuan);
        temuan.getFasilitas().perbaruiKondisi(Kondisi.RUSAK);
    }

    public void prioritaskan(KasusKerusakan temuan) {
        temuan.tetapkanPrioritas(strategi.tentukan(temuan), strategi.jelaskan(temuan));
    }

    public long tugaskanPetugas(long kasusId, long petugasId, String catatan) {
        KasusKerusakan temuan = kunciKasus(kasusId);
        Petugas pelaksana = pengguna.kunciPetugas(petugasId).orElseThrow(() -> new DataTidakDitemukan("Petugas tidak ditemukan."));
        if (tugas.findByKasusId(kasusId).isPresent()) throw new IllegalStateException("Kasus ini sudah memiliki penugasan.");
        if (tugas.existsByPetugasIdAndStatusNot(petugasId, StatusTugas.SELESAI)) {
            throw new IllegalStateException("Petugas sedang menangani tugas aktif.");
        }
        Admin admin = adminDemo();
        return tugas.save(admin.tetapkanPenugasan(temuan, pelaksana, LocalDateTime.now(jam), catatanOpsional(catatan))).getId();
    }

    public long pisahkanLaporan(long kasusId, long laporanId) {
        KasusKerusakan asal = kunciKasus(kasusId);
        adminDemo();
        LaporanKerusakan kiriman = asal.getDaftarLaporan().stream()
                .filter(item -> item.getId().equals(laporanId)).findFirst()
                .orElseThrow(() -> new DataTidakDitemukan("Laporan tidak ditemukan pada kasus ini."));
        KasusKerusakan baru = new KasusKerusakan(asal.getFasilitas(), asal.getJenisKerusakan(), false);
        asal.pindahkanLaporan(kiriman, baru);
        return kasus.save(baru).getId();
    }

    public void mulaiTugas(long tugasId, long petugasId) {
        kunciTugas(tugasId, petugasId).mulai(LocalDateTime.now(jam));
    }

    public void catatProgres(long tugasId, long petugasId, String catatan) {
        kunciTugas(tugasId, petugasId).catatProgres(catatan);
    }

    public void selesaikanTugas(long tugasId, long petugasId, String catatan) {
        TugasPerbaikan pekerjaan = kunciTugas(tugasId, petugasId);
        pengguna.kunciPetugas(petugasId).orElseThrow(() -> new DataTidakDitemukan("Petugas tidak ditemukan."));
        pekerjaan.selesaikan(LocalDateTime.now(jam), catatan);
        Fasilitas lokasi = pekerjaan.getKasus().getFasilitas();
        var kasusAktif = kasus.findByFasilitasIdAndStatusNot(lokasi.getId(), StatusKasus.SELESAI);
        if (kasusAktif.stream().anyMatch(KasusKerusakan::isTerverifikasi)) {
            lokasi.perbaruiKondisi(Kondisi.RUSAK);
        } else {
            lokasi.perbaruiKondisi(kasusAktif.isEmpty() ? Kondisi.BAIK : Kondisi.PERLU_DIPERIKSA);
        }
    }

    public LocalTime waktuKampus() { return LocalTime.now(jam); }

    private Admin adminDemo() {
        return pengguna.adminDemo().stream().filter(a -> a.bolehMelakukan(Aksi.KELOLA_KASUS)).findFirst()
                .orElseThrow(() -> new DataTidakDitemukan("Admin demo belum tersedia."));
    }

    private <T extends Pengguna> T peran(long id, Class<T> tipe, Aksi aksi) {
        return pengguna.findById(id).filter(tipe::isInstance).map(tipe::cast).filter(p -> p.bolehMelakukan(aksi))
                .orElseThrow(() -> new DataTidakDitemukan("Identitas demo tidak tersedia."));
    }

    private Fasilitas kunciFasilitas(long id) {
        return fasilitas.kunci(id).orElseThrow(() -> new DataTidakDitemukan("Fasilitas tidak ditemukan."));
    }

    private KasusKerusakan kunciKasus(long id) {
        kunciFasilitas(kasus.cariIdFasilitas(id).orElseThrow(() -> new DataTidakDitemukan("Kasus tidak ditemukan.")));
        return kasus.findById(id).orElseThrow(() -> new DataTidakDitemukan("Kasus tidak ditemukan."));
    }

    private TugasPerbaikan kunciTugas(long id, long petugasId) {
        kunciFasilitas(tugas.cariIdFasilitasTugas(id, petugasId)
                .orElseThrow(() -> new DataTidakDitemukan("Tugas ini tidak ditemukan pada akun petugas yang dipilih.")));
        return tugas.findById(id).orElseThrow(() -> new DataTidakDitemukan("Tugas tidak ditemukan."));
    }

    private String catatanOpsional(String catatan) {
        if (catatan == null || catatan.isBlank()) return null;
        if (catatan.length() > 1000) throw new IllegalArgumentException("Catatan maksimal 1000 karakter.");
        return catatan.strip();
    }
}
