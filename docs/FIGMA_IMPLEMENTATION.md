
# Implementasi desain UM Peduli

Konektor Figma menolak akses ke file `Untitled`. Implementasi memakai 13 ekspor PDF dari `Untitled.zip` yang dikirim pengguna. Seluruh PDF dirender dan ditinjau, termasuk layar desktop tiga peran dan tiga layar mobile mahasiswa.

## Pemetaan layar

| Ekspor PDF | Tampilan implementasi |
| --- | --- |
| Admin · Dashboard | `/admin` dengan metrik database dan kasus yang baru dilaporkan |
| Admin · Laporan | `/admin/kasus`, dengan pencarian, filter, dan pengurutan urgensi |
| Admin · Detail dan verifikasi | `/admin/kasus/{id}` untuk rincian dan penugasan, `/petugas/kasus/{id}/verifikasi` untuk pemeriksaan |
| Admin · Petugas | `/admin/petugas` untuk daftar keahlian, jadwal, ketersediaan, dan akses ke kasus siap ditugaskan |
| Civitas · Dashboard, Buat laporan, Laporan saya, Detail laporan | Dashboard, form, riwayat, dan detail mahasiswa |
| Petugas · Dashboard tugas, Detail dan progres | Dashboard dan detail tugas dengan aksi mulai, simpan progres, dan selesai |
| Mobile · Dashboard civitas, Buat laporan, Laporan saya | Tampilan responsif dengan navigasi bawah, ringkasan dua kolom, dan riwayat berbentuk kartu |

Warna latar biru muda `#f5f7fb`, teks `#192842`, dan panel utama `#273984` mengikuti PDF dashboard. Lambang UM diambil dari area logo pada PDF yang dikirim. Layout, panel, tombol, status, ikon, dan navigasi dibangun sebagai HTML serta CSS yang dapat menyesuaikan ukuran layar.

Detail admin menggunakan halaman tersendiri agar daftar laporan, hasil verifikasi, dan penugasan mudah dibaca. Form mengambil gedung dan ruang dari fasilitas yang dipilih. Pemilih identitas demo masuk ke header. Lingkup MVP memakai deskripsi dan catatan teks untuk laporan serta progres. Foto, grafik, dan ekspor menjadi opsi pengembangan setelah alur demo.
