# UM Peduli · Pelaporan Fasilitas Kampus

Aplikasi demo untuk mencatat kerusakan fasilitas, mengelompokkan laporan, menentukan prioritas, dan mengikuti perbaikan. Proyek Advance Programming (OOP), Kelompok 5 · Offering C · Teknik Informatika Universitas Negeri Malang.

Spring Boot 4.1.2, Java 21, Spring MVC, Thymeleaf, Spring Data JPA, MySQL 8, dan Flyway berjalan dalam satu aplikasi. Ketiga ruang kerja memakai database yang sama. Pengguna masuk dengan email atau NIM, lalu diarahkan ke ruang kerja sesuai perannya.

## Jalankan di Windows

1. Siapkan **JDK 21**, PowerShell, dan **MySQL 8**. Runner memakai JDK dari `JAVA_HOME` atau JDK 21 portable di `.tools/jdk-21*/`. Maven Wrapper disertakan.
2. Dari direktori yang berisi `pom.xml`, salin `.env.example` menjadi `.env` bila file lokal belum ada. Isi nama skema, pengguna, dan password MySQL. Gunakan skema khusus aplikasi.
3. Siapkan MySQL dengan salah satu cara berikut.

   **Laragon atau MySQL lokal.** Jalankan server, lalu buat skema dan pengguna jika belum tersedia. Jalankan contoh ini sebagai administrator MySQL dan ganti placeholder password dengan nilai lokal.

   ```sql
   CREATE DATABASE IF NOT EXISTS pelaporan_fasilitas
       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'kampus_app'@'localhost' IDENTIFIED BY '<PASSWORD_LOKAL>';
   GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
       ON pelaporan_fasilitas.* TO 'kampus_app'@'localhost';
   ```

   Bila akun aplikasi sudah ada dan hanya memiliki hak DML, isi `DB_MIGRATION_USERNAME` dan `DB_MIGRATION_PASSWORD` dengan akun migrasi yang memiliki hak DDL pada skema tersebut saat ada versi Flyway baru. Aplikasi tetap memakai `DB_USERNAME` dan `DB_PASSWORD` untuk query bisnis. Setelah migrasi selesai, akun DML bisa menjalankan versi skema yang sama tanpa kredensial migrasi.

   **Docker Compose.** Atur `DB_PORT=3307` dan isi juga `MYSQL_ROOT_PASSWORD` di `.env`. Jalankan `docker compose up -d --wait`. Volume `mysql_data` menyimpan data dan port MySQL hanya terikat ke loopback.

4. Jalankan perintah dari folder proyek.

   ```powershell
   .\scripts\dev.ps1 -Task Info
   .\scripts\dev.ps1 -Task Package
   .\scripts\dev.ps1 -Task VerifyDatabase
   .\scripts\dev.ps1 -Task Run
   ```

   Buka [http://127.0.0.1:8080/](http://127.0.0.1:8080/). Pakai port dari `SERVER_PORT` jika konfigurasi berbeda. Hentikan proses yang dijalankan dari terminal dengan Ctrl+C.

Flyway membuat lima tabel bisnis dan mengisi demo melalui `V1` dan `V2`. `V3` menormalkan email, menambah kolom `nim` yang unik, dan mengisi NIM demo. Hibernate memvalidasi skema saat startup. Simpan password di `.env` yang diabaikan Git. Aplikasi demo terikat ke `127.0.0.1`.

## Masuk dan urutan demo

Buka [http://127.0.0.1:8080/](http://127.0.0.1:8080/), klik **Masuk dan laporkan kerusakan**, lalu isi **email atau NIM**. Ruang kerja ditentukan dari data di tabel `pengguna`, bukan dari teks atau domain email.

| Peran | Email demo | NIM demo |
| --- | --- | --- |
| Admin | `admin@example.invalid` | |
| Petugas | `dimas@example.invalid` (pendingin, 24 jam), `raka@example.invalid` (listrik), `rudi@example.invalid` (perabot), `siti@example.invalid` (sanitasi), `agus@example.invalid` (pendingin, 08.00–16.00) | |
| Mahasiswa | `andi@example.invalid`, `budi@example.invalid`, `sinta@example.invalid` | `240535600101` (Andi), `240535600102` (Budi), `240535600103` (Sinta) |

Email atau NIM 12 digit yang belum terdaftar membuat akun mahasiswa baru secara otomatis. Email seperti `yy@admin.com` yang tidak ada di database tetap masuk sebagai mahasiswa.

| Langkah | Masuk sebagai | Aksi |
| --- | --- | --- |
| 1 | `andi@example.invalid` | Lapor kerusakan AC B13, jenis **Tidak berfungsi**. Klik **Keluar**. |
| 2 | `240535600102` (Budi) | Laporkan fasilitas dan jenis yang sama. Laporan tergabung ke kasus yang sama. |
| 3 | `dimas@example.invalid` | Buka **Verifikasi**, isi bahaya, tingkat gangguan, dan jumlah unit rusak. |
| 4 | `admin@example.invalid` | Buka kasus AC B13, periksa laporan gabungan dan prioritas, lalu tugaskan Dimas Pratama. |
| 5 | `dimas@example.invalid` | Di **Tugas saya**, mulai perbaikan, simpan catatan progres, lalu tandai selesai. |
| 6 | `andi@example.invalid` | Buka **Laporan saya** untuk melihat tahap 5/5 dan catatan perbaikan. |

Membuka ruang kerja peran lain menampilkan halaman 403. Membuka ruang kerja tanpa masuk mengarah ke `/login`, lalu kembali ke halaman semula setelah masuk.

### Keterbatasan: mode demo akademik

Login hanya memakai email atau NIM, **tanpa kata sandi dan tanpa OTP**. Siapa pun yang mengetahui email admin atau petugas dapat masuk sebagai peran tersebut. Mode ini diterima untuk demo mata kuliah dan **tidak aman untuk data sungguhan**. Pengamanan yang tersedia hanya bersifat pelengkap: proteksi route per peran, CSRF, rotasi ID sesi setelah masuk, batas 20 percobaan masuk per IP per 5 menit, dan waktu respons minimum yang seragam pada `POST /login`.

Rekomendasi peningkatan (belum diimplementasikan): kode OTP sekali pakai ke email kampus, atau SSO kampus (misalnya SIAKAD/OIDC) sehingga peran diambil dari sistem identitas universitas.

## Dokumentasi

- [Alur fitur dan route](docs/FEATURES.md)
- [Arsitektur, transaksi, dan basis data](docs/ARCHITECTURE.md)
- [Penerapan 10 kelas OOP dan satu interface](docs/OOP_IMPLEMENTATION.md)
- [Sistem desain: token, tipografi, komponen](docs/DESIGN_SYSTEM.md)
- [Pemetaan desain dari ZIP (versi awal)](docs/FIGMA_IMPLEMENTATION.md)
- [Hasil pengujian](docs/TEST_RESULTS.md)
