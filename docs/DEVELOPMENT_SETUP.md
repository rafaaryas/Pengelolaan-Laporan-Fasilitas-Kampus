# Setup pengembangan Windows

> Catatan fondasi awal sebelum MVP. Panduan menjalankan aplikasi dan ruang kerja demo saat ini ada di [README](../README.md).

## JDK dan editor

JDK 21 LTS wajib, Maven global tidak diperlukan. Distribusi resmi: [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=21). Gunakan ZIP Windows x64 jika tidak ingin mengubah sistem: ekstrak folder jdk-21... ke `.tools`. Jangan menghapus JDK lama.

Jika JDK 21 ada di lokasi lain, set hanya untuk sesi terminal:

```powershell
$env:JAVA_HOME = 'C:\lokasi\jdk-21'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version
javac -version
```

Skrip dev.ps1 mencari JDK 21 di JAVA_HOME, `.tools/jdk-21*`, lalu lokasi javac. Skrip hanya mengatur environment proses/sesi, bukan PATH/JAVA_HOME pengguna atau sistem. Gunakan `.\scripts\dev.ps1 -Task Info` untuk memeriksa Java dan Maven. Wrapper resmi mengunduh Maven saat pertama kali dijalankan. Cache wrapper dan dependency disimpan di `.cache` workspace ketika memakai skrip.

Buka folder proyek di VS Code. Java Extension Pack direkomendasikan; Spring Boot Extension Pack opsional. `.vscode/extensions.json` hanya rekomendasi, tidak memasang extension otomatis. Setelah membuka terminal PowerShell VS Code, jalankan dev.ps1 agar Maven memakai JDK 21; jangan memakai tombol Run Java yang belum diberi environment database. VS Code dapat menggunakan JDK yang lebih baru untuk language server, sementara compiler proyek tetap Java 21. Instal extension hanya bila diperlukan dan disetujui.

## MySQL Laragon / phpMyAdmin

phpMyAdmin adalah antarmuka administrasi; MySQL Laragon adalah server yang dihubungi JDBC. Jalankan MySQL melalui Laragon. Port umum 3306, namun ikuti konfigurasi laptop sendiri. Apache/Nginx Laragon hanya diperlukan jika ingin membuka phpMyAdmin, bukan untuk Spring Boot.

1. Salin `.env.example` menjadi `.env`, isi password lokal unik.
2. Administrator membuat database `pelaporan_fasilitas` dengan utf8mb4 serta user aplikasi khusus. Jangan gunakan root untuk Spring Boot.
3. Batasi hak user ke database proyek. Jangan memberi GRANT global `*.*`.
4. Set DB_HOST=127.0.0.1 dan DB_PORT sesuai server. Jalankan `VerifyDatabase`.

Contoh SQL yang perlu disesuaikan oleh administrator di phpMyAdmin (jangan simpan password nyata ke repository):

```sql
CREATE DATABASE pelaporan_fasilitas CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'kampus_app'@'localhost' IDENTIFIED BY '<password-unik-dari-env-lokal>';
GRANT SELECT, INSERT, UPDATE, DELETE ON pelaporan_fasilitas.* TO 'kampus_app'@'localhost';
```

Tidak ada tabel bisnis saat ini. Hak DDL/migrasi dikelola terpisah oleh integrator; aplikasi tidak membuat tabel otomatis. Jika database/user sudah ada, periksa kepemilikan dan haknya terlebih dahulu; jangan mengubah password user yang sedang dipakai aplikasi lain.

## Alternatif Docker Compose

Gunakan hanya jika Docker Desktop engine sudah berjalan. Tidak perlu memasang MySQL lain bila Laragon tersedia. Dengan Laragon di 3306, set DB_PORT=3307 di `.env`, isi MYSQL_ROOT_PASSWORD lokal unik, lalu:

```powershell
docker compose up -d --wait
docker compose ps
.\scripts\dev.ps1 -Task VerifyDatabase
```

Compose membaca `.env` untuk interpolasi. Image MySQL memproses MYSQL_* hanya saat volume pertama kali diinisialisasi; mengganti .env tidak mengubah password di volume lama. Jangan menghapus volume untuk mengatasi masalah password. Volume `mysql_data` persisten. Gunakan `docker compose stop` untuk berhenti dan `docker compose start` untuk melanjutkan. Bind database hanya ke loopback; jangan menjalankan Compose pada port Laragon yang sudah dipakai.

## Bagaimana .env masuk ke Java

Spring Boot tidak otomatis memuat `.env`. `scripts/Import-LocalEnv.ps1` membaca format KEY=value dengan daftar variabel yang diperbolehkan, lalu meneruskannya melalui environment proses. Skrip tidak menjalankan isi file. `dev.ps1 Run` dan `VerifyDatabase` memanggil importer serta mengaktifkan profil `local`. Java membaca `${DB_USERNAME}`, `${DB_PASSWORD}`, dan password demo dari environment tersebut. Test MVC biasa memakai password acak sementara dan tidak membutuhkan database.

Untuk menjalankan JAR dalam sesi baru:

```powershell
.\scripts\dev.ps1 -Task Info
.\scripts\Import-LocalEnv.ps1
$env:SPRING_PROFILES_ACTIVE = 'local'
java -jar .\target\pelaporan-fasilitas-kampus-0.0.1-SNAPSHOT.jar
```

Profil local menambahkan tiga akun demo (`mahasiswa`, `admin`, `petugas`) dengan password dari masing-masing DEMO_*_PASSWORD. Password di-hash BCrypt dalam memori saat startup; akun tidak ditulis ke database. Jangan gunakan profil ini untuk deployment publik. Autentikasi produksi, HTTPS dan pengelolaan akun belum merupakan cakupan setup ini.

## Troubleshooting

- `JAVA_HOME` salah / invalid target release 21: jalankan Info dan pastikan JDK lengkap, bukan JRE.
- Port 8080 dipakai: ganti SERVER_PORT di .env, jangan menghentikan proses lain tanpa izin.
- Port 3306 dipakai Laragon: pakai MySQL itu atau pilih 3307 untuk Compose.
- Access denied: pastikan DB_USERNAME/DB_PASSWORD dan host grant cocok; jangan ganti root atau user lain.
- Communications link failure: periksa MySQL berjalan, port, host, serta firewall.
- Table missing setelah menambah entity: jalankan migrasi schema yang telah ditinjau. Jangan mengganti ddl-auto ke create/create-drop.
- Redirect login: route role memerlukan sesi login; 403 berarti role tidak sesuai atau CSRF hilang. Logout harus POST dari form.
- Error password demo: isi semua DEMO_*_PASSWORD unik minimal 12 karakter; jangan gunakan CHANGE_ME.
- Unduhan Maven gagal: periksa koneksi/proxy dan coba ulang; jangan menonaktifkan verifikasi TLS atau test.
- Defender Controlled Folder Access / file tidak dapat dibuat di OneDrive: gunakan workspace pengembangan yang diizinkan di luar folder terlindungi, atau minta pemilik laptop mengizinkan aplikasi terkait. Jangan mematikan perlindungan secara otomatis.
- PowerShell execution policy memblokir skrip: ikuti kebijakan laptop; gunakan terminal yang diizinkan atau minta administrator. Setup tidak mengubah execution policy global.

Hentikan Spring Boot foreground menggunakan Ctrl+C. Jangan menghapus data/volume untuk troubleshooting. Direktori cache/JDK tidak perlu disalin ke anggota lain; masing-masing menggunakan instalasi lokalnya.
