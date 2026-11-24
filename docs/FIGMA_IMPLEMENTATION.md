# Implementasi desain UM Peduli

Acuan konten dan flow: 13 ekspor PDF di `Prompt/Untitled.zip` (render di `docs/redesain/acuan/layar-*.png`). Acuan tema: `Prompt/relate.so.pdf`. Hasil akhir: `docs/redesain/hasil/`.

| Layar acuan | Route | Template |
| --- | --- | --- |
| Civitas · Dashboard | `/mahasiswa` | `mahasiswa/index.html` |
| Civitas · Buat laporan | `/mahasiswa/laporan/baru` (POST `/mahasiswa/laporan`, multipart) | `mahasiswa/form.html` |
| Civitas · Laporan saya | `/mahasiswa/laporan?tahap=&q=&halaman=` | `mahasiswa/riwayat.html` |
| Civitas · Detail laporan | `/mahasiswa/laporan/{id}` | `mahasiswa/detail.html` |
| Petugas · Dashboard tugas | `/petugas` | `petugas/index.html` |
| Petugas · Detail dan progres | `/petugas/tugas/{id}` (POST `…/progres`, `…/selesai`) | `petugas/detail.html` |
| Admin · Dashboard | `/admin?rentang=6\|12` | `admin/index.html` |
| Admin · Laporan | `/admin/kasus?tahap=&q=&tanggal=&petugas=&halaman=`, ekspor `/admin/kasus/ekspor` | `admin/kasus.html` |
| Admin · Detail dan verifikasi (modal) | `/admin/kasus/{id}?modal=1` (halaman penuh tanpa `modal`), POST `…/verifikasi` | `admin/detail.html` |
| Admin · Petugas | `/admin/petugas`, POST `/admin/petugas`, POST `/admin/penugasan` | `admin/petugas.html` |
| Mobile · 3 layar civitas | route yang sama, < 900 px | CSS responsif + bottom nav |

Route lama tetap ada. Pemeriksaan di lokasi oleh petugas (`/petugas/verifikasi`, `/petugas/kasus/{id}/verifikasi`) dan penggabungan kasus tetap berjalan di model, tetapi tidak tampil di menu karena layar acuan menyerahkan verifikasi kepada admin. Foto bukti dan foto hasil dibuka lewat `/berkas/bukti/{laporanId}` dan `/berkas/hasil/{tugasId}`, dengan pemeriksaan hak akses (pelapor, petugas yang ditugaskan, admin).

Lambang UM dan foto contoh AC diambil dari gambar di dalam PDF acuan (bukan dibuat ulang). Kredit foto mengikuti teks layar: Antonio Mette, CC BY-SA 4.0, dipotong.
