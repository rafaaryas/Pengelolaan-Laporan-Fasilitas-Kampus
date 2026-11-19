# Arsitektur dan kontrak awal

Proyek Advance Programming Kelompok 5, Offering C, Teknik Informatika Universitas Negeri Malang. Monolith Spring MVC: browser mengirim request ke Controller, Controller memanggil service/domain, repository mengakses MySQL, lalu Thymeleaf merender HTML. Tidak ada frontend build atau REST API terpisah.

## Pemetaan rancangan OOP

| Class/interface | Package yang disepakati | Tanggung jawab / pemilik |
| --- | --- | --- |
| Pengguna (abstract) | model | Identitas bersama; kontrak disepakati integrator |
| Mahasiswa | model | Peran pelapor; integrator + backend pelaporan |
| Admin | model | Peran verifikator; integrator |
| Petugas | model | Pelaksana perbaikan; backend prioritas/perbaikan |
| Fasilitas | model | Data fasilitas; backend pelaporan |
| LaporanKerusakan | model | Laporan individual; backend pelaporan |
| KasusKerusakan | model | Kelompok laporan untuk satu kerusakan; backend pelaporan |
| SistemPelaporan | service | Orkestrasi use case pelaporan; backend pelaporan |
| StrategiPrioritas (interface) | service.prioritas | Kontrak perhitungan prioritas; backend prioritas/perbaikan |
| PrioritasBerbasisRisiko | service.prioritas | Implementasi strategi; backend prioritas/perbaikan |
| TugasPerbaikan | model | Penugasan dan progres pekerjaan; backend prioritas/perbaikan |

Class bisnis belum dibuat: fondasi ini tidak mengarang atribut, rumus risiko, status, atau relasi yang belum disepakati. Package disediakan lewat package-info.java, bukan banyak class kosong. Controller, SecurityConfig, dan konfigurasi demo adalah pendukung framework, bukan tambahan domain.

## Keputusan JPA

- Tidak ada entity atau tabel bisnis saat setup. `ddl-auto=validate` tidak membuat/menghapus tabel; setelah entity pertama ditambahkan, tim perlu menyediakan skrip migrasi skema yang ditinjau sebelum startup.
- Pertahankan Pengguna abstract. Pilihan `@MappedSuperclass` versus `@Entity` dengan inheritance harus disepakati setelah kebutuhan query lintas peran dan relasi diketahui. Jangan menambah tabel pengguna ganda tanpa kesepakatan.
- Entity memerlukan ID stabil serta constructor tanpa argumen setidaknya protected. Tetap gunakan constructor/method domain untuk menjaga invariant, dan kode Java eksplisit tanpa Lombok.
- SistemPelaporan dan strategi adalah objek service, bukan entity. Aturan bisnis di model/strategi, bukan template atau Controller; transaksi use case nantinya di service.
- `open-in-view=false`: service menyediakan data yang diperlukan view selama transaksi, jangan mengandalkan lazy loading dari template.
- Akun in-memory hanya profil local. Belum ada repository autentikasi produksi. Di luar local, autentikasi menolak semua akun sampai mekanisme produksi dibuat.

## Kontrak halaman

| Route | View | Akses | Pemilik |
| --- | --- | --- | --- |
| GET / | index | Publik | Integrator |
| GET /login | login | Publik | Integrator |
| POST /login | Spring Security | CSRF wajib | Integrator |
| POST /logout | Spring Security | CSRF wajib | Integrator |
| GET /mahasiswa | mahasiswa/index | MAHASISWA | Frontend mahasiswa |
| GET /admin | admin/index | ADMIN | Frontend admin/petugas |
| GET /petugas | petugas/index | PETUGAS | Frontend admin/petugas |

Sebelum memperluas modul, sepakati atribut/tipe ID, enum status, aturan transisi status, signature StrategiPrioritas, nama model attribute Thymeleaf, route dan validasi form. Jangan menganggap placeholder sebagai kontrak data bisnis final.
