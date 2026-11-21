# Laporan setup — 19 November 2026

> Catatan fondasi awal sebelum MVP. Panduan menjalankan aplikasi dan ruang kerja demo saat ini ada di [README](../README.md).

**Status: fondasi selesai dan terverifikasi berjalan secara lokal.**

Proyek aktif: `C:\Users\USER\source\pelaporan-fasilitas-kampus`.
URL: http://localhost:8080 . MySQL Laragon: 127.0.0.1:3306.

## Environment dan versi aktual

| Komponen | Hasil audit / tindakan |
| --- | --- |
| OS | Windows 11 Home Single Language 10.0.26200, 64-bit |
| Java sistem sebelum setup | Oracle JDK 25.0.1 aktif; JDK 17 dan Java 8 juga ditemukan; JAVA_HOME kosong |
| Java proyek | Temurin JDK 21.0.13+9 LTS portable di .tools/jdk-21.0.13+9; java dan javac berhasil |
| Maven | Global tidak tersedia; Maven 3.10.1 berjalan melalui wrapper resmi 3.3.4 |
| Spring Boot | 4.1.2; Hibernate 7.4.13.Final dan Spring Security 7.1.2 dari dependency management Boot |
| Git | 2.52.0.windows.1; repo lokal baru di proyek aktif pada branch main, belum ada commit atau remote |
| VS Code | 1.146.0 x64; Java Extension Pack, debugger, Java Test, Maven extension sudah terpasang |
| Spring editor tools | Spring Boot Extension Pack opsional direkomendasikan; tidak dipasang otomatis |
| Docker | CLI 29.8.2, Compose v5.5.1; engine tidak aktif saat audit, tidak dijalankan karena memilih Laragon |
| MySQL | Laragon 8.4.3 yang sudah berjalan pada port 3306 digunakan kembali |
| Ruang disk | Sekitar 9,6 GiB kosong sebelum setup; sekitar 8,6 GiB setelah JDK/cache/build |
| Port aplikasi | 8080 awalnya bebas; aplikasi kini bind hanya 127.0.0.1:8080 |
| Port alternatif | 3307 bebas saat audit; Compose disediakan sebagai alternatif, belum dijalankan |

Java versi lama, PATH/JAVA_HOME global, konfigurasi Windows Security, service/database lain, dan Docker volumes tidak diubah. Runner memilih Java 21 dan mengatur environment hanya untuk proses/sesi terminal. Menjalankan java langsung di terminal baru tanpa runner dapat tetap memakai Java 25 dari sistem.

## Software dan konfigurasi yang dibuat

- Setelah persetujuan pengguna, Temurin JDK 21 ZIP resmi diunduh ke .tools dan diekstrak. SHA-256 cocok dengan metadata Adoptium: `a4dbaa05fca4bd89d84584e58c459bd732efebb1722af80ab13c6516832004f9`.
- Wrapper resmi dari Spring Initializr diambil tanpa mengganti instalasi Maven global. Cache Maven/dependency berada di .cache proyek ketika memakai dev.ps1.
- Database baru pelaporan_fasilitas (utf8mb4) dan user kampus_app@localhost dibuat setelah memeriksa keduanya belum ada. Hak aplikasi hanya SELECT, INSERT, UPDATE, DELETE pada pelaporan_fasilitas.*. Root hanya dipakai pada provisioning administratif, bukan JDBC aplikasi.
- Password acak unik database dan tiga akun demo dibuat di .env lokal. git check-ignore mengonfirmasi .env, .tools dan .cache diabaikan. Tidak ada password aktual di source/repository.
- Proyek Spring MVC + Thymeleaf, konfigurasi JPA/Validation/Security, profil local, Compose alternatif, halaman dasar, test, skrip, dan dokumentasi tim tersedia. Tidak ada skema bisnis final atau class domain buatan di luar rancangan.
- Git lokal diinisialisasi pada main. Tidak ada commit, remote GitHub, atau push.

## Struktur dan tanggung jawab

pom.xml dan .mvn berisi build; src/main/java berisi controller, config, security, serta package model/repository/service/exception; templates berisi beranda/login/fragment/placeholder tiga role; static berisi CSS/JS/images. src/test/java memuat uji MVC/security dan koneksi MySQL nyata. scripts berisi runner dan importer .env. docs memuat ARCHITECTURE.md, TEAM_WORKFLOW.md, DEVELOPMENT_SETUP.md dan laporan ini. .vscode memuat rekomendasi extension dan task untuk menjalankan perintah setup yang sama.

## Pengujian yang benar-benar dijalankan

| Pemeriksaan | Hasil |
| --- | --- |
| java -version / javac -version dengan JDK portable | 21.0.13; berhasil |
| git --version, code --version | Berhasil |
| docker --version / docker compose version | Berhasil; docker info gagal karena engine berhenti, dilaporkan sesuai kondisi |
| dev.ps1 -Task Info | Maven 3.10.1 memakai Java 21.0.13 dari .tools |
| dev.ps1 -Task Package | clean verify, compile dan packaging JAR: BUILD SUCCESS |
| PageSecurityTest | 11 test lulus, 0 failure/error/skipped |
| dev.ps1 -Task VerifyDatabase | 1 test integrasi lulus; Spring context/JPA/Hikari terkoneksi ke MySQL 8.4.3 dengan kampus_app |
| Startup aplikasi | Spring Boot 4.1.2 dan Tomcat berhasil berjalan pada 8080; tidak ada startup exception |
| Browser beranda/login/3 halaman role | Berhasil dirender melalui controller dan Thymeleaf |
| CSS dan JavaScript | HTTP 200; tidak ada error/warning console pada beranda terakhir |
| Thymeleaf | Judul/model username/fragment ter-render; atribut th:text/th:href/th:replace tidak tersisa pada hasil beranda |
| Login dan logout nyata | Ketiga akun berhasil login dan membuka halaman sendiri; logout lewat form berhasil |
| CSRF | Token ada pada form; automated test menolak login/logout POST tanpa token dan menerima logout dengan token |
| Responsive | Pemeriksaan visual desktop 1366x768 dan mobile 390x844; lebar konten sama dengan viewport pada 390 dan 320 px |

Matriks HTTP pada sesi browser setelah login:

| Akun | /mahasiswa | /admin | /petugas |
| --- | --- | --- | --- |
| mahasiswa | 200 | 403 | 403 |
| admin | 403 | 200 | 403 |
| petugas | 403 | 403 | 200 |

Sebelum login, ketiga route mengarahkan ke /login. HTTP 403 lintas role adalah hasil proteksi yang diharapkan; tidak ditemukan 404/500 pada halaman dan aset yang dibuat. Browser pengujian dikembalikan ke beranda dalam keadaan logout.

Bukti lokal: .cache/build.log, .cache/database-test.log, .cache/application.log serta target/surefire-reports. Cache dan laporan test ini tidak masuk Git. File JAR: target/pelaporan-fasilitas-kampus-0.0.1-SNAPSHOT.jar. Proses Java aplikasi saat verifikasi memiliki PID 28220, namun PID akan berubah ketika dijalankan ulang.

## Masalah yang ditemukan dan diselesaikan

1. Controlled Folder Access memblokir PowerShell, curl, dan Git pada workspace OneDrive (log Defender 1123). Dengan izin pengguna, proyek disalin ke folder source; pengaturan keamanan tidak diubah. Salinan awal OneDrive dipertahankan dan README lokasi awal menunjuk ke proyek aktif.
2. JDK 21 tidak tersedia; portable Temurin dipasang di workspace dengan izin. Tidak ada perubahan global.
3. Metadata Initializr menghasilkan 4.1.2.RELEASE yang tidak tersedia di Maven Central. pom menggunakan 4.1.2 yang diverifikasi tersedia dan berhasil dibuild.
4. Argumen tunggal pada runner PowerShell awal terurai salah. Diperbaiki dengan array string eksplisit, lalu Info/Package/VerifyDatabase/Run diuji ulang.
5. Tiga assertion awal test mengharapkan pola redirect URL yang tidak cocok dengan redirect relatif /login dari versi Security ini. Assertion diperbaiki menjadi URL /login yang tepat; seluruh test dijalankan ulang dan lulus, tanpa menonaktifkan security/test.
6. JVM test menampilkan peringatan dynamic agent Mockito/Byte Buddy; test tetap lulus. Peringatan tidak disembunyikan dan bukan startup error aplikasi.
7. Penyimpanan screenshot oleh tool browser ke folder source ditolak batas root tool tersebut. Pemeriksaan visual tetap dilakukan melalui screenshot inline dari tool browser; tidak diklaim ada screenshot tersimpan di proyek.

## Batas verifikasi dan tugas berikutnya

Docker Compose hanya disediakan sebagai alternatif; tidak diuji menjalankan container karena pengguna memilih Laragon. phpMyAdmin tidak perlu diubah untuk JDBC dan UI phpMyAdmin tidak diuji. VS Code dan extension Java yang sudah terpasang diperiksa melalui CLI; terminal terintegrasi VS Code tidak dioperasikan langsung. Gunakan task Kampus atau dev.ps1 agar proses memakai JDK 21 serta environment lokal yang sama.

Akun in-memory hanya fondasi development; autentikasi produksi belum dibuat. Tidak ada entity/tabel bisnis saat setup. Anggota perlu menyepakati kontrak dan mengimplementasikan 10 class + 1 interface, schema/migrasi, laporan, verifikasi, prioritas, penugasan serta progres sesuai ARCHITECTURE.md dan TEAM_WORKFLOW.md. ddl-auto tetap validate; jangan memakai create-drop pada MySQL persisten.

## Menjalankan ulang

Buka folder aktif di VS Code. Jika instance setup masih berjalan, jangan memulai instance kedua pada port yang sama. Untuk menghentikan instance setup, periksa proses port 8080 terlebih dahulu; hanya hentikan PID aplikasi proyek yang benar (PID 28220 saat laporan ini). Saat menjalankan sendiri dengan dev.ps1 -Task Run, gunakan Ctrl+C pada terminal itu.

```powershell
cd C:\Users\USER\source\pelaporan-fasilitas-kampus
.\scripts\dev.ps1 -Task Package
.\scripts\dev.ps1 -Task VerifyDatabase
.\scripts\dev.ps1 -Task Run
```

Pastikan MySQL Laragon aktif sebelum menjalankan. Akun demo: mahasiswa/admin/petugas; password ada pada DEMO_*_PASSWORD di .env lokal. Jangan membagikan .env lewat repository.

Sumber resmi yang dipakai: [Spring Initializr](https://start.spring.io/), [system requirements Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html), [Maven Central](https://repo.maven.apache.org/maven2/), [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=21), dan dokumentasi Spring Security melalui Context7.

## Pembaruan publikasi repository

Setelah fase setup di atas, pengguna memberikan izin membuat repository GitHub baru dan melakukan commit/push. Repository tujuan adalah https://github.com/rafaaryas/Pengelolaan-Laporan-Fasilitas-Kampus dengan visibilitas private. README utama diperbarui menjadi panduan setup lengkap untuk anggota. Pernyataan belum ada remote/commit pada tabel audit di atas menggambarkan kondisi akhir fase setup awal, sebelum permintaan publikasi ini.
