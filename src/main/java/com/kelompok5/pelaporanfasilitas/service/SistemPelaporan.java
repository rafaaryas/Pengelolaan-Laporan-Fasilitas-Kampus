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

    /** Laporan dari form "Buat laporan kerusakan": lokasi, deskripsi, dan foto bukti. Satu laporan satu kasus. */
    public long laporkan(long mahasiswaId, Lokasi lokasi, String deskripsi, Berkas foto) {
        Mahasiswa pelapor = peran(mahasiswaId, Mahasiswa.class, Aksi.MELAPOR);
        LaporanKerusakan kiriman = pelapor.buatLaporan(lokasi, deskripsi, foto, LocalDateTime.now(jam));
        KasusKerusakan induk = kasus.save(KasusKerusakan.untukLaporan());
        induk.tambahLaporan(kiriman);
        laporan.save(kiriman);
        return kiriman.getId();
    }

    /** Admin memverifikasi laporan; bila petugas dipilih, langsung ditugaskan. */
    public void verifikasiAdmin(long kasusId, Long petugasId, String catatan) {
        KasusKerusakan temuan = kunciKasus(kasusId);
        adminDemo().verifikasi(temuan, LocalDateTime.now(jam));
        if (petugasId != null) tugaskanPetugas(kasusId, petugasId, catatan);
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
        if (temuan.getFasilitas() != null) temuan.getFasilitas().perbaruiKondisi(Kondisi.RUSAK);
    }

    public void prioritaskan(KasusKerusakan temuan) {
        temuan.tetapkanPrioritas(strategi.tentukan(temuan), strategi.jelaskan(temuan));
    }

    public long tugaskanPetugas(long kasusId, long petugasId, String catatan) {
        KasusKerusakan temuan = kunciKasus(kasusId);
        Petugas pelaksana = pengguna.kunciPetugas(petugasId).orElseThrow(() -> new DataTidakDitemukan("Petugas tidak ditemukan."));
        if (tugas.findByKasusId(kasusId).isPresent()) throw new IllegalStateException("Kasus ini sudah memiliki penugasan.");
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

    /** "Simpan pembaruan" di layar petugas: memulai perbaikan bila belum dimulai, lalu mencatat progres. */
    public void perbaruiProgres(long tugasId, long petugasId, String catatan) {
        TugasPerbaikan pekerjaan = kunciTugas(tugasId, petugasId);
        if (catatan == null || catatan.isBlank()) throw new IllegalArgumentException("Isi catatan penanganan.");
        if (pekerjaan.getStatus() == StatusTugas.DITUGASKAN) pekerjaan.mulai(LocalDateTime.now(jam));
        pekerjaan.catatProgres(catatan);
    }

    /** "Tandai selesai": catatan dan foto hasil perbaikan wajib. */
    public void selesaikanTugas(long tugasId, long petugasId, String catatan, Berkas fotoHasil) {
        TugasPerbaikan pekerjaan = kunciTugas(tugasId, petugasId);
        if (pekerjaan.getStatus() == StatusTugas.DITUGASKAN) pekerjaan.mulai(LocalDateTime.now(jam));
        Petugas pelaksana = pengguna.kunciPetugas(petugasId).orElseThrow(() -> new DataTidakDitemukan("Petugas tidak ditemukan."));
        pekerjaan.selesaikan(LocalDateTime.now(jam), catatan, fotoHasil);
        pelaksana.perbaruiKetersediaan(tugas.existsByPetugasIdAndStatusNot(petugasId, StatusTugas.SELESAI));
    }

    public long tambahPetugas(String nama, String email, Keahlian bidang) {
        String kontak = email == null ? "" : email.strip().toLowerCase(java.util.Locale.ROOT);
        if (!kontak.matches("^[a-z0-9._%+-]{1,64}@[a-z0-9-]+(\\.[a-z0-9-]+)*\\.[a-z]{2,24}$")) {
            throw new IllegalArgumentException("Isi email petugas yang valid, misalnya nama@um.ac.id.");
        }
        if (pengguna.cariEmail(kontak).isPresent()) throw new IllegalArgumentException("Email ini sudah dipakai akun lain.");
        String kode = String.format("PTG-%03d", pengguna.jumlahPetugas() + 1);
        return pengguna.save(Petugas.baru(nama, kontak, bidang, kode)).getId();
    }

    public void selesaikanTugas(long tugasId, long petugasId, String catatan) {
        TugasPerbaikan pekerjaan = kunciTugas(tugasId, petugasId);
        pengguna.kunciPetugas(petugasId).orElseThrow(() -> new DataTidakDitemukan("Petugas tidak ditemukan."));
        pekerjaan.selesaikan(LocalDateTime.now(jam), catatan);
        pengguna.kunciPetugas(petugasId).ifPresent(p -> p.perbaruiKetersediaan(tugas.existsByPetugasIdAndStatusNot(petugasId, StatusTugas.SELESAI)));
        Fasilitas lokasi = pekerjaan.getKasus().getFasilitas();
        if (lokasi == null) return;
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
        Long fasilitasId = kasus.cariIdFasilitas(id).orElse(null);
        if (fasilitasId != null) {
            kunciFasilitas(fasilitasId);
            return kasus.findById(id).orElseThrow(() -> new DataTidakDitemukan("Kasus tidak ditemukan."));
        }
        return kasus.kunci(id).orElseThrow(() -> new DataTidakDitemukan("Kasus tidak ditemukan."));
    }

    private TugasPerbaikan kunciTugas(long id, long petugasId) {
        TugasPerbaikan milik = tugas.findByIdAndPetugasId(id, petugasId)
                .orElseThrow(() -> new DataTidakDitemukan("Tugas ini tidak ditemukan pada akun petugas yang dipilih."));
        kunciKasus(milik.getKasus().getId());
        return milik;
    }

    private String catatanOpsional(String catatan) {
        if (catatan == null || catatan.isBlank()) return null;
        if (catatan.length() > 1000) throw new IllegalArgumentException("Catatan maksimal 1000 karakter.");
        return catatan.strip();
    }
}
