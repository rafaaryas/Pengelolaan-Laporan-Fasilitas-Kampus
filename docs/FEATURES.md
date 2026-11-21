
# Fitur dan route

| Peran | Halaman GET | Aksi POST |
| --- | --- | --- |
| Umum | `/` | |
| Mahasiswa | `/mahasiswa`, `/mahasiswa/laporan`, `/mahasiswa/laporan/baru`, `/mahasiswa/laporan/{id}` | `/mahasiswa/identitas`, `/mahasiswa/laporan` |
| Admin | `/admin`, `/admin/kasus`, `/admin/kasus/{id}`, `/admin/petugas` | `/admin/kasus/{id}/penugasan`, `/admin/kasus/{id}/laporan/{laporanId}/pisahkan` |
| Petugas | `/petugas`, `/petugas/verifikasi`, `/petugas/kasus/{id}/verifikasi`, `/petugas/tugas`, `/petugas/tugas/{id}` | `/petugas/identitas`, `/petugas/kasus/{id}/verifikasi`, `/petugas/tugas/{id}/mulai`, `/petugas/tugas/{id}/progres`, `/petugas/tugas/{id}/selesai` |

## Status kasus

1. **Menunggu verifikasi.** Sistem mencatat laporan dan mencari kasus aktif dengan fasilitas serta jenis kerusakan yang sama.
2. **Menunggu penugasan.** Petugas memeriksa kasus, lalu sistem menyimpan hasil dan alasan prioritas.
3. **Ditugaskan.** Admin memilih petugas yang cocok, berada dalam jam kerja, dan bebas tugas aktif.
4. **Sedang diperbaiki.** Petugas memulai tugas dan dapat menyimpan catatan sementara.
5. **Selesai.** Petugas menyimpan hasil akhir. Waktu selesai tercatat, petugas kembali tersedia sesuai jadwal, dan kondisi fasilitas dihitung ulang. Laporan berjenis sama setelah penutupan dapat memulai kasus baru.

Mahasiswa dapat mencari dan memfilter riwayat akun yang dipilih. Admin dapat mencari kasus berdasarkan ID, fasilitas, jenis, atau lokasi dan memfilter status serta prioritas. Admin dapat memisahkan laporan yang salah dikelompokkan sebelum verifikasi jika kasus asal masih memiliki laporan lain. Laporan tetap menyimpan pelapor, waktu, dan isi awal, dan kasus hasil koreksi diperiksa secara terpisah.

## Prioritas

| Level | Hasil pemeriksaan |
| --- | --- |
| Kritis | Ada bahaya keselamatan kritis yang dikonfirmasi. |
| Tinggi | Fasilitas tidak dapat digunakan dan tidak ada bahaya kritis. |
| Sedang | Sebagian fungsi fasilitas terganggu. |
| Rendah | Gangguan penggunaan terbatas. |

Daftar kasus aktif diurutkan berdasarkan prioritas, jumlah unit rusak, jumlah pelapor unik, dan waktu laporan terbaru. Kasus selesai tampil setelah kasus aktif. Aturan prioritas adalah kebijakan simulasi akademik Kelompok 5 untuk demo mata kuliah Advance Programming. Standar penanganan resmi mengikuti kebijakan Universitas Negeri Malang.
