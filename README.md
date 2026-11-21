# UM Peduli · Pelaporan Fasilitas Kampus

Aplikasi demo untuk mencatat kerusakan fasilitas, mengelompokkan laporan, menentukan prioritas, dan mengikuti perbaikan. Proyek Advance Programming (OOP), Kelompok 5 · Offering C · Teknik Informatika Universitas Negeri Malang.

Spring Boot 4.1.2, Java 21, Spring MVC, Thymeleaf, Spring Data JPA, MySQL 8, dan Flyway berjalan dalam satu aplikasi. Ketiga ruang kerja memakai database yang sama dan dapat dibuka langsung dalam mode demo localhost.

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

Flyway membuat lima tabel bisnis dan mengisi demo satu kali melalui `V1` dan `V2`. Hibernate memvalidasi skema saat startup. Simpan password di `.env` yang diabaikan Git. Aplikasi demo terikat ke `127.0.0.1`.

## Urutan demo

| Ruang kerja | Aksi |
| --- | --- |
| [Mahasiswa](http://127.0.0.1:8080/mahasiswa) | Pilih Andi. Laporkan AC B13 dengan jenis **Tidak berfungsi**. Ganti ke Budi dan laporkan fasilitas serta jenis yang sama. |
| [Petugas](http://127.0.0.1:8080/petugas) | Pilih Dimas Pratama, buka Verifikasi, dan isi bahaya, tingkat gangguan, serta jumlah unit rusak. |
| [Admin](http://127.0.0.1:8080/admin) | Buka kasus AC B13. Periksa laporan gabungan dan hasil prioritas, lalu tugaskan Dimas Pratama. |
| [Petugas](http://127.0.0.1:8080/petugas/tugas) | Mulai perbaikan, simpan catatan progres, lalu tandai selesai. |
| [Riwayat mahasiswa](http://127.0.0.1:8080/mahasiswa/laporan) | Pilih Andi atau Budi dan buka laporan untuk melihat perubahan status dan catatan perbaikan. |

Pilih akun dari bagian atas halaman lalu klik **Ganti**. Pilihan akun tersimpan di sesi browser. Dimas tersedia sepanjang hari dalam jadwal demo sehingga alur AC dapat dicoba kapan saja.

## Dokumentasi

- [Alur fitur dan route](docs/FEATURES.md)
- [Arsitektur, transaksi, dan basis data](docs/ARCHITECTURE.md)
- [Penerapan 10 kelas OOP dan satu interface](docs/OOP_IMPLEMENTATION.md)
- [Pemetaan desain dari ZIP](docs/FIGMA_IMPLEMENTATION.md)
- [Hasil pengujian](docs/TEST_RESULTS.md)
