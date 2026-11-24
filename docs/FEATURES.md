
# Fitur dan route

| Peran | Halaman GET | Aksi POST |
| --- | --- | --- |
| Publik | `/`, `/login`, `/bantuan`, `/akses-ditolak` | `/login` |
| Semua yang sudah masuk | | `/logout` |
| Mahasiswa | `/mahasiswa`, `/mahasiswa/laporan`, `/mahasiswa/laporan/baru`, `/mahasiswa/laporan/{id}` | `/mahasiswa/laporan` |
| Admin | `/admin`, `/admin/kasus`, `/admin/kasus/{id}`, `/admin/petugas` | `/admin/kasus/{id}/verifikasi`, `/admin/kasus/{id}/penugasan`, `/admin/kasus/{id}/tindak-lanjut`, `/admin/kasus/{id}/laporan/{laporanId}/pisahkan`, `/admin/petugas` |
| Petugas | `/petugas`, `/petugas/tugas`, `/petugas/tugas/{id}` | `/petugas/tugas/{id}/pemeriksaan`, `/petugas/tugas/{id}/tindak-lanjut`, `/petugas/tugas/{id}/mulai`, `/petugas/tugas/{id}/progres`, `/petugas/tugas/{id}/selesai` |

## Masuk dan proteksi route

- `/admin/**` hanya untuk peran ADMIN, `/petugas/**` hanya PETUGAS, `/mahasiswa/**` hanya MAHASISWA. Beranda, `/login`, halaman galat, dan aset statis bersifat publik.
- `POST /login` menerima satu kolom `identitas`. Email dinormalisasi (trim, huruf kecil) lalu dicari di kolom `pengguna.kontak`. NIM harus 12 digit dan dicari di kolom `pengguna.nim`. Masukan valid yang tidak terdaftar membuat akun mahasiswa baru. Peran admin dan petugas hanya berasal dari data.
- Setelah masuk, identitas disimpan di `SecurityContext` sesi Spring Security, ID sesi diganti, dan token CSRF diperbarui. Halaman transisi singkat menampilkan "Masuk sebagai …" lalu mengarah ke halaman yang diminta sebelum masuk, atau ke beranda peran.
- Pengguna yang sudah masuk lalu membuka `/login` diarahkan ke ruang kerjanya. Akses ke ruang kerja peran lain menampilkan halaman 403.
- Pemilih akun demo lama (`/mahasiswa/identitas`, `/petugas/identitas`) sudah dihapus.

## Status kasus

1. **Menunggu verifikasi.** Mahasiswa mengisi lokasi dan deskripsi. Foto kerusakan bersifat opsional.
2. **Menunggu penugasan.** Admin telah memeriksa laporan. Kasus melewati antrean ini jika admin sekaligus menentukan petugas.
3. **Ditugaskan.** Petugas terpilih memeriksa kondisi di lokasi. Sistem menyimpan temuan, prioritas, dan alasan penetapannya.
4. **Sedang diperbaiki.** Petugas melanjutkan tugas setelah pemeriksaan dan dapat mencatat perkembangan.
5. **Perlu tindak lanjut.** Jika perlu keputusan admin, petugas mengirim alasan dan kebutuhan. Admin dapat menginstruksikan petugas sekarang untuk melanjutkan atau mengalihkan tugas yang sama dengan alasan tertulis. Selanjutnya status kembali ke Sedang Diperbaiki.
6. **Selesai.** Petugas menutup tugas dengan catatan dan foto hasil. Kondisi fasilitas dari katalog serta ketersediaan petugas diperbarui.

Mahasiswa dapat membuka, mencari, dan memfilter laporan miliknya. Admin dapat menyaring laporan, membuka antrean tindak lanjut, melihat kasus aktif Kritis, dan mengekspor hasil filter. Tautan lama ke antrean verifikasi petugas mengarah ke tugas petugas yang belum diperiksa.

Penggabungan berdasarkan fasilitas dan jenis kerusakan berjalan pada alur berbasis katalog. Admin masih dapat memisahkan laporan yang salah dikelompokkan sebelum verifikasi. Form berbasis lokasi membuat satu kasus per laporan.

## Prioritas

| Level | Hasil pemeriksaan |
| --- | --- |
| Kritis | Ada bahaya keselamatan kritis yang dikonfirmasi. |
| Tinggi | Fasilitas tidak dapat digunakan dan tidak ada bahaya kritis. |
| Sedang | Sebagian fungsi fasilitas terganggu. |
| Rendah | Gangguan penggunaan terbatas. |

Antrean tindak lanjut diurutkan berdasarkan prioritas, lalu waktu pengajuan terlama. Kasus Kritis yang masih aktif terlihat pada dashboard admin walaupun sedang diperbaiki langsung. Aturan prioritas adalah kebijakan simulasi akademik Kelompok 5 untuk demo mata kuliah Advance Programming. Standar penanganan resmi mengikuti kebijakan Universitas Negeri Malang.
