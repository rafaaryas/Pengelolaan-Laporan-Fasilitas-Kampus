# Pengelolaan Laporan Fasilitas Kampus

Fondasi website **Sistem Pelaporan, Prioritas, dan Monitoring Kerusakan Fasilitas Kampus** untuk tugas akhir Advance Programming (OOP), Kelompok 5 — Offering C, Teknik Informatika Universitas Negeri Malang.

Aplikasi memakai **Spring Boot MVC Monolith + Thymeleaf**. Halaman diproses oleh Java dan dikirim sebagai HTML ke browser. Tidak memerlukan Node.js, npm, React, atau frontend build terpisah.

Saat ini tersedia beranda, login, halaman placeholder mahasiswa/admin/petugas, pembatasan akses sesuai role, serta koneksi MySQL. Form laporan, verifikasi, penugasan, prioritas, dan monitoring bisnis dikembangkan pada tahap berikutnya.

## 1. Software yang diperlukan

Panduan utama di bawah menggunakan **Windows dan PowerShell**.

| Software | Kebutuhan | Sumber resmi / catatan |
| --- | --- | --- |
| JDK 21 LTS | Wajib, gunakan JDK lengkap yang memiliki java dan javac | [Eclipse Temurin JDK 21](https://adoptium.net/temurin/releases/?version=21) |
| Git | Wajib untuk clone dan kerja kelompok | [Git for Windows](https://git-scm.com/downloads/win) |
| MySQL 8.x | Wajib; pilih Laragon/MySQL lokal ATAU Docker | [Laragon](https://laragon.org/download/) atau [MySQL Community](https://dev.mysql.com/downloads/mysql/) |
| PowerShell | Untuk menjalankan scripts/dev.ps1; PowerShell 7 direkomendasikan | [Panduan instalasi resmi](https://learn.microsoft.com/powershell/scripting/install/installing-powershell-on-windows) |
| VS Code | Editor yang direkomendasikan | [VS Code](https://code.visualstudio.com/) |
| Extension Pack for Java | Direkomendasikan untuk editor | ID extension: vscjava.vscode-java-pack |
| Spring Boot Extension Pack | Opsional untuk editor | ID extension: vmware.vscode-boot-dev-pack |
| Docker Desktop + Compose | Opsional; hanya jika memilih database container | [Docker Desktop](https://www.docker.com/products/docker-desktop/) |
| phpMyAdmin | Opsional, untuk administrasi MySQL lewat browser | [phpMyAdmin](https://www.phpmyadmin.net/) atau versi yang tersedia di Laragon |

**Maven global tidak wajib.** Maven Wrapper di repository mengunduh Maven otomatis saat pertama dijalankan. Koneksi internet diperlukan untuk unduhan awal Maven dan dependency.

Versi yang telah diuji: Temurin **21.0.13+9**, Spring Boot **4.1.2**, Maven **3.10.1** melalui wrapper **3.3.4**, dan MySQL Laragon **8.4.3**. Dependency Spring MVC, Thymeleaf, JPA/Hibernate, Validation, dan Security mengikuti versi yang dikelola Spring Boot.

Jika software kompatibel sudah terpasang, gunakan instalasi tersebut. Jangan menghapus Java/MySQL lama atau mengubah konfigurasi global tanpa kebutuhan.

## 2. Clone dan buka proyek

Repository ini private. Anggota harus masuk menggunakan akun GitHub yang sudah mendapat akses dari pemilik repository. Jika clone menampilkan Repository not found, pastikan akun dan aksesnya benar.

Jalankan di PowerShell:

```powershell
# Pilih direktori pengembangan yang dapat ditulis.
New-Item -ItemType Directory -Path "$env:USERPROFILE\source" -Force
Set-Location "$env:USERPROFILE\source"

git clone https://github.com/rafaaryas/Pengelolaan-Laporan-Fasilitas-Kampus.git
Set-Location Pengelolaan-Laporan-Fasilitas-Kampus
code .
```

Jika perintah code tidak tersedia, buka VS Code secara manual → File → Open Folder → pilih folder hasil clone. Semua perintah selanjutnya dijalankan **dari folder yang memiliki pom.xml**, melalui Terminal → New Terminal di VS Code dengan shell PowerShell.

Gunakan folder di luar OneDrive/Documents yang dilindungi jika Windows Defender Controlled Folder Access memblokir build. Tidak perlu mematikan Windows Security.

## 3. Siapkan Java JDK 21

Pilih salah satu cara berikut.

### A. JDK 21 sudah terpasang

Arahkan environment **hanya untuk sesi terminal ini** ke folder JDK 21 yang benar:

```powershell
# Contoh path; sesuaikan dengan instalasi pada laptop Anda.
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version
javac -version
```

Keduanya harus menampilkan versi 21. JAVA_HOME menunjuk ke folder utama JDK, bukan folder bin atau java.exe. Pengaturan di atas tidak mengubah PATH/JAVA_HOME global dan perlu diulang saat membuka terminal baru jika tidak memakai JDK portable di bawah.

### B. JDK portable di dalam proyek

1. Unduh **JDK 21, Windows, x64, HotSpot, ZIP** dari halaman resmi Temurin.
2. Buat folder .tools di dalam proyek.
3. Ekstrak ZIP sehingga strukturnya seperti `.tools/jdk-21.0.13+9/bin/java.exe` dan `.tools/jdk-21.0.13+9/bin/javac.exe`. Jangan sampai ada folder ZIP tambahan di antara .tools dan jdk-21....
4. Runner otomatis mencari folder `.tools/jdk-21*`; nama patch dapat berbeda.

```powershell
New-Item -ItemType Directory -Path .tools -Force
# Setelah ZIP diekstrak ke .tools:
.\scripts\dev.ps1 -Task Info
```

Task Info harus menampilkan Maven dan **Java version: 21...**. JDK, cache Maven, dan hasil build tidak dikirim ke GitHub; setiap anggota menyiapkan JDK sendiri. Runner memilih JDK 21 dari JAVA_HOME, kemudian .tools, kemudian lokasi javac, dan tidak mengubah pengaturan sistem global.

## 4. Buat konfigurasi lokal .env

```powershell
Copy-Item .env.example .env
code .env
```

Jika .env sudah ada, edit file tersebut; jangan menimpanya karena berisi password lokal Anda. Isi variabel berikut:

| Variabel | Isi untuk Laragon lokal |
| --- | --- |
| DB_HOST | 127.0.0.1 |
| DB_PORT | 3306, atau port MySQL pada laptop Anda |
| DB_NAME | pelaporan_fasilitas |
| DB_USERNAME | kampus_app |
| DB_PASSWORD | Password user MySQL khusus aplikasi; harus sama dengan SQL pada langkah 5 |
| SERVER_PORT | 8080, atau port aplikasi lain yang masih kosong |
| DEMO_MAHASISWA_PASSWORD | Password unik akun demo mahasiswa, minimal 12 karakter |
| DEMO_ADMIN_PASSWORD | Password unik akun demo admin, minimal 12 karakter |
| DEMO_PETUGAS_PASSWORD | Password unik akun demo petugas, minimal 12 karakter |
| MYSQL_ROOT_PASSWORD | Hanya untuk Docker; boleh tetap CHANGE_ME jika memakai Laragon |

Gunakan password lokal yang berbeda untuk setiap akun. Ganti semua nilai CHANGE_ME yang diperlukan. Format file adalah `KEY=value`, tanpa tanda kutip, awalan export, substitusi variabel, atau komentar di akhir baris. Untuk memudahkan penyalinan ke SQL, gunakan password database acak berupa huruf/angka yang panjang.

**Spring Boot tidak membaca .env secara otomatis.** Saat Run atau VerifyDatabase, dev.ps1 memanggil Import-LocalEnv.ps1, meneruskan nilainya sebagai environment proses Java, lalu mengaktifkan profil local. Compose membaca .env untuk interpolasi konfigurasinya sendiri.

File .env diabaikan Git. Jangan mengirimnya melalui repository atau memasukkan password nyata ke source, dokumentasi, atau tangkapan layar.

## 5. Siapkan database — pilih A atau B

### A. MySQL Laragon dan phpMyAdmin (pilihan pada laptop setup)

phpMyAdmin adalah antarmuka untuk mengelola database. Spring Boot terhubung langsung ke **server MySQL Laragon**, bukan ke halaman phpMyAdmin.

1. Buka Laragon dan jalankan MySQL. Pastikan portnya sesuai DB_PORT.
2. Buka phpMyAdmin jika tersedia. Login dengan akun administrator MySQL lokal Anda. Gunakan kredensial yang sudah berlaku pada laptop tersebut; jangan mengubah password root untuk mengikuti tutorial.
3. Buka tab **SQL**, lalu jalankan SQL di bawah **sekali saja**. Ganti placeholder password dengan nilai DB_PASSWORD di .env terlebih dahulu.

```sql
CREATE DATABASE pelaporan_fasilitas
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER 'kampus_app'@'localhost'
    IDENTIFIED BY '<GANTI_DENGAN_DB_PASSWORD_DARI_ENV>';

GRANT SELECT, INSERT, UPDATE, DELETE
    ON pelaporan_fasilitas.*
    TO 'kampus_app'@'localhost';
```

SQL yang sama dapat dijalankan melalui terminal MySQL di Laragon jika phpMyAdmin tidak tersedia. Apache/Nginx Laragon diperlukan untuk membuka phpMyAdmin, tetapi tidak diperlukan untuk menjalankan Spring Boot.

Jika database atau user sudah ada, periksa terlebih dahulu. Jangan menjalankan ulang CREATE USER atau mengganti password user milik aplikasi lain. Pada laptop yang telah disiapkan sebelumnya, database dan user khusus ini sudah dibuat: cukup gunakan .env lokal yang sudah ada.

Tidak diperlukan import dump SQL bisnis pada fondasi ini. Belum ada entity/tabel bisnis. Hibernate memakai `ddl-auto=validate`, tidak membuat atau menghapus skema otomatis. Setelah entity dikembangkan, integrator perlu menyediakan migrasi schema yang ditinjau. User aplikasi tidak diberi hak DDL global.

### B. MySQL melalui Docker Compose (alternatif)

Lewati pilihan ini jika sudah memakai MySQL Laragon.

1. Pastikan Docker Desktop sudah berjalan dan engine dapat diakses.
2. Set DB_HOST=127.0.0.1 dan **DB_PORT=3307** di .env agar tidak bentrok dengan Laragon di 3306.
3. Isi DB_NAME, DB_USERNAME, DB_PASSWORD serta MYSQL_ROOT_PASSWORD unik di .env.
4. Jalankan:

```powershell
docker --version
docker compose version
docker info
docker compose up -d --wait
docker compose ps
```

Compose menyediakan MySQL 8.4, user aplikasi, database, healthcheck, dan volume persisten mysql_data. Koneksi hanya diekspos ke loopback laptop. Environment MYSQL_* dipakai untuk inisialisasi volume pertama; mengganti .env tidak otomatis mengganti password pada volume yang sudah terisi.

Untuk berhenti dan melanjutkan tanpa menghapus data:

```powershell
docker compose stop
docker compose start
```

Jangan menghapus volume/data untuk mengatasi error koneksi. Alternatif Compose tersedia, tetapi pengujian database pada setup awal dilakukan terhadap MySQL Laragon.

## 6. Build, uji database, dan jalankan aplikasi

```powershell
# Periksa Maven Wrapper dan JDK yang dipakai.
.\scripts\dev.ps1 -Task Info

# Download dependency, compile, jalankan test, dan buat JAR.
.\scripts\dev.ps1 -Task Package

# Pastikan MySQL aktif dan .env benar; uji koneksi JDBC sungguhan.
.\scripts\dev.ps1 -Task VerifyDatabase

# Jalankan aplikasi dengan profil local.
.\scripts\dev.ps1 -Task Run
```

Unduhan awal dapat memerlukan waktu. Package dan VerifyDatabase harus menampilkan **BUILD SUCCESS**. Run akan tetap berjalan di terminal; tunggu pesan Tomcat started on port 8080 dan Started PelaporanFasilitasKampusApplication.

Buka **[http://localhost:8080](http://localhost:8080)**. Jika SERVER_PORT diganti, gunakan port itu pada URL. Hentikan aplikasi dengan **Ctrl+C** pada terminal yang menjalankannya. Jangan menjalankan dua instance pada port yang sama.

Runner menaruh Maven dan dependency di .cache, memilih JDK 21, dan memuat environment lokal bila diperlukan. Tidak perlu menginstal Maven secara global. Maven Wrapper resmi juga tersedia sebagai mvnw.cmd untuk Windows dan mvnw untuk shell Unix; panduan runner di repository ini ditujukan untuk Windows.

## 7. Login dan akses halaman

| Halaman | Akses | Nama pengguna demo | Password |
| --- | --- | --- | --- |
| / | Publik | Tidak perlu | Tidak perlu |
| /login | Publik | Sesuai akun | Sesuai akun |
| /mahasiswa | Role MAHASISWA | mahasiswa | DEMO_MAHASISWA_PASSWORD di .env |
| /admin | Role ADMIN | admin | DEMO_ADMIN_PASSWORD di .env |
| /petugas | Role PETUGAS | petugas | DEMO_PETUGAS_PASSWORD di .env |

Klik halaman sesuai role, lalu login. Pengguna yang belum login diarahkan ke /login. Role yang tidak sesuai menerima HTTP 403. Gunakan **Keluar akun** sebelum berganti akun. CSRF tetap aktif pada login dan logout.

Akun ini hanya untuk pengembangan pada profil local. Password di-hash BCrypt dalam memori saat startup dan tidak disimpan sebagai plaintext di repository. Autentikasi produksi dan registrasi belum dibuat.

## 8. Test dan menjalankan hasil JAR

```powershell
.\scripts\dev.ps1 -Task Test            # Test MVC, Thymeleaf, login, role, dan CSRF; tanpa MySQL.
.\scripts\dev.ps1 -Task Package         # clean verify dan membuat JAR; tanpa MySQL.
.\scripts\dev.ps1 -Task VerifyDatabase  # Test terpisah ke MySQL sungguhan; membutuhkan .env.
```

Hasil verifikasi awal: **11 test MVC/security lulus** dan **1 test koneksi MySQL lulus**. Pengujian database diberi tag mysql dan dipisahkan agar test halaman tetap dapat dijalankan tanpa server database. Untuk memverifikasi setup penuh, jalankan Package serta VerifyDatabase.

Setelah Package berhasil, JAR berada di `target/pelaporan-fasilitas-kampus-0.0.1-SNAPSHOT.jar`. Untuk menjalankannya di PowerShell:

```powershell
# Info menyiapkan JDK 21 pada sesi terminal yang sama.
.\scripts\dev.ps1 -Task Info
.\scripts\Import-LocalEnv.ps1
$env:SPRING_PROFILES_ACTIVE = 'local'
java -jar .\target\pelaporan-fasilitas-kampus-0.0.1-SNAPSHOT.jar
```

MySQL tetap harus berjalan. Gunakan Run ATAU JAR, bukan keduanya pada port yang sama.

## 9. Menggunakan VS Code

1. Buka folder yang berisi pom.xml.
2. Pasang Java Extension Pack jika belum tersedia; Spring Boot Extension Pack opsional.
3. Tunggu import proyek Maven selesai.
4. Buka Terminal → Run Task dan pilih tugas **Kampus: Periksa Java dan Maven**, **Kampus: Build dan test**, **Kampus: Uji koneksi MySQL**, atau **Kampus: Jalankan aplikasi**.
5. Alternatifnya jalankan perintah dev.ps1 di terminal PowerShell VS Code.

Task memakai runner yang sama dengan terminal. Jangan mengandalkan tombol Run Java yang belum diberi environment database. Language server VS Code boleh memakai JDK yang lebih baru, sementara build proyek menggunakan JDK 21.

## 10. Struktur file dan tanggung jawab

```text
.mvn/                  Konfigurasi Maven Wrapper
.vscode/               Rekomendasi extension dan task editor
scripts/               Runner PowerShell dan importer .env
docs/                  Arsitektur, setup, workflow tim, laporan verifikasi
src/main/java/com/kelompok5/pelaporanfasilitas/
  PelaporanFasilitasKampusApplication.java   Entry point aplikasi
  config/              Akun demo profil local
  controller/          Controller halaman MVC
  model/               Tempat model domain OOP
  repository/          Tempat repository JPA
  service/prioritas/   Tempat service dan strategi prioritas
  security/            Route authorization dan password encoder
  exception/           Tempat penanganan exception modul mendatang
src/main/resources/
  application.properties         Konfigurasi umum dan placeholder environment
  application-local.properties   Konfigurasi pengembangan lokal
  templates/           HTML Thymeleaf, fragments, halaman setiap role
  static/              CSS, JavaScript, images
src/test/java/         Test MVC/security dan koneksi MySQL
.env.example           Contoh konfigurasi tanpa password nyata
compose.yaml           Alternatif database Docker
pom.xml                Dependency dan konfigurasi Maven
mvnw / mvnw.cmd        Menjalankan Maven versi proyek
```

File HTML Thymeleaf dijalankan melalui controller Spring Boot; **jangan membuka HTML langsung dari File Explorer atau Live Server**. CSS/JS dilayani aplikasi dari static. .env, .tools, .cache, dan target dibuat per laptop dan tidak tersedia setelah clone.

## 11. Pembagian kerja lima anggota

| Anggota | Tanggung jawab | Branch |
| --- | --- | --- |
| 1 | Fullstack/integrator, environment, config, database, security, integrasi | feature/setup-auth |
| 2 | Frontend mahasiswa, form laporan, riwayat | feature/frontend-mahasiswa |
| 3 | Frontend admin/petugas, verifikasi, penugasan, monitoring | feature/frontend-admin-petugas |
| 4 | Backend fasilitas, laporan, kasus, pencarian dan penggabungan laporan | feature/backend-pelaporan |
| 5 | Backend petugas, tugas perbaikan, strategi prioritas, verifikasi/penugasan | feature/backend-prioritas-perbaikan |

Semua anggota memakai satu repository. Setelah clone:

```powershell
git switch main
git pull --ff-only origin main
git switch -c feature/frontend-mahasiswa
```

Ganti branch sesuai tugas. Jangan membuat ulang proyek Spring Boot di branch sendiri. Koordinasikan perubahan domain bersama pemilik modul. Setelah perubahan selesai, jalankan Package, periksa git diff, commit file yang relevan, push branch, dan buka pull request ke main. Minimal satu anggota lain meninjau sebelum integrator merge. Jalankan VerifyDatabase untuk perubahan persistence.

Lihat [TEAM_WORKFLOW.md](docs/TEAM_WORKFLOW.md) untuk detail workflow dan rencana 14 hari, serta [ARCHITECTURE.md](docs/ARCHITECTURE.md) untuk pemetaan 10 class + 1 interface dan keputusan JPA.

## 12. Troubleshooting

| Masalah | Langkah pemeriksaan |
| --- | --- |
| JDK 21 belum ditemukan / invalid target release 21 | Pastikan JDK lengkap; periksa JAVA_HOME atau struktur .tools/jdk-21.../bin. Jalankan Info. |
| java -version di terminal baru masih Java 25 | Runner memakai Java 21 hanya pada sesi yang menjalankannya. Jalankan Info di terminal tersebut atau set JAVA_HOME sesi ke JDK 21. |
| Script PowerShell diblokir | Ikuti kebijakan laptop. Jika diizinkan, gunakan Set-ExecutionPolicy -Scope Process -ExecutionPolicy RemoteSigned untuk sesi itu saja; jangan mengubah kebijakan sistem global. |
| Wrapper/dependency gagal diunduh | Periksa internet/proxy dan ulangi. Jangan menonaktifkan TLS atau melewati test. |
| Port 8080 sudah dipakai | Ubah SERVER_PORT di .env; periksa instance aplikasi yang masih aktif, jangan menghentikan service lain sembarangan. |
| Access denied for user kampus_app | Cocokkan DB_PASSWORD di .env dengan user MySQL dan pastikan grant host/database benar. |
| Communications link failure | Pastikan MySQL Laragon/Docker aktif dan DB_HOST/DB_PORT benar. |
| Docker tidak dapat terhubung ke engine | Buka Docker Desktop dan tunggu engine siap, atau gunakan MySQL Laragon. |
| Port database bentrok | Gunakan Laragon pada 3306 atau Compose pada 3307 sesuai .env; jangan memakai keduanya pada port sama. |
| Login gagal | Nama akun harus sesuai tabel role; isi password DEMO_* minimal 12 karakter dan restart aplikasi setelah mengubah .env. |
| HTTP 403 | Periksa role akun; login/logout POST juga memerlukan token CSRF. Gunakan form halaman. |
| Template tidak diproses | Akses melalui http://localhost:8080, bukan file HTML/Live Server. |
| Missing table setelah menambah entity | Jalankan migrasi schema yang disepakati; jangan mengubah ddl-auto menjadi create/create-drop. |
| Gagal menulis file di OneDrive/Documents | Gunakan workspace pengembangan yang diizinkan di luar folder terlindungi atau atur izin aplikasi melalui pemilik laptop. |

Panduan lebih lanjut: [DEVELOPMENT_SETUP.md](docs/DEVELOPMENT_SETUP.md). Catatan audit dan batas verifikasi awal: [SETUP_REPORT.md](docs/SETUP_REPORT.md).

## 13. Catatan laptop setup awal

Pada laptop integrator, folder aktif adalah `C:\Users\USER\source\pelaporan-fasilitas-kampus`, memakai MySQL Laragon di 3306. Folder clone anggota lain boleh berbeda. Gunakan README ini sebagai panduan portabel, bukan menyalin path/password laptop integrator. Runtime lokal dan password sengaja tidak diunggah; anggota menyiapkan JDK serta .env masing-masing.
