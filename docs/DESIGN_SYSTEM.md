# Sistem desain UM Peduli

Bahasa visual mengikuti referensi relate.so (sampling piksel, lihat `ANALISIS.md` bagian C), diterapkan untuk aplikasi kerja, bukan landing page. Semua nilai ada di `src/main/resources/static/css/tokens.css`; `app.css` hanya membaca variabel dari berkas itu.

## Warna

| Token | Nilai | Peran |
| --- | --- | --- |
| `--canvas` / `--surface` / `--surface-2` / `--surface-3` | `#fbfbfb` / `#ffffff` / `#f9f9fb` / `#f3f3f6` | Kanvas halaman, kartu, sidebar dan kepala tabel, kotak bantuan |
| `--ink` / `--ink-2` / `--ink-3` | `#14141e` / `#364150` / `#6b6b75` | Teks utama, teks isi/menu, teks sekunder (5,0:1 pada kanvas) |
| `--ink-display-2` | `#87878c` | Baris kedua headline dua nada. **Hanya teks ≥ 22 px tebal** (3,2–3,45:1) |
| `--accent` / `--accent-ink` | `#1569ff` / `#1055e6` | Satu aksen: tombol utama, tautan, tab dan menu aktif di bottom nav |
| `--line` / `--line-strong` / `--line-kontrol` | `#ececef` / `#e2e2e6` / `#909098` | Garis kartu, tombol sekunder, batas input (3,17:1, WCAG 1.4.11) |
| `--hero-*` | `#fbfbfb → #dfe8fb → #a9c3f9` + grid putih 45% | **Satu-satunya gradasi**: hero dashboard civitas (dan hero landing publik) |
| `--navy-edge` / `--navy-core` | `#0e1d3a` / `#050f22` | **Satu-satunya blok gelap**: panel fokus dashboard petugas |

Status selalu berupa teks di dalam pill bertint (tidak bergantung pada warna):

| Tahap | Latar | Teks (kontras) | Titik |
| --- | --- | --- | --- |
| Menunggu Verifikasi | `#fff7e6` | `#8a5a00` (5,56) | `#f0a020` |
| Terverifikasi | `#eef4ff` | `#1d4fc4` (6,41) | `#1890ff` |
| Sedang Diperbaiki | `#f1f0ff` | `#4a3fb0` (7,09) | `#6c5ce7` |
| Selesai | `#f2fbf4` | `#127a2c` (5,17) | `#16ca2d` |
| Galat | `#fef6f5` | `#b3261e` (6,14) | `#f26052` |

## Tipografi

Inter variable, self-host (`static/fonts/inter-latin*.woff2`, OFL di `OFL-inter.txt`), `font-display: swap`. Skala: hero 40/44 700 −0,03em (28 px di ponsel); h1 30/36 650 −0,02em; angka statistik 34/40 650 tabular; judul kartu 18/25 600; isi 15/23; sekunder 13; label 12 tanpa huruf kapital dan tanpa tracking negatif. Headline dua nada (hitam + abu) hanya pada hero civitas, judul tips form, dan panel navy.

## Bentuk dan kedalaman

| Tingkat | Radius | Padding | Bayangan |
| --- | --- | --- | --- |
| Hero, panel navy | 28 | 32–40 | tanpa (garis tipis) |
| Kartu utama (form, detail, tabel, grafik) | 20 | 24 | `--shadow-panel` |
| Kartu pendukung (tips, timeline, statistik di ponsel) | 14 | 20 | `--shadow-card` |
| Kontrol (input, select, dropzone) | 10 | 0 14 | tanpa |
| Tombol, pill status | 999 | 0 20 / 2 10 | tombol sekunder `--shadow-pill` |
| Modal | 20 (ponsel: 20 di atas saja, bottom sheet) | 20–24 | `--shadow-float` |

Spasi kelipatan 4 px (`--s-1` 4 px … `--s-16` 64 px).

## Komponen

`shell` (sidebar 248 px, topbar, bottom nav), `kepala-hal`, `hero`, `strip-angka` + `angka-sel` (statistik satu strip dengan titik status, sel "Laporan masuk" bertint), `pill-*`, `tab` + `tab-indikator`, `saring` + `cari`, `baris` (laporan terakhir), `laporan-item`, `tugas-item`, `tabel` (berubah jadi kartu berlabel < 720 px), `paginasi`, `field` + `galat-field` + `bantuan-field`, `dropzone`, `rincian` + `fakta` + `foto-bukti`, `linimasa` (garis putus-putus ala relate), `panel-info`, `panel-waspada`, `panel-navy`, `modal` (`<dialog>`), `pop-panel` (notifikasi, menu akun), `toast`, `kosong`, `skeleton`. Ikon: satu sprite SVG di `fragments/shell :: ikon`, stroke 1,75 px.

## Gerak

Hanya `transform` dan `opacity`, satu kurva `cubic-bezier(.2,.7,.2,1)`: fade halaman 140 ms, modal 160 ms (bottom sheet 200 ms), toast 200 ms, tab 200 ms, popover 120 ms, batang grafik tumbuh sekali per sesi (480 ms, stagger 40 ms), donut tersapu sekali, pill berkedip tint sekali setelah status berubah, garis linimasa terisi bila tahap bertambah sejak kunjungan terakhir. `prefers-reduced-motion: reduce` mematikan semuanya.

## Responsif

≥ 1200 px sidebar penuh; 900–1199 px sidebar ikon 76 px; < 1024 px kolom kanan turun; < 900 px bottom nav (civitas 3, petugas 2, admin 3) + topbar ringkas; < 720 px tabel jadi kartu berlabel, statistik 2 kolom, modal jadi bottom sheet, tombol utama lebar penuh. Target sentuh ≥ 44 px di layar sentuh.
