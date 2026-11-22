# Hasil pengujian

## Redesain dan login email/NIM · 22 November 2026

Dijalankan di Windows (JDK 21, Maven Wrapper, MySQL Laragon 8) pada branch `redesain-um-peduli`.

| Pemeriksaan | Hasil |
| --- | --- |
| `.\scripts\dev.ps1 -Task Package` | 50 tes lulus (`AlurPelaporanTest` 2, `LoginTest` 22, `PageSecurityTest` 21, `PrioritasBerbasisRisikoTest` 5). JAR berhasil dibuat. |
| Migrasi | Skema QA baru `pelaporan_fasilitas_qa` dimigrasi V1–V3 tanpa galat; Hibernate `validate` lulus. |
| Alur browser (port 8081, skema QA) | Landing → CTA → login galat → Andi (email) membuat laporan AC B13 → 403 saat membuka `/admin` → keluar → Dimas memverifikasi → admin dibuka dari `/admin/kasus/1`, diarahkan ke login, lalu kembali ke kasus tersebut dan menugaskan Dimas → Dimas memulai, mencatat, dan menyelesaikan → Andi (via NIM 240535600101) melihat tahap 5/5. |
| Responsif | 1440, 390, dan 360 px untuk ruang mahasiswa, petugas, dan admin: `scrollWidth` sama dengan lebar layar (tanpa scroll horizontal). |
| Keyboard | Tab pertama menuju "Lewati ke konten", CTA landing menerima fokus dengan garis fokus terlihat dan dapat diaktifkan dengan Enter. |
| Kontras | Semua pasangan teks utama, status, dan prioritas ≥ 4,5:1 (dihitung dari token). |

Tes otomatis yang ditambahkan mencakup deteksi peran (admin, petugas, mahasiswa via email, via NIM, tidak terdaftar, `yy@admin.com` tetap mahasiswa), akun mahasiswa baru yang bisa melapor dan hanya melihat miliknya, proteksi route (redirect ke login, 403), pengalihan pengguna yang sudah masuk, kembali ke tujuan semula, logout dengan CSRF, validasi masukan, CSRF pada form login, rotasi ID sesi, dan pembatasan percobaan.

`scripts/test-browser-demo.js` sudah diperbarui untuk alur login dan dapat dijalankan dengan Playwright CLI seperti di bawah. Pada sesi ini alur yang sama dijalankan manual melalui browser.

## Versi awal · 20 November 2026


Pengujian dilakukan di Windows dengan Temurin JDK 21, Spring Boot 4.1.2, Maven Wrapper, MySQL Laragon 8.4.3, dan Chrome melalui Playwright CLI. Build otomatis memakai H2 mode MySQL untuk isolasi. Pengujian browser memakai skema MySQL terpisah `pelaporan_fasilitas_qa` pada port aplikasi 8081.

| Perintah | Hasil |
| --- | --- |
| `.\scripts\dev.ps1 -Task Package` | 19 pengujian lulus. JAR Spring Boot berhasil dibuat. |
| `.\scripts\dev.ps1 -Task VerifyDatabase` | 1 pengujian JDBC lulus terhadap MySQL Laragon menggunakan akun aplikasi. |
| `playwright-cli -s=kampus-qa run-code --filename=./scripts/test-browser-demo.js` | Alur lengkap dari browser lulus pada skema QA. Pemeriksaan ulang setelah restart mencakup persistensi, kiriman serentak, dan ukuran layar. |

## Skenario penerimaan

| Skenario | Hasil yang diamati |
| --- | --- |
| Laporan pertama | Kiriman Andi tentang AC B13 membuka kasus dengan status menunggu verifikasi. |
| Laporan duplikat | Kiriman Budi tentang fasilitas dan jenis yang sama memakai kasus yang sama. |
| Pelapor yang sama | Tiga laporan dari Andi dan Budi dihitung sebagai dua pelapor unik. |
| Kerusakan berbeda | Kebocoran AC membuka kasus berbeda. Pemisahan administratif memindahkan laporan yang dipilih dan mempertahankan isinya. |
| Verifikasi | Nama petugas, waktu, bahaya, gangguan, dan jumlah unit tersimpan. Jumlah di atas kapasitas ditolak, dan prioritas baru muncul setelah verifikasi. |
| Penilaian risiko | Satu laporan kabel terbuka memperoleh **Kritis** setelah bahaya dikonfirmasi. Kasus ini muncul sebelum AC **Tinggi**. |
| Penugasan | Hanya petugas dengan keahlian dan jadwal yang cocok muncul sebagai pilihan. Saat petugas listrik sedang bertugas, kasus lampu tetap menunggu penugasan. |
| Penyelesaian | Tugas berpindah dari ditugaskan ke sedang diperbaiki lalu selesai. Catatan progres, hasil akhir, dan kedua waktu tersimpan. Kasus tersinkron dan Dimas kembali tersedia. |
| Persistensi | Spring Boot dihentikan lalu dijalankan kembali ke skema QA yang sama. Laporan asli, kaitan kasus, prioritas, tugas, waktu selesai, dan hasil perbaikan masih tampil. Flyway melaporkan skema sudah pada versi 2. |
| Route demo | Beranda, tiga dashboard, dan halaman kerja dapat dibuka tanpa login. Data yang dibuat mahasiswa terlihat bagi admin dan petugas. |
| Tampilan | Chrome diperiksa pada lebar 1440, 768, 390, dan 320 px. Form, riwayat, tabel, navigasi, tampilan kosong, HTTP 404, serta pesan validasi diperiksa. Tabel admin memakai kartu pada ponsel dan tabel penuh pada tablet dan desktop. |

Pengujian tambahan membuka kasus baru ketika AC yang sudah selesai dilaporkan kembali. Dua kiriman bersamaan melalui dua tab Chrome ke MySQL menghasilkan satu kasus dan dua laporan. Pengujian otomatis juga memeriksa penggabungan serentak dari dua akun, urutan status, penugasan ganda, kecocokan keahlian, jam kerja, CSRF, dan validasi input.

## Mengulang pengujian browser

Jalankan salinan aplikasi demo pada port 8081 dengan skema QA yang baru dimigrasi dan masih berisi seed pengguna serta fasilitas. Browser test membuat laporan dan tugas di skema yang terhubung ke port tersebut.

```powershell
New-Item -Path .cache/browser -ItemType Directory -Force
playwright-cli -s=kampus-qa open http://127.0.0.1:8081/
playwright-cli -s=kampus-qa run-code --filename=./scripts/test-browser-demo.js
```

Tangkapan layar disimpan di `.cache/browser/`. Setelah alur berakhir, halaman terakhir harus menampilkan **Data tidak ditemukan** dari kasus yang sengaja dicari untuk memvalidasi HTTP 404.

Pemeriksaan akhir memakai MySQL Laragon. Docker Compose tersedia sebagai opsi setup dan belum dijalankan pada sesi pengujian ini.
