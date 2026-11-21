package com.kelompok5.pelaporanfasilitas.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import com.kelompok5.pelaporanfasilitas.exception.DataTidakDitemukan;
import com.kelompok5.pelaporanfasilitas.model.*;
import com.kelompok5.pelaporanfasilitas.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PantauanPelaporan {
    private static final Comparator<KasusKerusakan> URGENSI = Comparator
            .comparing(KasusKerusakan::getPrioritas, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(KasusKerusakan::getUnitRusak, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Comparator.comparingLong(KasusKerusakan::hitungPelaporUnik).reversed())
            .thenComparing(KasusKerusakan::getLaporanTerbaru, Comparator.reverseOrder())
            .thenComparing(KasusKerusakan::getId, Comparator.reverseOrder());
    private final FasilitasRepository fasilitas;
    private final LaporanRepository laporan;
    private final KasusRepository kasus;
    private final TugasRepository tugas;

    public PantauanPelaporan(FasilitasRepository fasilitas, LaporanRepository laporan,
                             KasusRepository kasus, TugasRepository tugas) {
        this.fasilitas = fasilitas;
        this.laporan = laporan;
        this.kasus = kasus;
        this.tugas = tugas;
    }

    public List<Fasilitas> daftarFasilitas() { return fasilitas.findAllByOrderByGedungAscNamaAsc(); }

    public List<LaporanKerusakan> laporanMahasiswa(long mahasiswaId, String pencarian, StatusKasus status) {
        String kata = kataKunci(pencarian);
        return laporan.findByPelaporIdOrderByWaktuLaporanDescIdDesc(mahasiswaId).stream()
                .filter(l -> status == null || l.getKasus().getStatus() == status)
                .filter(l -> cocok(kata, l.getId(), l.getFasilitas(), l.getJenisKerusakan(), l.getDeskripsi()))
                .toList();
    }

    public DetailLaporan detailLaporan(long id, long mahasiswaId) {
        LaporanKerusakan kiriman = laporan.findByIdAndPelaporId(id, mahasiswaId)
                .orElseThrow(() -> new DataTidakDitemukan("Laporan tidak ditemukan pada akun mahasiswa yang dipilih."));
        return new DetailLaporan(kiriman, tugas.findByKasusId(kiriman.getKasus().getId()).orElse(null));
    }

    public List<KasusKerusakan> daftarKasus(String pencarian, StatusKasus status, Prioritas prioritas) {
        String kata = kataKunci(pencarian);
        return kasus.ringkasan().stream()
                .filter(k -> status == null || k.getStatus() == status)
                .filter(k -> prioritas == null || k.getPrioritas() == prioritas)
                .filter(k -> cocok(kata, k.getId(), k.getFasilitas(), k.getJenisKerusakan(), ""))
                .sorted(Comparator.comparing((KasusKerusakan k) -> k.getStatus() == StatusKasus.SELESAI)
                        .thenComparing(URGENSI))
                .toList();
    }

    public RangkumanAdmin rangkumanAdmin() {
        List<KasusKerusakan> semua = kasus.ringkasan();
        long selesai = jumlahStatus(semua, StatusKasus.SELESAI);
        long kritis = semua.stream().filter(k -> k.getStatus() != StatusKasus.SELESAI && k.getPrioritas() == Prioritas.KRITIS).count();
        return new RangkumanAdmin(laporan.count(), semua.size() - selesai,
                jumlahStatus(semua, StatusKasus.MENUNGGU_VERIFIKASI), kritis,
                jumlahStatus(semua, StatusKasus.DIKERJAKAN), selesai,
                semua.stream().sorted(Comparator.comparing(KasusKerusakan::getLaporanTerbaru).reversed()
                        .thenComparing(KasusKerusakan::getId, Comparator.reverseOrder())).limit(5).toList());
    }

    public DetailKasus detailKasus(long id) {
        KasusKerusakan temuan = kasus.detail(id).orElseThrow(() -> new DataTidakDitemukan("Kasus tidak ditemukan."));
        return new DetailKasus(temuan, tugas.findByKasusId(id).orElse(null));
    }

    public List<TugasPerbaikan> tugasPetugas(long id) { return tugas.findByPetugasIdOrderByWaktuPenugasanDescIdDesc(id); }

    public TugasPerbaikan detailTugas(long id, long petugasId) {
        return tugas.findByIdAndPetugasId(id, petugasId)
                .orElseThrow(() -> new DataTidakDitemukan("Tugas ini tidak ditemukan pada akun petugas yang dipilih."));
    }

    private long jumlahStatus(List<KasusKerusakan> semua, StatusKasus status) {
        return semua.stream().filter(k -> k.getStatus() == status).count();
    }

    private String kataKunci(String pencarian) {
        return pencarian == null ? "" : pencarian.strip().toLowerCase(Locale.ROOT);
    }

    private boolean cocok(String kata, long id, Fasilitas fasilitas, JenisKerusakan jenis, String deskripsi) {
        if (kata.isEmpty()) return true;
        String isi = id + " um-" + id + " k-" + id + " " + fasilitas.getNama() + " " + fasilitas.getLokasi()
                + " " + jenis.getLabel() + " " + deskripsi;
        return isi.toLowerCase(Locale.ROOT).contains(kata);
    }

    public record DetailLaporan(LaporanKerusakan laporan, TugasPerbaikan tugas) {}
    public record DetailKasus(KasusKerusakan kasus, TugasPerbaikan tugas) {}
    public record RangkumanAdmin(long totalLaporan, long kasusAktif, long menungguVerifikasi, long kritis,
                                 long dikerjakan, long selesai, List<KasusKerusakan> terbaru) {}
}
