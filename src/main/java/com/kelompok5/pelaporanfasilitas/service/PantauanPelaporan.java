package com.kelompok5.pelaporanfasilitas.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Predicate;
import com.kelompok5.pelaporanfasilitas.exception.DataTidakDitemukan;
import com.kelompok5.pelaporanfasilitas.model.*;
import com.kelompok5.pelaporanfasilitas.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PantauanPelaporan {
    private static final Locale ID = Locale.forLanguageTag("id-ID");
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
    private final PenggunaRepository pengguna;
    private final Clock jam;

    public PantauanPelaporan(FasilitasRepository fasilitas, LaporanRepository laporan, KasusRepository kasus,
                             TugasRepository tugas, PenggunaRepository pengguna, Clock jam) {
        this.fasilitas = fasilitas;
        this.laporan = laporan;
        this.kasus = kasus;
        this.tugas = tugas;
        this.pengguna = pengguna;
        this.jam = jam;
    }

    public List<Fasilitas> daftarFasilitas() { return fasilitas.findAllByOrderByGedungAscNamaAsc(); }

    // ---------- Civitas ----------

    public List<LaporanKerusakan> laporanMahasiswa(long mahasiswaId, String pencarian, Tahap tahap) {
        String kata = kataKunci(pencarian);
        return laporan.findByPelaporIdOrderByWaktuLaporanDescIdDesc(mahasiswaId).stream()
                .filter(l -> tahap == null || l.getTahap() == tahap)
                .filter(l -> cocok(kata, l, false))
                .toList();
    }

    public HitungTahap hitungMahasiswa(long mahasiswaId, String pencarian) {
        return HitungTahap.dari(laporanMahasiswa(mahasiswaId, pencarian, null));
    }

    public DetailLaporan detailLaporan(long id, long mahasiswaId) {
        LaporanKerusakan kiriman = laporan.findByIdAndPelaporId(id, mahasiswaId)
                .orElseThrow(() -> new DataTidakDitemukan("Laporan tidak ditemukan pada akun mahasiswa yang dipilih."));
        return new DetailLaporan(kiriman, tugas.findByKasusId(kiriman.getKasus().getId()).orElse(null));
    }

    // ---------- Admin ----------

    public Halaman<LaporanKerusakan> daftarLaporan(SaringanLaporan saring, int ukuran) {
        return Halaman.dari(saringLaporan(saring, true), saring.halaman(), ukuran);
    }

    /** Hitungan tab mengikuti kata kunci, tanggal, dan petugas, tetapi tidak mengikuti tab yang aktif. */
    public HitungTahap hitungLaporan(SaringanLaporan saring) {
        return HitungTahap.dari(saringLaporan(saring, false));
    }

    public List<LaporanKerusakan> semuaLaporanTersaring(SaringanLaporan saring) { return saringLaporan(saring, true); }

    private List<LaporanKerusakan> saringLaporan(SaringanLaporan saring, boolean pakaiTahap) {
        String kata = kataKunci(saring.q());
        Set<Long> kasusPetugas = saring.petugasId() == null ? null : new HashSet<>(tugas.semua().stream()
                .filter(t -> t.getPetugas().getId().equals(saring.petugasId())).map(t -> t.getKasus().getId()).toList());
        Predicate<LocalDateTime> tanggal = rentangTanggal(saring.tanggal());
        var hasil = laporan.semua().stream()
                .filter(l -> !pakaiTahap || saring.tahap() == null || l.getTahap() == saring.tahap())
                .filter(l -> kasusPetugas == null || kasusPetugas.contains(l.getKasus().getId()))
                .filter(l -> tanggal.test(l.getWaktuLaporan()))
                .filter(l -> cocok(kata, l, true));
        if (saring.kritis()) {
            hasil = hasil.filter(l -> l.getKasus().getPrioritas() == Prioritas.KRITIS && l.getTahap() != Tahap.SELESAI);
        }
        if (pakaiTahap && saring.tahap() == Tahap.TINDAK_LANJUT) {
            Map<Long, LocalDateTime> waktuPengajuan = new HashMap<>();
            tugas.semua().stream().filter(t -> t.getStatus() == StatusTugas.PERLU_TINDAK_LANJUT)
                    .forEach(t -> waktuPengajuan.put(t.getKasus().getId(), t.getWaktuTindakLanjut()));
            hasil = hasil.sorted(Comparator.comparing((LaporanKerusakan l) -> l.getKasus().getPrioritas(),
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(l -> waktuPengajuan.get(l.getKasus().getId()), Comparator.nullsLast(Comparator.naturalOrder())));
        }
        return hasil.toList();
    }

    private Predicate<LocalDateTime> rentangTanggal(String kode) {
        LocalDate hariIni = LocalDate.now(jam);
        if (kode == null) return w -> true;
        return switch (kode) {
            case "hari-ini" -> w -> w.toLocalDate().equals(hariIni);
            case "7-hari" -> w -> !w.toLocalDate().isBefore(hariIni.minusDays(6));
            case "30-hari" -> w -> !w.toLocalDate().isBefore(hariIni.minusDays(29));
            case "bulan-ini" -> w -> YearMonth.from(w).equals(YearMonth.from(hariIni));
            default -> w -> true;
        };
    }

    public RingkasanAdmin ringkasanAdmin(int bulan) {
        List<LaporanKerusakan> semua = laporan.semua();
        HitungTahap hitung = HitungTahap.dari(semua);
        YearMonth kini = YearMonth.now(jam);
        long bulanIni = semua.stream().filter(l -> YearMonth.from(l.getWaktuLaporan()).equals(kini)).count();
        long bulanLalu = semua.stream().filter(l -> YearMonth.from(l.getWaktuLaporan()).equals(kini.minusMonths(1))).count();
        List<TugasPerbaikan> selesai = tugas.semua().stream().filter(t -> t.getWaktuSelesai() != null).toList();
        List<TitikBulan> grafik = new ArrayList<>();
        for (int i = bulan - 1; i >= 0; i--) {
            YearMonth ym = kini.minusMonths(i);
            long masuk = semua.stream().filter(l -> YearMonth.from(l.getWaktuLaporan()).equals(ym)).count();
            long tuntas = selesai.stream().filter(t -> YearMonth.from(t.getWaktuSelesai()).equals(ym)).count();
            String label = ym.getMonth().getDisplayName(TextStyle.SHORT, ID);
            grafik.add(new TitikBulan(label, ym.getMonth().getDisplayName(TextStyle.FULL, ID) + " " + ym.getYear(), masuk, tuntas));
        }
        long puncak = grafik.stream().mapToLong(t -> Math.max(t.masuk(), t.selesai())).max().orElse(0);
        int skala = (int) Math.max(15, Math.ceil(puncak / 15.0) * 15); // habis dibagi 3 garis bantu
        int persen = hitung.semua() == 0 ? 0 : (int) Math.round(hitung.selesai() * 100.0 / hitung.semua());
        return new RingkasanAdmin(hitung.semua(), bulanIni - bulanLalu, hitung, persen, grafik, skala,
                semua.stream().filter(l -> l.getKasus().getPrioritas() == Prioritas.KRITIS && l.getTahap() != Tahap.SELESAI)
                        .map(l -> l.getKasus().getId()).distinct().count(),
                semua.stream().limit(3).toList());
    }

    public List<KasusKerusakan> kasusSiapDitugaskan() {
        return kasus.ringkasan().stream().filter(k -> k.getStatus() == StatusKasus.MENUNGGU_PENUGASAN)
                .sorted(Comparator.comparing(KasusKerusakan::getId).reversed()).toList();
    }

    public List<BarisPetugas> daftarPetugas(String pencarian) {
        String kata = kataKunci(pencarian);
        LocalDateTime sekarang = LocalDateTime.now(jam);
        Map<Long, Long> aktif = new HashMap<>();
        tugas.semua().stream().filter(t -> t.getStatus() != StatusTugas.SELESAI)
                .forEach(t -> aktif.merge(t.getPetugas().getId(), 1L, Long::sum));
        return pengguna.petugasDemo().stream()
                .map(p -> new BarisPetugas(p, aktif.getOrDefault(p.getId(), 0L), p.dalamJamKerja(sekarang.toLocalTime())))
                .filter(b -> kata.isEmpty() || (b.petugas().getNama() + " " + b.petugas().getKeahlian().getLabel() + " "
                        + b.petugas().getKode()).toLowerCase(ID).contains(kata))
                .sorted(Comparator.comparing((BarisPetugas b) -> b.petugas().getKode()))
                .toList();
    }

    public long jumlahMenungguVerifikasi() {
        return kasus.ringkasan().stream().filter(k -> k.getStatus() == StatusKasus.MENUNGGU_VERIFIKASI)
                .mapToLong(k -> k.getDaftarLaporan().size()).sum();
    }

    // Dipakai alur lama (verifikasi petugas, tes domain).
    public List<KasusKerusakan> daftarKasus(String pencarian, StatusKasus status, Prioritas prioritas) {
        String kata = kataKunci(pencarian);
        return kasus.ringkasan().stream()
                .filter(k -> status == null || k.getStatus() == status)
                .filter(k -> prioritas == null || k.getPrioritas() == prioritas)
                .filter(k -> kata.isEmpty() || k.getDaftarLaporan().stream().anyMatch(l -> cocok(kata, l, false))
                        || ("k-" + k.getId()).equals(kata))
                .sorted(Comparator.comparing((KasusKerusakan k) -> k.getStatus() == StatusKasus.SELESAI)
                        .thenComparing(URGENSI))
                .toList();
    }

    public DetailKasus detailKasus(long id) {
        KasusKerusakan temuan = kasus.detail(id).orElseThrow(() -> new DataTidakDitemukan("Kasus tidak ditemukan."));
        return new DetailKasus(temuan, tugas.findByKasusId(id).orElse(null));
    }

    // ---------- Petugas ----------

    public List<TugasPerbaikan> tugasPetugas(long id) { return tugas.findByPetugasIdOrderByWaktuPenugasanDescIdDesc(id); }

    public RingkasanPetugas ringkasanPetugas(long petugasId) {
        List<TugasPerbaikan> semua = tugasPetugas(petugasId);
        YearMonth kini = YearMonth.now(jam);
        List<TugasPerbaikan> aktif = semua.stream().filter(t -> t.getStatus() != StatusTugas.SELESAI).toList();
        return new RingkasanPetugas(aktif.size(),
                aktif.stream().filter(t -> !t.getKasus().isSudahDiperiksa()).count(),
                aktif.stream().filter(t -> t.getStatus() == StatusTugas.DIKERJAKAN).count(),
                aktif.stream().filter(t -> t.getStatus() == StatusTugas.PERLU_TINDAK_LANJUT).count(),
                semua.size() - aktif.size(),
                semua.stream().filter(t -> t.getWaktuSelesai() != null && YearMonth.from(t.getWaktuSelesai()).equals(kini)).count(),
                aktif);
    }

    public TugasPerbaikan detailTugas(long id, long petugasId) {
        return tugas.findByIdAndPetugasId(id, petugasId)
                .orElseThrow(() -> new DataTidakDitemukan("Tugas ini tidak ditemukan pada akun petugas yang dipilih."));
    }

    // ---------- Berkas ----------

    public LaporanKerusakan laporanUntukBerkas(long id) {
        return laporan.denganKasus(id).orElseThrow(() -> new DataTidakDitemukan("Foto tidak ditemukan."));
    }

    public Optional<TugasPerbaikan> tugasKasus(long kasusId) { return tugas.findByKasusId(kasusId); }

    public TugasPerbaikan tugasUntukBerkas(long id) {
        return tugas.denganKasus(id).orElseThrow(() -> new DataTidakDitemukan("Foto tidak ditemukan."));
    }

    // ---------- Notifikasi ----------

    public List<Notifikasi> notifikasiMahasiswa(long mahasiswaId) {
        List<Notifikasi> hasil = new ArrayList<>();
        for (LaporanKerusakan l : laporan.findByPelaporIdOrderByWaktuLaporanDescIdDesc(mahasiswaId)) {
            KasusKerusakan k = l.getKasus();
            String tautan = "/mahasiswa/laporan/" + l.getId();
            if (k.getWaktuVerifikasi() != null) hasil.add(new Notifikasi(l.getKode() + " terverifikasi", l.getJudul(), k.getWaktuVerifikasi(), tautan));
            tugas.findByKasusId(k.getId()).ifPresent(t -> {
                if (t.getWaktuMulai() != null) hasil.add(new Notifikasi(l.getKode() + " sedang diperbaiki", l.getJudul(), t.getWaktuMulai(), tautan));
                if (t.getStatus() == StatusTugas.PERLU_TINDAK_LANJUT) hasil.add(new Notifikasi(l.getKode() + " perlu tindak lanjut", l.getJudul(), t.getWaktuTindakLanjut(), tautan));
                if (t.getWaktuSelesai() != null) hasil.add(new Notifikasi(l.getKode() + " selesai diperbaiki", l.getJudul(), t.getWaktuSelesai(), tautan));
            });
        }
        return terbaru(hasil);
    }

    public List<Notifikasi> notifikasiPetugas(long petugasId) {
        List<Notifikasi> hasil = new ArrayList<>();
        for (TugasPerbaikan t : tugasPetugas(petugasId)) {
            LaporanKerusakan l = t.getKasus().getLaporanUtama();
            String tautan = "/petugas/tugas/" + t.getId();
            hasil.add(new Notifikasi("Tugas baru " + l.getKode(), l.getJudul(), t.getWaktuPenugasan(), tautan));
            if (t.getWaktuKeputusan() != null && !t.getWaktuKeputusan().equals(t.getWaktuPenugasan())) {
                hasil.add(new Notifikasi("Keputusan tindak lanjut " + l.getKode(), l.getJudul(), t.getWaktuKeputusan(), tautan));
            }
        }
        return terbaru(hasil);
    }

    public List<Notifikasi> notifikasiAdmin() {
        List<Notifikasi> hasil = new ArrayList<>(laporan.semua().stream().filter(l -> l.getTahap() == Tahap.MENUNGGU)
                .map(l -> new Notifikasi("Laporan masuk " + l.getKode(), l.getJudul(), l.getWaktuLaporan(),
                        "/admin/kasus/" + l.getKasus().getId()))
                .toList());
        for (TugasPerbaikan t : tugas.semua()) {
            if (t.getStatus() != StatusTugas.PERLU_TINDAK_LANJUT) continue;
            LaporanKerusakan l = t.getKasus().getLaporanUtama();
            hasil.add(new Notifikasi("Tindak lanjut " + l.getKode(), l.getJudul(), t.getWaktuTindakLanjut(),
                    "/admin/kasus/" + t.getKasus().getId()));
        }
        return terbaru(hasil);
    }

    private List<Notifikasi> terbaru(List<Notifikasi> daftar) {
        return daftar.stream().sorted(Comparator.comparing(Notifikasi::waktu).reversed()).limit(5).toList();
    }

    // ---------- Bantuan ----------

    private String kataKunci(String pencarian) {
        return pencarian == null ? "" : pencarian.strip().toLowerCase(ID);
    }

    private boolean cocok(String kata, LaporanKerusakan l, boolean denganPelapor) {
        if (kata.isEmpty()) return true;
        String isi = l.getId() + " " + l.getKode() + " " + l.getJudul() + " " + l.getLokasiRingkas() + " "
                + l.getGedungRuang() + " " + l.getDeskripsi() + (denganPelapor ? " " + l.getPelapor().getNama() : "");
        return isi.toLowerCase(ID).contains(kata);
    }

    public record DetailLaporan(LaporanKerusakan laporan, TugasPerbaikan tugas) {}
    public record DetailKasus(KasusKerusakan kasus, TugasPerbaikan tugas) {}

    public record SaringanLaporan(String q, Tahap tahap, String tanggal, Long petugasId, int halaman, boolean kritis) {}

    public record HitungTahap(long semua, long menunggu, long terverifikasi, long ditugaskan,
                              long diperbaiki, long tindakLanjut, long selesai) {
        static HitungTahap dari(Collection<LaporanKerusakan> daftar) {
            long[] n = new long[Tahap.values().length];
            daftar.forEach(l -> n[l.getTahap().ordinal()]++);
            return new HitungTahap(daftar.size(), n[0], n[1], n[2], n[3], n[4], n[5]);
        }

        public long jumlah(Tahap tahap) {
            if (tahap == null) return semua;
            return switch (tahap) {
                case MENUNGGU -> menunggu;
                case TERVERIFIKASI -> terverifikasi;
                case DITUGASKAN -> ditugaskan;
                case DIPERBAIKI -> diperbaiki;
                case TINDAK_LANJUT -> tindakLanjut;
                case SELESAI -> selesai;
            };
        }

        public long diproses() { return terverifikasi + ditugaskan + diperbaiki + tindakLanjut; }
    }

    public record TitikBulan(String label, String labelPanjang, long masuk, long selesai) {}

    public record RingkasanAdmin(long total, long selisihBulanLalu, HitungTahap hitung, int persenSelesai,
                                 List<TitikBulan> grafik, int skalaGrafik, long kritisAktif, List<LaporanKerusakan> terbaru) {
        /** Panjang busur donut (keliling 2πr dengan r = 52). */
        public double busurSelesai() { return 2 * Math.PI * 52 * persenSelesai / 100.0; }
        public double kelilingDonut() { return 2 * Math.PI * 52; }
        /** "+13 dibanding bulan lalu" atau "−4 dibanding bulan lalu". */
        public String selisihTeks() {
            return (selisihBulanLalu >= 0 ? "+" : "−") + Math.abs(selisihBulanLalu) + " dibanding bulan lalu";
        }
    }

    public record RingkasanPetugas(long aktif, long perluDiperiksa, long diperbaiki, long tindakLanjut, long selesaiTotal, long selesaiBulanIni,
                                   List<TugasPerbaikan> tugasAktif) {}

    public record BarisPetugas(Petugas petugas, long tugasAktif, boolean dalamJamKerja) {
        public boolean bertugas() { return tugasAktif > 0; }
        public String ketersediaan() { return bertugas() ? "Bertugas" : (dalamJamKerja ? "Tersedia" : "Di luar jam kerja"); }
        public String keteranganTugas() { return tugasAktif == 0 ? "Tidak ada tugas" : tugasAktif + " tugas aktif"; }
    }

    public record Notifikasi(String judul, String isi, LocalDateTime waktu, String tautan) {}

    /** Satu halaman daftar beserta nomor halaman yang ditampilkan (0 menandai jeda "…"). */
    public record Halaman<T>(List<T> isi, int nomor, int jumlahHalaman, int total, int ukuran) {
        public static <T> Halaman<T> dari(List<T> semua, int diminta, int ukuran) {
            int jumlah = Math.max(1, (int) Math.ceil(semua.size() / (double) ukuran));
            int nomor = Math.min(Math.max(1, diminta), jumlah);
            int awal = (nomor - 1) * ukuran;
            return new Halaman<>(semua.subList(Math.min(awal, semua.size()), Math.min(awal + ukuran, semua.size())),
                    nomor, jumlah, semua.size(), ukuran);
        }

        public int awal() { return total == 0 ? 0 : (nomor - 1) * ukuran + 1; }
        public int akhir() { return Math.min(nomor * ukuran, total); }
        public boolean adaSebelumnya() { return nomor > 1; }
        public boolean adaBerikutnya() { return nomor < jumlahHalaman; }

        public List<Integer> daftarNomor() {
            TreeSet<Integer> tampil = new TreeSet<>(List.of(1, jumlahHalaman, nomor, nomor - 1, nomor + 1));
            if (nomor <= 2) tampil.add(2);
            tampil.removeIf(n -> n < 1 || n > jumlahHalaman);
            List<Integer> hasil = new ArrayList<>();
            int sebelum = 0;
            for (int n : tampil) {
                if (sebelum != 0 && n - sebelum > 1) hasil.add(0);
                hasil.add(n);
                sebelum = n;
            }
            return hasil;
        }
    }
}
