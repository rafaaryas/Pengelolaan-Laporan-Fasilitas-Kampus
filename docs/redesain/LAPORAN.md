# Laporan akhir redesain UM Peduli (tema relate.so)

Tanggal: 24 November 2026. Analisis dan keputusan: `ANALISIS.md`. Kelima usulan di G.3 disetujui pengguna.

## Hasil verifikasi

| Pemeriksaan | Hasil | Bukti |
| --- | --- | --- |
| Tes Maven (JUnit, H2) | 51/51 lulus, termasuk tes baru alur layar + hak akses foto | `mvn test` |
| Alur penuh di browser (Chrome): civitas lapor + foto → admin verifikasi + tugaskan lewat modal → petugas simpan pembaruan → petugas tandai selesai (tanpa foto ditolak, dengan foto lulus) → civitas melihat timeline 4 tahap | Lulus, 10,5 s; badge, statistik, tab, dan notifikasi ikut berubah | `scripts/test-browser-demo.js` |
| Scroll horizontal, 14 halaman × 390/768/1024/1440 px (4 peran) | 0 luapan pada 56 kombinasi | `hasil/cek-responsif.txt` |
| Konsol dan jaringan | Tidak ada error/warning baru, tidak ada aset gagal (satu-satunya 404 adalah uji sengaja `/mahasiswa/laporan/999999`) | skrip responsif + uji alur |
| Struktur heading | Tepat satu `h1` per halaman; temuan `skipped-heading` di halaman pemeriksaan diperbaiki | `cek-responsif.txt`, detektor Impeccable |
| Keyboard | Menu akun: Enter membuka, panah atas/bawah, Esc menutup dan fokus kembali; tab: panah kiri/kanan; modal: fokus terkunci (`<dialog>`), Esc/latar/Tutup menutup, fokus kembali ke pemicu; validasi memindah fokus ke galat pertama | Chrome DevTools |
| Reduced motion | Semua animasi dan transisi mati (dicek nilai terhitung `animation-name: none`, `transition-duration: 0s`) | Chrome DevTools |
| Kontras | 30 pasangan warna lulus; placeholder (3,66 → 4,98) dan batas input (1,29 → 3,17) diperbaiki | bagian Kontras di bawah |
| Target sentuh < 900 px | ≥ 44 px, kecuali tautan judul kartu yang area kliknya seluruh kartu (`::after`) | `cek-responsif.txt` |

Kontras utama: teks 17,7:1; sekunder `#6b6b75` 4,76–5,09:1; abu headline pada gradasi hero 3,19:1 (teks 40 px tebal, ambang 3:1); tombol utama 4,67:1; pill 5,17–7,09:1; galat 6,14:1; navy 19,1:1 dan 7,66:1; batang grafik "Selesai" 3,85:1 (grafis ≥ 3:1).

## Audit "AI slop" (7 poin)

1. **Dekorasi tanpa fungsi:** tidak ada blob, glow, partikel, atau emoji. Emoji 👋 dihapus dari sapaan. Angka raksasa "01" di hero dihapus. Gradasi + grid hanya di `.hero` (`app.css`, `.hero::before`), dan navy hanya di `.panel-navy` (dashboard petugas).
2. **Kartu identik dan ikon dalam kotak tint:** tiga tingkat bobot dengan radius/padding/bayangan berbeda (28/20/14/10, lihat `DESIGN_SYSTEM.md`). Statistik menjadi satu strip dengan titik status ala header kolom relate, tanpa ikon berkotak. Daftar tugas berupa baris bergaris, bukan kartu di dalam kartu.
3. **Eyebrow kapital dan penomoran palsu:** "BERSAMA MERAWAT KAMPUS", "FOKUS HARI INI", "LAPOR · PANTAU · SELESAI" dihapus. "RUANG CIVITAS" menjadi "Ruang civitas" dan berfungsi sebagai label navigasi (`aria-labelledby`). `grep uppercase app.css` = 0. Angka hanya dipakai untuk urutan nyata: 3 langkah "Laporanmu berarti.", 4 langkah "Sebelum menyelesaikan tugas", dan timeline 4 tahap. Tips form ("01/02/03" di layar) dibuat tanpa nomor.
4. **Animasi berlebih:** tidak ada fade-up per section, hover-lift, parallax, atau loop. Gerak hanya yang tercantum di brief (lihat `DESIGN_SYSTEM.md`, bagian Gerak), semuanya `transform`/`opacity`.
5. **Copy generik:** teks layar dipertahankan. Copy baru hanya untuk state yang tidak ada di layar, ditulis spesifik, misalnya "Belum ada laporan yang cocok. Ubah kata kunci atau filter.", "Foto maksimal 5 MB, format JPG atau PNG. Pilih foto yang lebih kecil.", "Unggah foto hasil perbaikan sebelum menandai selesai.", dan panel info per tahap.
6. **Palet tren dan kontras abu:** netral + satu aksen biru dari sampling relate. Lavender hanya untuk pill "Sedang Diperbaiki" (warna status dari layar), tidak pernah untuk permukaan. Kontras di atas.
7. **Tanda baca template:** `A · B · C` hanya muncul di data yang memang begitu di layar ("Teknik · B11 · Lantai 2"). Tidak ada label "KATA — fragmen". Panah hanya pada tautan yang pindah halaman ("Lihat semua", "Lihat detail", "Lihat tugas", "Pusat bantuan"). Panah pada tombol aksi "Kirim Laporan →" dan "Tugaskan petugas →" di layar sengaja dihapus.

Detektor Impeccable: `overused-font` (Inter) dan `codex-grid-background` (grid hero) dikecualikan secara sempit karena dipatok brief (`.impeccable/config.json`, alasan tercatat). `flat-type-hierarchy` (16 temuan) dibiarkan sebagai positif palsu: detektor membaca template tanpa stylesheet sehingga semua heading terbaca 16 px, padahal ukuran nyatanya h1 30, h2 18, dan isi 15.

## Berkas

**Ditambah:** `model/Tahap`, `Lokasi`, `Berkas`, `KatalogLokasi`; `service/PenyimpananBerkas`, `WaktuKampus`; `controller/BerkasController`; `db/migration/V4__alur_layar.sql`; `db/demo/V100__data_demo_layar.sql`; `demo-unggahan/ac-ruang.png`; `templates/fragments/shell.html`, `unggah.html`, `admin.html`, `publik.html`; `templates/bantuan.html`; font Inter + `OFL-inter.txt`; `scripts/buat-data-demo.py`; `ANALISIS.md`; `docs/redesain/`.

**Diubah:** model (`LaporanKerusakan`, `KasusKerusakan`, `TugasPerbaikan`, `Petugas`, `Mahasiswa`, `Admin`, `Keahlian`), repositori, `SistemPelaporan`, `PantauanPelaporan`, semua controller, `SecurityConfig` (`/berkas/**` wajib masuk, keluar ke `/login` untuk "Masuk dengan akun lain"), `application.properties`, semua template, `tokens.css`, `app.css`, `app.js`, tes (`AlurPelaporanTest`, `LoginTest`, `PageSecurityTest`), `scripts/test-browser-demo.js`, README, dokumen desain.

**Dihapus:** `fragments/common.html`, `fragments/kasus.html`, font Public Sans dan Source Serif 4.

**Sengaja tidak diubah:** nama route lama, login email/NIM, CSRF/sesi/pembatas percobaan, strategi prioritas berbasis risiko dan penggabungan kasus (tetap di model dan tes, tidak tampil di menu), migrasi V1–V3 (agar checksum Flyway tetap sah).

## Deviasi dari layar acuan

| Layar | Deviasi | Alasan |
| --- | --- | --- |
| Petugas Dashboard | "Selamat pagi, Budi", bukan "Pak Budi" | Sapaan bergender tidak ditebak dari nama (petugas lain: Siti, Dewi, Rina) |
| Semua sapaan dan tanggal | Mengikuti jam WIB dan tanggal hari ini | Data nyata, bukan teks tetap |
| Civitas Dashboard | "Laporan terakhir" menampilkan 3 laporan terbaru (UM-0248, UM-0235, UM-0229) | Layar melompati UM-0229 (G.1 no. 1) |
| Admin Petugas | Panel "Tugaskan laporan" hanya berisi laporan terverifikasi yang belum ditugaskan; UM-0247 sudah ditugaskan ke Budi | Layar memuat dua keadaan UM-0247 sekaligus (G.1 no. 4) |
| Modal verifikasi | Pilihan status hanya Menunggu Verifikasi/Terverifikasi | Diperbaiki dan Selesai milik petugas, Selesai wajib foto (G.1 no. 5) |
| Admin Laporan | Paginasi "1, 2, …, 36" | 248 laporan dengan 7 per halaman |
| Laporan saya (ponsel) | Satu pilihan status berhitungan, bukan dua dropdown | Kedua dropdown di layar menyaring hal yang sama |
| Buat laporan | Tidak ada kolom judul; judul diambil dari kalimat pertama deskripsi | Layar tidak punya kolom judul, tetapi daftar menampilkan judul |
| Buat laporan | "1 foto · 119 KB · Siap dikirim" alih-alih "Berhasil diunggah" | Foto baru terunggah saat laporan dikirim |
| Topbar | Nama sesuai data akun ("Administrator UM") | Layar menulis "Administrator" di topbar dan "Administrator UM" di sidebar |
| Batas input | Garis `#909098`, lebih tegas daripada relate | Kontras komponen ≥ 3:1 (WCAG 1.4.11) |
| Data foto | Laporan demo tanpa foto menampilkan "Laporan ini dikirim tanpa foto bukti." | Foto contoh hanya ada untuk dua laporan AC di layar; foto tidak dikarang |

## Menjalankan

```powershell
.\scripts\dev.ps1                 # aplikasi di http://127.0.0.1:8080 (MySQL dari .env, migrasi + data demo otomatis)
.\scripts\dev.ps1 -Task Test      # 51 tes
playwright-cli -s=kampus-qa run-code --filename=./scripts/test-browser-demo.js   # alur penuh (default 127.0.0.1:8081)
```

Akun demo: `nadia@example.invalid` (civitas), `budi.santoso@example.invalid` (petugas), `admin@example.invalid` (admin).
