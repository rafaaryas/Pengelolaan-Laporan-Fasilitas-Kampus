# UM Peduli · Pelaporan Fasilitas Kampus

Aplikasi demo untuk melaporkan kerusakan fasilitas, menentukan prioritas dari pemeriksaan petugas, dan memantau perbaikan. Proyek Advance Programming (OOP), Kelompok 5 · Offering C · Teknik Informatika Universitas Negeri Malang.

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

Flyway membuat lima tabel bisnis (`V1`–`V3`) dan menambah kolom alur layar acuan (`V4`: fakultas, gedung, lantai, ruang, foto bukti, foto hasil, verifikator admin). Aplikasi juga memuat `db/demo/V100__data_demo_layar.sql`, yaitu data persis seperti 13 layar acuan (248 laporan, 12 petugas). Data itu dibuat ulang dengan `python scripts/buat-data-demo.py src/main/resources/db/demo/V100__data_demo_layar.sql`. Migrasi `V101` menambah catatan pemeriksaan dan tindak lanjut pada data yang sudah ada. Tes hanya memakai `db/migration`. Foto unggahan disimpan di `data/unggahan/` (diabaikan Git, ubah dengan `UNGGAHAN_FOLDER`). Hibernate memvalidasi skema saat startup. Simpan password di `.env` yang diabaikan Git. Aplikasi demo terikat ke `127.0.0.1`.

## Masuk dan urutan demo

Buka [http://127.0.0.1:8080/](http://127.0.0.1:8080/), klik **Laporkan kerusakan**, lalu isi **email atau NIM**. Ruang kerja ditentukan dari data di tabel `pengguna`, bukan dari teks atau domain email.

| Peran | Email demo | Catatan |
| --- | --- | --- |
| Civitas | `nadia@example.invalid` (NIM `240535600201`) | 8 laporan contoh dengan berbagai status |
| Petugas | `budi.santoso@example.invalid` | PTG-001, Kelistrikan & AC, 2 tugas aktif |
| Admin | `admin@example.invalid` | Administrator UM |

Petugas lain: `agus@`, `siti@`, `rudi@`, `dimas@`, `raka@example.invalid`, dan lainnya (lihat halaman Petugas). Email atau NIM 12 digit yang belum terdaftar membuat akun mahasiswa baru secara otomatis.

| Langkah | Masuk sebagai | Aksi |
| --- | --- | --- |
| 1 | `nadia@example.invalid` | **Buat Laporan**. Pilih lokasi dan isi deskripsi. Foto bukti opsional, JPG/PNG maksimal 5 MB. |
| 2 | `admin@example.invalid` | Buka laporan, cek kelengkapannya, lalu klik **Verifikasi laporan**. Pilih petugas saat verifikasi atau kemudian di antrean **Menunggu Penugasan**. |
| 3 | `budi.santoso@example.invalid` | Buka tugas dan klik **Simpan Pemeriksaan** setelah mencatat bahaya, gangguan, dan jumlah unit rusak. Sistem menentukan prioritas dan alasannya. |
| 4 | `budi.santoso@example.invalid` | Pilih **Lanjutkan Perbaikan**, atau **Ajukan Tindak Lanjut** dengan alasan dan kebutuhan. Admin membuka antrean tindak lanjut untuk meneruskan atau mengalihkan tugas. |
| 5 | Petugas yang ditugaskan | Tambahkan catatan dan foto hasil, lalu klik **Tandai Selesai**. Mahasiswa melihat perkembangan dan bukti di **Laporan Saya**. |

Satu kasus tetap memiliki satu tugas. Pengalihan mengganti petugas pada tugas tersebut dan menyimpan pemeriksaan serta keputusan terakhir. Kasus aktif berprioritas Kritis terlihat pada dashboard admin.

Penggabungan laporan berdasarkan fasilitas dan jenis kerusakan tetap tersedia untuk alur berbasis katalog. Form berbasis lokasi saat ini membuat satu kasus per laporan. Penggabungan laporan dari form tersebut merupakan pengembangan terpisah.

Menu akun di kanan atas berisi **Masuk dengan akun lain** dan **Keluar**. Membuka ruang kerja peran lain menampilkan halaman 403. Membuka ruang kerja tanpa masuk mengarah ke `/login`, lalu kembali ke halaman semula setelah masuk.

### Keterbatasan: mode demo akademik

Login hanya memakai email atau NIM, **tanpa kata sandi dan tanpa OTP**. Siapa pun yang mengetahui email admin atau petugas dapat masuk sebagai peran tersebut. Mode ini diterima untuk demo mata kuliah dan **tidak aman untuk data sungguhan**. Pengamanan yang tersedia hanya bersifat pelengkap: proteksi route per peran, CSRF, rotasi ID sesi setelah masuk, batas 20 percobaan masuk per IP per 5 menit, dan waktu respons minimum yang seragam pada `POST /login`.

Rekomendasi peningkatan (belum diimplementasikan): kode OTP sekali pakai ke email kampus, atau SSO kampus (misalnya SIAKAD/OIDC) sehingga peran diambil dari sistem identitas universitas.

## Dokumentasi

- [Alur fitur dan route](docs/FEATURES.md)
- [Arsitektur, transaksi, dan basis data](docs/ARCHITECTURE.md)
- [Penerapan 10 kelas OOP dan satu interface](docs/OOP_IMPLEMENTATION.md)
- [Sistem desain: token, tipografi, komponen](docs/DESIGN_SYSTEM.md)
- [Pemetaan desain dari ZIP](docs/FIGMA_IMPLEMENTATION.md)
- [Analisis redesain relate.so](ANALISIS.md) dan [laporan akhir redesain](docs/redesain/LAPORAN.md)
- [Hasil pengujian](docs/TEST_RESULTS.md)
