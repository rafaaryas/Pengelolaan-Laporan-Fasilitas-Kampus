# Analisis redesain UM Peduli mengikuti bahasa visual relate.so

Fase 1. Dokumen ini belum mengubah kode UI. Implementasi menunggu persetujuan, terutama jawaban untuk pertanyaan di bagian G.

Bukti gambar ada di `docs/redesain/acuan/`: 13 render layar (`layar-*.png`) dan 7 potongan relate.so (`relate-*.png`).

---

## 0. Persiapan

### 0.1 Skill yang dipakai

| Skill | Dipakai untuk | Status |
| --- | --- | --- |
| `frontend-design:frontend-design` | Arah visual, hierarki tipografi, larangan pola template (eyebrow kapital, `A · B · C`, panah di semua tombol, kartu identik), aturan spesifisitas CSS | Dipanggil dan dibaca. |
| `impeccable:impeccable` | Mode **Operate** (aplikasi kerja: keterbacaan dan konsistensi di atas ekspresi), critique dan audit di Fase 3, detektor `impeccable detect --changed` setelah UI selesai | Dipanggil. `impeccable context` berjalan dan melaporkan PRODUCT.md belum ada. Karena brief ini sudah mematok dunia visual (relate.so), saya tidak menjalankan alur `init` yang menanyakan arah dari awal. Fakta produk dirangkum di bagian A dan B dokumen ini. |
| `web-design-guidelines` | Daftar aturan aksesibilitas, fokus, form, animasi, tipografi, konten panjang, gambar. Dipakai sebagai daftar cek audit Fase 3 dengan format `file:baris` | Dipanggil, aturan dibaca. |
| `unslop` | Audit anti-AI-look per lapisan (spasi, warna, ikon, copy, tipe, layout, detail, motion) dan checklist akhir | Dipanggil dan dibaca. Dua aturannya **kalah oleh brief**: (1) unslop melarang sapaan sebagai judul, tetapi "Selamat pagi, Nadia" adalah konten layar acuan; (2) unslop membatasi bobot 400–600, tetapi headline relate.so tebal (≈700). Brief menang di keduanya. |
| `chrome-devtools-mcp:chrome-devtools` | Fase 3: screenshot 390/768/1024/1440, console, network, uji keyboard | Dipanggil. `chrome-devtools-mcp:a11y-debugging` akan dipanggil di Fase 3 untuk pohon aksesibilitas dan kontras. |

Skill yang relevan tetapi **tidak tersedia**, saya katakan terus terang:
- Tidak ada skill untuk **Spring Boot / Thymeleaf**. Saya memakai pola yang sudah ada di proyek (fragmen `th:replace`, peningkatan progresif tanpa framework JS).
- Tidak ada skill `webapp-testing`. Pengganti: Chrome DevTools MCP dan skrip Playwright yang sudah ada di `scripts/test-browser-demo.js`.
- Skill GSAP sengaja **tidak** dipakai: brief melarang library animasi berat. Semua motion ditulis dengan CSS transition + sedikit JS.

### 0.2 Alat render

`pdftoppm`/`pdfimages` tidak terpasang. Pengganti setara: PyMuPDF di venv terpisah (scratchpad sesi), zip diekstrak ke folder kosong terpisah, skrip render ada di folder lain dan dijalankan dengan `python -I`. Layar dirender 60 dpi, relate.so 72 dpi (1 px = 1 pt) lalu dipotong per 1000 px dan dilihat satu per satu. Warna diambil dengan `PIL.Image.getpixel` dan pencarian piksel tergelap/terjenuh per area.

### 0.3 Inventaris source

| Bagian | Isi |
| --- | --- |
| Stack | Java 21, Spring Boot 4.1.2, Thymeleaf, Spring Security, JPA + Flyway (MySQL, H2 untuk tes). Bukan PHP. |
| Controller | `PageController` (`/`), `LoginController` (`/login`, `/akses-ditolak`), `MahasiswaController` (`/mahasiswa/**`), `PetugasController` (`/petugas/**`), `AdminController` (`/admin/**`), `AkunGlobal` (akun di header), `KesalahanController`. |
| Model | `LaporanKerusakan` (laporan mahasiswa) bergabung ke `KasusKerusakan` (kasus per fasilitas + jenis kerusakan). `TugasPerbaikan` milik petugas. Status kasus 5 tahap, status tugas 3 tahap. Prioritas berbasis risiko dihitung dari verifikasi petugas. |
| View | 20 template di `templates/` + 4 fragmen (`common`, `kasus`, `laporan`, `petugas`). Tiga shell berbeda: mahasiswa = navigasi atas; petugas = bilah gelap + tab; admin = sidebar biru tua. |
| Aset | `static/css/tokens.css` (121 baris), `static/css/app.css` (581 baris, 4× `!important`), `static/js/app.js` (21 baris), font Public Sans + Source Serif 4 (self-host), `images/um-emblem.png`. **Inter belum ada.** |
| Login | Hanya email/NIM, tanpa sandi (mode demo). Peran ditentukan dari tabel pengguna. |
| Tes | `AlurPelaporanTest`, `LoginTest`, `PageSecurityTest`, `PrioritasBerbasisRisikoTest`, dll. + skrip browser Playwright. |

### 0.4 Selisih source dan layar acuan (ringkas, rinci di A dan G)

Source saat ini **tidak sama** dengan 13 layar. Shell, label menu, alur verifikasi, isian form, format ID, dan data contoh berbeda. Sesuai brief, layar di zip menjadi acuan konten dan flow. Masalahnya, sebagian selisih bukan soal tampilan melainkan logika backend dan skema data, yang menurut brief tidak boleh diubah tanpa persetujuan. Ini pertanyaan nomor 1 di bagian G.

---

## A. Flow aplikasi

### A.1 Flow menurut layar acuan (terverifikasi dari layar)

Bukti dari layar sendiri: kartu "Laporanmu berarti." menulis *"Admin verifikasi, petugas perbaiki."*, modal admin berjudul *"Detail dan verifikasi"* dengan tombol *"Verifikasi laporan"*, dan dropzone petugas *"Wajib dilampirkan saat status Selesai"*.

```
Civitas : Buat laporan (fakultas, gedung, lantai, ruang, deskripsi, foto)  -> "Menunggu Verifikasi"
Admin   : Verifikasi (+ pilih petugas, opsional)                           -> "Terverifikasi"
Petugas : Perbarui progres (status + catatan)                              -> "Sedang Diperbaiki"
Petugas : Tandai selesai (catatan + foto hasil wajib)                      -> "Selesai"
Civitas : Pantau di Laporan Saya / Detail (timeline 4 tahap)
```

Flow di brief **sesuai** dengan layar. Satu koreksi kecil: di layar Admin Petugas, penugasan juga bisa dilakukan terpisah lewat panel "Tugaskan laporan" (pilih laporan terverifikasi -> pilih petugas -> catatan). Jadi penugasan ada dua pintu: di modal verifikasi (opsional) dan di halaman Petugas.

### A.2 Flow menurut kode saat ini (berbeda)

```
Mahasiswa : Lapor (pilih fasilitas + jenis kerusakan + deskripsi; tanpa foto)
            -> laporan digabung ke kasus yang sama bila fasilitas+jenis sama
            -> kasus "Menunggu verifikasi"
Petugas   : Periksa di lokasi (bahaya kritis, tingkat gangguan, unit rusak, catatan)
            -> prioritas dihitung otomatis -> "Menunggu penugasan"
Admin     : Tugaskan petugas yang keahliannya cocok dan sedang jam kerja -> "Ditugaskan"
Petugas   : Mulai perbaikan -> "Sedang diperbaiki"; simpan catatan progres
Petugas   : Tandai selesai (catatan wajib, tanpa foto) -> "Selesai"
Admin     : Bisa memisahkan laporan dari kasus sebelum diverifikasi
```

Perbedaan utama: **siapa yang memverifikasi** (petugas vs admin), **unit data** (kasus gabungan vs laporan tunggal), **isian form** (fasilitas+jenis vs fakultas/gedung/lantai/ruang), **foto** (tidak ada di backend), **jumlah tahap** (5 vs 4).

### A.3 Peran, menu, route

| Peran (layar) | Label ruang | Menu (layar) | Route yang ada | Catatan |
| --- | --- | --- | --- | --- |
| Civitas | Ruang civitas | Dashboard, Buat Laporan, Laporan Saya | `/mahasiswa`, `/mahasiswa/laporan/baru`, `/mahasiswa/laporan`, `/mahasiswa/laporan/{id}` | Route cocok, hanya label beda ("Beranda", "Lapor kerusakan"). Nama route tetap. |
| Petugas | Ruang petugas | Dashboard, Tugas Saya | `/petugas`, `/petugas/tugas`, `/petugas/tugas/{id}` + **`/petugas/verifikasi`, `/petugas/kasus/{id}/verifikasi`** | Menu "Verifikasi" di kode tidak ada di layar. |
| Admin | Ruang admin | Dashboard, Laporan (badge 24), Petugas | `/admin`, `/admin/kasus`, `/admin/kasus/{id}`, `/admin/petugas` | "Laporan" = `/admin/kasus`. Detail sekarang halaman penuh, di layar berupa modal. |
| Mobile civitas | – | Bottom nav: Dashboard, Buat Laporan, Laporan Saya | sama | |

Kondisi akses (tidak berubah): `SecurityConfig` membatasi `/admin/**` ke ADMIN, `/petugas/**` ke PETUGAS, `/mahasiswa/**` ke MAHASISWA. Selain itu publik. Akses lintas peran diarahkan ke `/akses-ditolak`. Mahasiswa hanya bisa membuka laporannya sendiri (`detailLaporan(id, mahasiswaId)`), petugas hanya tugasnya sendiri (`detailTugas(id, petugasId)`).

Hak mengubah status:

| Transisi | Layar | Kode sekarang |
| --- | --- | --- |
| (baru) -> Menunggu Verifikasi | Civitas | Mahasiswa |
| Menunggu Verifikasi -> Terverifikasi | **Admin** | **Petugas** (menjadi "Menunggu penugasan") |
| Terverifikasi -> (ditugaskan) | Admin (modal atau halaman Petugas) | Admin |
| -> Sedang Diperbaiki | Petugas | Petugas (tombol "Mulai") |
| -> Selesai | Petugas, foto wajib | Petugas, catatan wajib, tanpa foto |

Data antar layar: dashboard civitas (ringkasan 4 status + 3 laporan terakhir) <- daftar Laporan Saya <- detail (timeline). Dashboard admin (statistik, grafik, donut, laporan terbaru) <- tabel Laporan <- modal verifikasi -> menulis status + petugas -> muncul di dashboard Petugas -> detail tugas menulis progres/selesai -> kembali tercermin di semua layar civitas dan admin. Badge "Laporan 24" = jumlah Menunggu Verifikasi.

Pemetaan 5 status kode ke 4 status layar (sudah dipakai `MahasiswaController`): Menunggu verifikasi -> **Menunggu Verifikasi**; Menunggu penugasan + Ditugaskan -> **Terverifikasi**; Sedang diperbaiki -> **Sedang Diperbaiki**; Selesai -> **Selesai**.

---

## B. Inventaris layar dan komponen

State berlaku untuk semua layar: **kosong** (kalimat arahan spesifik), **loading** (skeleton baris/kartu hanya untuk konten yang dimuat JS, mis. modal dan penyaringan), **error** (alert inline + pesan per kolom), **sukses** (toast setelah redirect dengan flash `sukses`).

**Shell desktop (semua peran).** Sidebar 248 px: logo UM + "UM Peduli" + "Fasilitas kampus", label ruang ("Ruang civitas" dst.), menu dengan ikon, kotak "Butuh bantuan? / Hubungi pengelola fasilitas / Pusat bantuan", profil (nama + keterangan) dan tombol keluar. Topbar: "Sistem Pelaporan Fasilitas / Universitas Negeri Malang", lonceng dengan titik notifikasi, blok pengguna (nama + peran) dengan chevron. Aksi: navigasi, keluar, buka notifikasi, buka menu pengguna.

| Layar | Tujuan | Komponen dan data | Aksi | State khusus |
| --- | --- | --- | --- | --- |
| Civitas Dashboard | Mulai lapor, lihat ringkasan | Sapaan "Selamat pagi, Nadia" + tanggal; hero "Kampus nyaman, belajar lebih tenang." + "Temukan fasilitas rusak? Sampaikan laporanmu di sini." + tombol "Laporkan Kerusakan"; kartu "Laporanmu berarti." 3 langkah; "Ringkasan laporan saya" 4 status (2/1/2/3) + "8 laporan terkirim"; "Laporan terakhir" 3 baris + "Lihat semua" | Buat laporan, buka detail, lihat semua | Kosong: ringkasan 0 dan daftar diganti ajakan membuat laporan pertama |
| Civitas Buat laporan | Kirim laporan | Form "Informasi kerusakan": Fakultas*, Gedung*, Lantai*, Ruang (opsional), Deskripsi*, Foto bukti* (dropzone JPG/PNG ≤5 MB, preview + nama + ukuran + "Ganti foto"), Batal, "Kirim Laporan"; kolom kanan: tips 3 langkah, "Laporan atas nama", panel kuning "Utamakan keselamatan" | Isi, unggah, kirim, batal | Galat per kolom, fokus ke galat pertama, tombol "Mengirim…" nonaktif |
| Civitas Laporan saya | Pantau semua laporan | Judul + "Buat laporan"; tab Semua (8) / Menunggu (2) / Terverifikasi (1) / Diperbaiki (2) / Selesai (3); cari + filter status; daftar (ID, judul, lokasi, tanggal kirim, pill, "Lihat detail"); "Menampilkan 1–4 dari 8 laporan" + Sebelumnya/Berikutnya | Tab, cari, saring, paginasi, buka detail | Kosong saat saringan: "Belum ada laporan yang cocok. Ubah kata kunci atau filter." |
| Civitas Detail laporan | Lihat isi dan progres | "Kembali ke Laporan Saya"; ID + waktu kirim; kartu detail (pill, judul, pelapor, fakultas, gedung·ruang, lantai, deskripsi, foto + nama berkas + "1 foto terlampir" + "Lihat foto" + kredit); "Perkembangan laporan" timeline 4 tahap; panel info "Laporanmu sudah kami terima" | Kembali, lihat foto | Isi panel info berubah sesuai tahap |
| Petugas Dashboard | Tahu tugas hari ini | Sapaan "Selamat pagi, Pak Budi"; 4 statistik (Tugas aktif 2, Perlu diperiksa 1, Sedang diperbaiki 1, Diselesaikan bulan ini 14); "Tugas Saya" kartu tugas (ID, judul, lokasi, pill, "Ditugaskan …", "Lihat tugas"); panel gelap "2 tugas, 1 kampus lebih baik." + bidang; "Sebelum menyelesaikan tugas" 4 langkah | Buka tugas | Kosong: "Belum ada tugas. Tugas dari admin muncul di sini." |
| Petugas Detail dan progres | Catat penanganan | "Kembali ke tugas saya"; "Detail tugas perbaikan" + "UM-0246 · Ditugaskan oleh Administrator UM pada …"; kartu detail laporan; "Perbarui progres": Status laporan (select), Catatan penanganan* (textarea), Foto hasil perbaikan (dropzone, wajib saat Selesai), "Simpan pembaruan", "Tandai selesai" | Ubah status, simpan, selesai | Selesai tanpa foto/catatan -> galat per kolom |
| Admin Dashboard | Pantau seluruh laporan | Sapaan + tanggal (kotak); 4 statistik (248 "+13 dibanding bulan lalu", 24 "Menunggu verifikasi", 38 "12 terverifikasi · 26 diperbaiki", 186 "75% dari seluruh laporan"); grafik batang masuk vs selesai per bulan + select "6 bulan terakhir"; donut 75% (186/38/24); tabel "Laporan terbaru" + "Lihat semua laporan" | Ganti rentang, buka laporan | Grafik tanpa data: teks "Belum ada laporan pada rentang ini." |
| Admin Laporan | Kelola semua laporan | Judul + "Ekspor laporan"; tab dengan hitungan (248/24/12/26/186); cari "Cari ID, pelapor, atau lokasi..."; filter status + tanggal; tabel (ID, Nama pelapor, Fakultas, Gedung, Tanggal, Status); paginasi angka "Menampilkan 1–7 dari 248 laporan" | Tab, cari, saring, paginasi, buka modal, ekspor | Kosong saringan, skeleton saat menyaring |
| Admin Detail dan verifikasi (modal) | Verifikasi + tugaskan | "Detail laporan", ID + waktu, pill, judul, grid 4 fakta, deskripsi, foto + "Lihat foto ukuran penuh" + kredit, "Tindak lanjut laporan" + "Verifikasi laporan sebelum menugaskan petugas.", select "Pilih petugas (opsional)", select status + "Status lanjutan: Sedang Diperbaiki / Selesai", "Tutup", "Verifikasi laporan" | Tutup, verifikasi | Laporan sudah terverifikasi: tombol menjadi penugasan saja |
| Admin Petugas | Atur penugasan | Judul + "Tambah petugas"; 3 statistik (12 "Tim fasilitas", 8 "Siap ditugaskan", 4 "Tugas aktif"); daftar petugas (nama, PTG-001, bidang, jumlah tugas, Bertugas/Tersedia, chevron) + cari "Cari nama atau bidang..." + "Menampilkan 5 dari 12 petugas"; panel "Tugaskan laporan" (select laporan, ringkasan, select petugas, catatan, "Tugaskan petugas") | Cari, pilih, tugaskan | Tidak ada laporan terverifikasi: select nonaktif + kalimat arahan |
| Mobile (civitas) | Sama, satu kolom | Header (logo + "Universitas Negeri Malang" + lonceng), statistik 2×2, kartu laporan, bottom nav 3 tab dengan garis indikator atas | Sama | Copy mobile sedikit beda dari desktop (lihat G.1) |

Halaman di kode yang **tidak ada** di layar: landing `/`, `/login`, `masuk-berhasil`, `akses-ditolak`, `error`, petugas `verifikasi` + `periksa`, admin `detail` halaman penuh. Akan diberi tema yang sama tanpa menambah section.

---

## C. Analisis tema relate.so (nilai hasil sampling)

### C.1 Warna

| Peran | Nilai sampling | Lokasi sampling | Token usulan | Catatan kontras |
| --- | --- | --- | --- | --- |
| Kanvas | `#fbfbfb` | (10,150) | `--canvas: #fbfbfb` | |
| Permukaan kartu | `#ffffff` | kartu kanban, kartu fitur | `--surface: #ffffff` | |
| Permukaan 2 (header tabel, sidebar app) | `#f9f9fb` | header tabel Retain, sidebar app | `--surface-2: #f9f9fb` | |
| Garis | `#f0f0f0` (kartu), `#e8e8e9` (baris) | tepi kartu, garis baris tabel | `--line: #ececef`, `--line-strong: #e2e2e6` | |
| Sidebar aktif | `#e4e6eb` | pill "Pipeline" | `--nav-active: #e9eaee` | pill abu, bukan biru |
| Teks utama | `#13131d` | headline hero | `--ink: #14141e` | 17,7:1 di kanvas |
| Teks sidebar/isi | `#364150` | label menu app | `--ink-2: #364150` | 10,0:1 |
| Teks sekunder | `#696972` / `#6f6f75` | meta kartu, "Backed by" | `--ink-3: #6b6b75` | 5,27:1 di putih, 5,09:1 di kanvas: lulus AA |
| Abu headline | `#87878c` | baris "in good hands" | `--ink-display-2: #87878c` | 3,45:1. **Hanya untuk teks ≥24 px tebal** (ambang 3:1 teks besar). Tidak untuk teks kecil. |
| Aksen (tombol) | `#1569ff`–`#1787ff` | CTA nav | `--accent: #1569ff`, `--accent-hover: #1756e0` | putih di atasnya 4,67:1 (lulus) |
| Aksen sebagai teks | `#1055e6` | teks CTA sekunder | `--accent-ink: #1055e6` | 6,06:1 di putih |
| Hero gradasi | `#fbfbfb` -> `#d5e2fb` -> `#8eb3fb`/`#a5c0f9` | (720,200), (100,600), (100,780) | `--hero-top/-mid/-low` | Grid tipis putih ±30% opacity, sel ≈56 px |
| Panel navy | `#11203a` (tepi) -> `#000714`/`#010816` (tengah) -> `#0d1d39` | footer | `--navy-edge: #0e1d3a`, `--navy-core: #050f22` | putih 19:1, teks link `#a7b0c0` 8,2:1 |
| Kolom biru | bg `#f4f9ff`, titik `#1890ff` | header "Potential/Pending" | status Terverifikasi | |
| Kolom hijau | bg `#f4fcf5`, titik `#16ca2d` | header "Closed Won" | status Selesai | teks hijau relate `#16ca2d` hanya 2,11:1 -> teks digelapkan ke `#127a2c` (5,17:1), titik tetap terang |
| Kolom merah | bg `#fef8f9`, titik `#f26052` | header "Lost", pill "Churned" | galat/bahaya | teks merah relate 3,05:1 -> `#b3261e` (6,14:1) |
| Pill netral | bg `#ebebee`, teks `#696972` | "in 3 months" | status netral | |
| Garis putus penghubung | `#2194fe` | garis vertikal di atas "Close." | timeline | |

Status UM Peduli (4 tahap + bahaya), semua lulus AA dan selalu dengan teks:

| Status | Latar | Teks | Titik |
| --- | --- | --- | --- |
| Menunggu Verifikasi | `#fff7e6` | `#8a5a00` (5,56:1) | `#f0a020` |
| Terverifikasi | `#eef4ff` | `#1d4fc4` (6,41:1) | `#1890ff` |
| Sedang Diperbaiki | `#f1f0ff` | `#4a3fb0` (7,09:1) | `#6c5ce7` |
| Selesai | `#f2fbf4` | `#127a2c` (5,17:1) | `#16ca2d` |
| Bahaya / galat | `#fef6f5` | `#b3261e` (6,14:1) | `#f26052` |

Catatan palet: layar acuan memakai lavender untuk "Sedang Diperbaiki". Saya pertahankan karena itu bahasa status yang sudah disetujui, tetapi hanya di pill/titik, tidak pernah di permukaan atau gradasi, jadi tidak jatuh ke tren "ungu-biru".

### C.2 Tipografi

Relate memakai Inter (bentuk glyph dan angka cocok). Headline dua nada: baris hitam `#14141e` + baris abu `#87878c`, tebal, tracking rapat. Subjudul abu sedang dengan lebar baris sempit (±40 karakter).

Skala untuk aplikasi kerja (lebih kecil dari landing relate yang 76 px):

| Peran | Ukuran / line-height | Bobot | Tracking |
| --- | --- | --- | --- |
| Hero (dua nada) | 40/44 desktop, 28/32 mobile | 700 | -0.03em |
| H1 halaman | 30/36 | 650 | -0.02em |
| Angka statistik | 34/40, `tabular-nums` | 650 | -0.02em |
| H2 judul kartu | 18/26 | 600 | -0.01em |
| H3 / judul baris | 15/22 | 600 | 0 |
| Isi | 15/24 | 400 | 0 |
| Sekunder / meta | 13/20 | 400–500 | 0 |
| Label kecil, kepala tabel | 12/16 | 500 | 0 (tidak pernah negatif, tidak kapital) |

Font: **Inter variable, self-host woff2, `font-display: swap`**, subset latin, dengan lisensi OFL ikut disimpan. Public Sans dan Source Serif 4 dilepas dari halaman.

### C.3 Spasi, radius, bayangan

- Spasi grid 4 px: 4, 8, 12, 16, 20, 24, 32, 40, 48, 64. Padding: kontrol 12–16, kartu pendukung 20, kartu utama 24, hero 40, tepi halaman 32 (desktop) / 16 (mobile).
- Radius (berbeda per tingkat, disengaja):

| Elemen | Radius | Bukti relate |
| --- | --- | --- |
| Hero, panel navy | 28 px | hero ±32 px, footer ±28 px |
| Kartu utama (form, detail, tabel wadah) | 20 px | bingkai app, kartu fitur ±20 px |
| Kartu pendukung (statistik, tips, kartu tugas) | 14 px | kartu kanban ±10–12 px |
| Kontrol (input, select, dropzone) | 10 px | kotak search ±8 px |
| Tombol | 999 px (pill) | CTA pill |
| Pill status | 999 px | "Customer", "Churned" |

- Bayangan (netral, lebar, sangat halus):
  - `--shadow-card: 0 1px 2px rgb(20 20 30 / .04), 0 8px 24px -12px rgb(20 20 30 / .08)` kartu pendukung
  - `--shadow-panel: 0 1px 2px rgb(20 20 30 / .04), 0 24px 48px -24px rgb(20 30 60 / .12)` kartu utama
  - `--shadow-float: 0 32px 80px -24px rgb(20 40 90 / .22)` modal, popover
  - `--shadow-pill: 0 1px 2px rgb(20 20 30 / .06), 0 4px 12px rgb(20 20 30 / .08)` tombol sekunder putih (seperti "Get early access" di hero)

Bukti: `relate-hero.png`, `relate-kolom-status.png`, `relate-sidebar-aktif.png`, `relate-garis-putus.png`, `relate-tabel-pill.png`, `relate-kartu-fitur.png`, `relate-panel-navy.png`.

---

## D. Pemetaan komponen

| Komponen UM Peduli | Padanan relate.so | Keputusan |
| --- | --- | --- |
| Panel hero Civitas | Hero gradasi putih -> biru + grid | Panel 28 px, gradasi `#fbfbfb -> #d5e2fb -> #8eb3fb` dari atas ke bawah, grid tipis. Headline dua nada: "Kampus nyaman," hitam / "belajar lebih tenang." abu. Tombol "Laporkan Kerusakan" pill biru. Angka "01" dan eyebrow "BERSAMA MERAWAT KAMPUS" serta "LAPOR · PANTAU · SELESAI" dihapus. **Satu-satunya gradasi di aplikasi.** |
| Kartu "Laporanmu berarti." | Kartu fitur putih | Kartu pendukung; 3 langkah tetap bernomor (memang urutan). |
| Kartu statistik | Header kolom kanban (titik + nama + hitungan) | Bukan 4 kartu identik dengan ikon dalam kotak tint. Satu strip putih dibagi 4 sel bergaris tipis; tiap sel: titik status berwarna + label + angka besar. Di Admin, kartu "Laporan masuk" (yang menuntut aksi) diberi tint kolom kuning. Di Petugas, ikon panah dihapus. |
| Pill status | "Customer" / "Churned" | Pill tint + teks pekat + titik kecil. |
| Timeline 4 tahap | Garis putus vertikal biru | Garis putus `#2194fe` menghubungkan tahap; tahap lewat = lingkaran centang, tahap aktif = lingkaran aksen penuh + teks tebal, tahap belum = lingkaran garis abu. |
| Panel "2 tugas, 1 kampus lebih baik." (Petugas) | Footer navy membulat | Panel navy 28 px, **satu-satunya blok gelap**. Eyebrow "FOKUS HARI INI" dihapus (tidak fungsional). |
| Tabel admin | Tabel Retain/Recycle | Header `#f9f9fb` 12 px abu, baris 52 px bergaris `#ececef`, hover baris abu sangat muda, ID biru. |
| Sidebar aktif | Pill "Pipelines" abu | Pill `#e9eaee`, teks `#14141e` 600. Garis kiri biru dan tint biru dihapus. |
| Tab (Laporan saya, Admin Laporan) | Header kolom (nama + hitungan abu) | Label + hitungan abu `tabular-nums`; indikator garis bawah 2 px aksen yang bergeser. |
| Tombol utama | CTA pill biru | Pill `#1569ff`, teks putih, tinggi 44 px. |
| Tombol sekunder | Pill putih dengan bayangan lembut | Pill putih + `--shadow-pill`. |
| Input, select, search | Kotak "Search …" | Radius 10, garis `#e2e2e6`, tinggi 44, ikon kaca pembesar. |
| Dropzone | (tidak ada) | Garis putus abu radius 10, mengikuti garis putus relate. |
| Panel "Utamakan keselamatan" | Kolom "Lost" | Tint kuning hangat (bukan merah, karena isinya himbauan, bukan galat). |
| Panel info "Laporanmu sudah kami terima" | Kolom "Potential" | Tint biru `#f4f9ff`. |
| Grafik batang + donut | Grafik "Total Revenue" | Grid putus abu, label abu 12 px. Seri "Masuk" biru aksen, "Selesai" hijau `#16ca2d`. Donut satu warna hijau di atas lintasan abu. |
| Modal verifikasi | Bingkai app mengambang | Radius 20, `--shadow-float`, latar gelap 40%. |
| Kotak "Butuh bantuan?" | Kartu kecil di sidebar | Permukaan `#f4f4f6` tanpa bayangan. |

**Yang TIDAK dibawa dari relate.so**: relate adalah landing page pemasaran, UM Peduli aplikasi kerja. Tidak ada section marketing baru, logo klien, testimoni, emoji hati, footer promosi dengan form email, headline 76 px, atau "Backed by". Navigasi atas relate tidak dipakai; shell tetap sidebar sesuai layar.

---

## E. Rencana motion

Semua hanya `transform` dan `opacity`. Satu kurva easing `cubic-bezier(.2,.7,.2,1)`. Di bawah `prefers-reduced-motion: reduce` semua durasi jadi 0 dan perubahan tampil instan.

| Animasi | Pemicu | Durasi | Properti | Alasan |
| --- | --- | --- | --- | --- |
| Fade konten halaman | Klik tautan navigasi | 140 ms | opacity `main` | Menandai halaman berganti setelah klik. Pakai View Transitions bila didukung, selain itu fade masuk `main`. |
| Batang tumbuh | Dashboard Admin pertama dibuka per sesi (`sessionStorage`, dibungkus try/catch) | 480 ms, stagger 40 ms | `transform: scaleY` dari dasar | Menunjukkan bahwa batang adalah jumlah. Sekali saja. |
| Donut tersapu | Sama | 600 ms | `stroke-dashoffset` diganti `transform: rotate` pada mask, atau langsung tampil bila tidak mungkin tanpa properti lain | Membaca 75% sebagai porsi. |
| Modal | Klik baris/aksi | 160 ms | `scale .98 -> 1` + opacity | Asal modal terlihat. |
| Bottom sheet (<720 px) | Sama | 200 ms | `translateY(100%) -> 0` | Pola mobile untuk modal. |
| Toast | Setelah aksi sukses (flash) | masuk 180 ms, hilang otomatis 4 s, bisa ditutup | translate + opacity | Konfirmasi aksi. `role="status"`. |
| Pill status berkedip tint | Status berubah (deteksi lewat flash setelah aksi) | 1× 600 ms | opacity lapisan tint | Menunjukkan apa yang berubah. |
| Langkah timeline terisi | Detail dibuka setelah status berubah | 300 ms | `scaleY` garis | Sama. |
| Indikator tab | Ganti tab | 180 ms | `translateX` + `scaleX` | Menunjukkan tab aktif. |
| Hover | Tombol, tautan, baris tabel | 120 ms | warna/latar/bayangan (lewat opacity pseudo-elemen) | Hanya elemen yang bisa diklik. Tanpa lift. |
| Popover lonceng / menu pengguna | Klik | 140 ms | translateY 4 px + opacity | Asal menu. |

Tidak ada animasi otomatis lain, tidak ada fade-up per section, tidak ada hover-lift kartu, tidak ada loop.

---

## F. Rencana responsif

| Lebar | Shell | Konten |
| --- | --- | --- |
| ≥1200 | Sidebar 248 px + topbar 72 px | Grid 12 kolom, gutter 24, maks 1200 px konten. Kolom kanan 4/12. |
| 900–1199 | Sidebar ikon saja 76 px (label jadi tooltip + `aria-label`), kotak bantuan disembunyikan | Kolom kanan turun ke bawah bila < 1024. |
| <900 | Sidebar menjadi bottom nav (Civitas 3, Petugas 2, Admin 3) dengan garis indikator atas pada tab aktif + `aria-current`; topbar ringkas (logo + "Universitas Negeri Malang" + lonceng + avatar) | Satu kolom. |
| <720 | sama | Tabel -> daftar bertumpuk berlabel (`data-label`); statistik 2×2; modal -> bottom sheet; tombol utama lebar penuh di zona jempol (form: tombol kirim di akhir form, lebar penuh); filter dipadatkan jadi 2 select seperti layar mobile; tab Civitas jadi select "Semua (8)". |

Target sentuh ≥44 px (bottom nav 64 px tinggi). `body` tanpa scroll horizontal; tabel di 720–900 pakai wadah scroll sendiri berlabel. Teks panjang: judul laporan 2 baris lalu ellipsis (`line-clamp`), nama/lokasi di tabel 1 baris ellipsis + `title`, deskripsi pengguna `overflow-wrap: anywhere`. Flex child diberi `min-width: 0`. Uji di 390, 768, 1024, 1440.

---

## G. Temuan, risiko, pertanyaan

### G.1 Inkonsistensi di layar acuan (usulan, tidak diubah diam-diam)

1. **"Laporan terakhir" Civitas melompati UM-0229** (14 Nov, Terverifikasi) dan menampilkan UM-0218 (10 Nov). Usul: tampilkan 3 terbaru dari data (UM-0248, UM-0235, UM-0229).
2. **Grafik**: brief menyebut label "8 bulan terakhir", tetapi layar menulis **"6 bulan terakhir"** dan menampilkan 6 bulan. Layarnya konsisten; tidak ada yang perlu diperbaiki. Nilai diukur dari tinggi batang pada render 144 dpi (garis dasar y=598,5; 20 unit = 49 px): Jun 22/16, Jul 32/24, Agu 27/21, Sep 43/33, Okt 35/29, Nov 48/39. Jul selesai terbaca **24**, bukan 23. Total selesai 6 bulan = 162, total masuk = 207; tidak harus sama dengan 248/186 karena 248 mencakup semua waktu.
3. **Badge "Laporan 24"** konsisten dengan tab "Menunggu verifikasi 24" dan statistik "Laporan masuk 24". Angka lain juga cocok: 186+38+24 = 248, 12+26 = 38, 186/248 = 75%, Nov 48 − Okt 35 = +13. Badge akan dihitung dari data, bukan ditulis tetap.
4. **UM-0247 ganda**: di Admin Petugas, UM-0247 masih "Terverifikasi" dan baru akan ditugaskan ke Budi, tetapi di Dashboard Petugas UM-0247 sudah tampil sebagai tugas Budi ("Ditugaskan 20 Nov 2026, 09.15"). Usul: dashboard petugas memakai data nyata, jadi hanya salah satu keadaan yang benar.
5. **Modal admin, select status** menawarkan "Status lanjutan: Sedang Diperbaiki / Selesai". Itu bertentangan dengan flow (Diperbaiki dan Selesai milik petugas, Selesai wajib foto). Usul: select hanya berisi "Menunggu Verifikasi" dan "Terverifikasi"; kalimat bantuan menjadi "Status berikutnya diperbarui petugas."
6. **Kapitalisasi status** tidak seragam: pill "Menunggu Verifikasi" (Title Case), tab admin "Menunggu verifikasi", tab civitas "Diperbaiki". Usul: ikuti layar apa adanya per tempat (pill Title Case, tab sesuai layar).
7. **Copy mobile beda dari desktop**: "Mari rawat kampus kita bersama." vs "Terima kasih sudah ikut merawat fasilitas kampus kita."; "Temukan kerusakan? Laporkan di sini." vs "Temukan fasilitas rusak? Sampaikan laporanmu di sini."; "Laporan saya" vs "Ringkasan laporan saya"; "Semua" vs "Lihat semua". Keputusan saya: tampilkan versi sesuai breakpoint, karena keduanya layar yang disetujui.
8. **Sapaan dan tanggal** tertulis "Selamat pagi" dan "20 November 2026". Keputusan saya: sapaan mengikuti jam kampus WIB (pagi/siang/sore/malam) dan tanggal hari ini.
9. **Paginasi admin** "1–7 dari 248" hanya menampilkan halaman 1 dan 2, padahal ada 36 halaman. Usul: angka halaman diringkas (1, 2, …, 36).
10. **Bidang petugas** di layar (Kelistrikan & AC, Bangunan & sipil, Jaringan & perangkat) tidak ada di enum `Keahlian` (Pendingin & ventilasi, Kelistrikan, Perabot & furnitur, Air & sanitasi).
11. Teks layar memakai `...` pada placeholder; akan ditulis dengan elipsis `…` (tipografi, isi sama).

### G.2 Risiko teknis

- **Backend tidak mendukung sebagian besar isi layar** (lihat pertanyaan 1): foto upload, fakultas/lantai, verifikasi oleh admin, statistik per bulan, ekspor, tambah petugas, ID `UM-0248` empat digit, 248 data.
- **Data demo** sekarang Andi/Budi/Sinta dan "AC B13". Data contoh layar (Nadia Putri, 248 laporan, 12 petugas) butuh migrasi Flyway baru. Mengubah V2 yang sudah dipakai akan merusak checksum Flyway; harus V4 baru.
- **Tes** `AlurPelaporanTest`, `LoginTest`, skrip Playwright mencari teks lama ("Keluar", "Email kampus atau NIM", "Periksa kasus"). Perubahan label/alur akan memerlukan pembaruan tes.
- **CSS lama** (`app.css` 581 baris, 4× `!important`, tiga shell berbeda) akan diganti, bukan ditambal, agar tidak ada konflik spesifisitas. Token tetap satu berkas `tokens.css`.
- **Inter** belum ada di repo. Perlu mengunduh woff2 resmi (rsms/inter, OFL) sekali dan menyimpannya di `static/fonts/`.
- **Modal dari halaman detail**: `/admin/kasus/{id}` tetap ada sebagai halaman penuh (tanpa JS); modal memuat isinya lewat `fetch`. CSRF token sudah ada di form Thymeleaf.
- **Unggah foto** butuh `multipart` + penyimpanan berkas (folder lokal di luar `static`) + validasi tipe/ukuran di server, bukan hanya di browser.

### G.3 Pertanyaan yang perlu jawabanmu

1. **Lingkup backend.** Layar menuntut perubahan logika dan skema, sementara brief melarangnya tanpa persetujuan. Pilihan:
   - **(a) Sesuaikan backend secukupnya agar layar jalan nyata** (usulan saya): verifikasi pindah ke admin, langkah pemeriksaan risiko oleh petugas dan penggabungan kasus disembunyikan dari UI (logika tetap di model), tambah kolom fakultas, lantai, foto bukti, foto hasil (migrasi V4), unggah foto multipart, ekspor CSV, statistik per bulan. Route lama tetap, route baru hanya ditambah.
   - **(b) Tema saja di atas backend sekarang**: layar mengikuti isi yang didukung kode (fasilitas + jenis kerusakan, verifikasi oleh petugas, tanpa foto, tanpa grafik). Banyak bagian layar acuan tidak akan muncul.
2. **Data contoh.** Boleh saya tambahkan migrasi data demo V4 berisi data persis layar (Nadia Putri, UM-0218 s.d. UM-0248, 12 petugas, total 248 laporan dengan sebaran per bulan sesuai grafik), dan akun masuk untuk Nadia, Budi Santoso, Administrator? Data demo lama (Andi, AC B13) dihapus atau dibiarkan?
3. **Bidang petugas.** Ganti enum `Keahlian` menjadi 5 bidang di layar (Kelistrikan & AC, Bangunan & sipil, Air & sanitasi, Perabot & furnitur, Jaringan & perangkat)?
4. **"Tambah petugas" dan "Ekspor laporan".** Buat berfungsi (form tambah petugas sederhana; ekspor CSV sesuai saringan), atau tampil nonaktif dengan tooltip "Belum tersedia"?
5. **"Ganti peran" di menu pengguna.** Login di aplikasi ini per email, jadi ganti peran = keluar lalu masuk dengan akun lain. Usul: menu berisi "Masuk dengan akun lain" (keluar lalu ke `/login`) dan "Keluar". Setuju?

Keputusan yang saya ambil sendiri (tidak perlu dijawab kecuali tidak setuju): copy per breakpoint (G.1 no. 7), sapaan dinamis (no. 8), paginasi ringkas (no. 9), select status modal dibatasi (no. 5), gradasi hanya di hero Civitas dan navy hanya di panel Petugas, landing dan login diberi tema tanpa section baru, ikon tetap sprite SVG yang sudah ada.
