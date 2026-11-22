# Sistem desain UM Peduli

Arah visual: layanan publik universitas yang tenang dan rapi, seperti formulir resmi yang dirancang dengan baik. Satu fondasi identitas dipakai bersama, tetapi tiap ruang kerja punya komposisi sesuai tugas penggunanya.

Semua nilai ada di `src/main/resources/static/css/tokens.css`. `app.css` hanya membaca variabel dari berkas itu.

## Warna

| Token | Nilai | Alasan |
| --- | --- | --- |
| `--um-700` | `#273984` | Biru tua UM dari aset lama dan lambang. Satu-satunya warna merek untuk tombol utama dan penanda aktif. |
| `--um-900` | `#151f4f` | Latar sidebar admin. Lebih gelap agar tabel di area konten menjadi pusat perhatian. |
| `--gold-500` / `--gold-600` | `#c8971b` / `#7d5c0d` | Emas dari lambang UM. Aksen terkendali: garis bawah CTA, tab aktif petugas, penanda tahap yang sedang berjalan, nomor langkah. `gold-600` dipakai untuk teks (kontras 5,6:1). |
| `--canvas` / `--paper` | `#f5f4ef` / `#ffffff` | Netral sedikit hangat seperti kertas formulir, membedakan dari "biru SaaS" yang dingin. |
| `--ink`, `--ink-2`, `--ink-3` | `#161b2e`, `#3a4156`, `#5c6275` | Tiga tingkat teks. Kontras terendah 5,0:1 di atas `--canvas-strong`. |

**Status kasus (5 tahap)** memakai pasangan warna teks/latar dengan kontras 6,1–7,4:1, **plus** label teks dan bentuk penanda yang berbeda: lingkaran kosong (menunggu verifikasi), setengah terisi (menunggu penugasan), titik (ditugaskan), kotak (sedang diperbaiki), centang (selesai). Linimasa mahasiswa juga menulis "Tahap n/5".

**Prioritas** memakai label teks dan meter 1–4 batang (Rendah 1, Sedang 2, Tinggi 3, Kritis 4), jadi tetap terbaca tanpa warna. Baris kasus kritis aktif di tabel admin diberi garis merah di sisi kiri.

## Tipografi

- **Source Serif 4** (judul halaman, angka ringkasan mahasiswa). Serif bergaya buku memberi suara institusi akademik dan membedakan judul dari isi formulir. Sumbu `opsz` membuat judul besar tetap rapat.
- **Public Sans** (seluruh antarmuka). Dirancang untuk layanan pemerintah, netral, sangat terbaca di ukuran kecil, dan memiliki angka tabular (`tnum`) untuk ID kasus, waktu, dan jumlah unit.
- Ruang petugas sengaja memakai Public Sans tebal untuk judul: lebih cepat dibaca di lapangan.
- Kedua font di-host lokal (`static/fonts/`, lisensi SIL OFL disertakan). Tidak ada CDN.
- Skala: 12, 13, 14, 15, 16, 17, 22, 28, 36 px, judul landing `clamp(2rem … 3.25rem)`. Tinggi baris 1,15 (judul), 1,3 (subjudul), 1,55 (isi).

## Jarak, bentuk, gerak

- Grid 4/8: `--s-1` 4 px sampai `--s-20` 80 px.
- Radius kecil (4, 6, 10 px). Tidak ada bayangan besar; pemisahan memakai garis tipis `--line`.
- Target sentuh minimal 44 px, kolom penting 56 px (login, tombol besar, pilihan petugas).
- Transisi 120–180 ms untuk hover/fokus. `prefers-reduced-motion` membuat durasi 0.
- Fokus keyboard: garis 3 px `--focus` dengan offset; di latar gelap memakai kuning `#ffd36b`.

## Komponen

`tombol` (utama, garis, teks, besar, penuh), `field` + `field-pesan` (galat dengan `aria-invalid` dan `aria-live`), `langkah` (fieldset bernomor), `pilihan` (radio sebagai kartu), `alert` (sukses, galat, waspada), `status`, `prio`, `tahap`, `kosong`, `sorot`, `fakta`, `blok`, `tabel`. Ikon: satu sprite SVG di `fragments/common :: symbols`, garis 1,75 px pada grid 24. Tidak ada emoji.

## Tiga ruang kerja

| Ruang | Shell | Keputusan |
| --- | --- | --- |
| Mahasiswa | Navigasi atas putih, satu kolom 760 px | Tugasnya satu: melapor lalu memantau. Form tiga langkah bernomor, jenis kerusakan sebagai pilihan besar, riwayat berupa daftar dengan garis 5 tahap. Bahasa "kamu". |
| Petugas | Bilah gelap, tab bawah di ponsel | Dipakai satu tangan di lokasi. Kartu berbingkai tebal, gedung tampil paling besar, tombol 52–56 px, verifikasi berupa empat langkah dengan pilihan besar, bukan dropdown. |
| Admin | Sidebar biru tua tetap, konten lebar | Pekerjaan memindai dan memutuskan. Angka ringkasan dalam satu strip, tabel padat 14 px dengan angka tabular, panel samping lengket untuk verifikasi dan penugasan. Di ponsel tabel berubah menjadi daftar berlabel. |

## Uji "AI slop"

1. **Ganti logo.** Judul landing menyebut AC kelas, kursi patah, dan keran bocor; langkah alur menyebut gedung, ruang, bahaya, dan unit rusak; kartu petugas menonjolkan nama gedung. Mengganti nama produk membuat isi tidak masuk akal, jadi desain terikat pada layanan ini.
2. **Tutup teks.** Tanpa kalimat pemasaran, struktur tetap menjelaskan layanan: satu tombol menuju login, tiga langkah bernomor, garis lima tahap, tabel dengan kolom prioritas dan unit rusak.
3. **Alasan.** Setiap pilihan warna, font, dan komponen di atas punya alasan yang terkait konteks. Tidak ada gradasi, glassmorphism, ilustrasi, statistik karangan, testimoni, atau emoji. Semua angka berasal dari database.
