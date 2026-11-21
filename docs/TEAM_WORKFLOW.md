# Kerja kelompok selama 14 hari

> Catatan fondasi awal sebelum MVP. Panduan menjalankan aplikasi dan ruang kerja demo saat ini ada di [README](../README.md).

Satu repository, satu proyek Spring Boot. Jangan generate ulang proyek di branch anggota. Branch utama adalah `main`; anggota mengembangkan modul di feature branch dan menggabungkannya melalui pull request.

| Anggota | Branch yang disarankan | Area utama |
| --- | --- | --- |
| 1 Fullstack / integrator | feature/setup-auth | config, security, database, pom, integrasi |
| 2 Frontend mahasiswa | feature/frontend-mahasiswa | templates/mahasiswa, CSS/JS khusus modul |
| 3 Frontend admin & petugas | feature/frontend-admin-petugas | templates/admin, templates/petugas |
| 4 Backend pelaporan | feature/backend-pelaporan | Fasilitas, LaporanKerusakan, KasusKerusakan, SistemPelaporan, pencarian/penggabungan laporan |
| 5 Backend prioritas & perbaikan | feature/backend-prioritas-perbaikan | Petugas, TugasPerbaikan, StrategiPrioritas, PrioritasBerbasisRisiko, verifikasi/penugasan |

Pengguna, Mahasiswa, Admin, route, DTO/form bila perlu, dan relasi lintas modul disepakati bersama integrator. Jangan mengubah domain milik anggota lain tanpa koordinasi. Fragment umum dan app.css dikoordinasikan integrator agar dua frontend tidak sering konflik.

## Urutan harian

Hari 1–2: semua laptop bisa build; sepakati kontrak domain, schema SQL, status, route, prioritas berbasis risiko, dan template attribute. Hari 3–6: backend use case dan frontend dikerjakan paralel dengan kontrak yang sama. Hari 7–10: integrasikan alur laporan → verifikasi → prioritas → penugasan → progres. Hari 11–12: pengujian role, validasi, transisi status, konflik/penggabungan kasus. Hari 13–14: perbaikan, dokumentasi, dan latihan demo.

## Mulai branch (setelah integrator membuat commit awal dan remote)

```powershell
git switch main
git pull --ff-only origin main
git switch -c feature/frontend-mahasiswa
```

Ganti nama branch sesuai pembagian. Buat perubahan kecil; jangan memasukkan .env, password, target, .cache, atau .tools ke commit. Setelah memeriksa diff:

```powershell
.\scripts\dev.ps1 -Task Package
git status
git diff --check
git add <file-yang-sudah-ditinjau>
git commit -m "Tambahkan tampilan mahasiswa"
git push -u origin feature/frontend-mahasiswa
```

Perintah commit/push di atas untuk anggota, tidak dijalankan otomatis oleh setup. Buka pull request ke main, jelaskan perubahan, cara uji, dan perubahan schema bila ada. Minimal satu anggota lain meninjau, integrator memastikan build/test lulus sebelum merge. Jalankan VerifyDatabase jika mengubah persistence. Jangan langsung push ke main.

Sinkronkan main secara rutin. Selesaikan konflik dengan pemilik file; jangan menimpa pekerjaan anggota lain atau force push branch bersama. Jangan membuat remote GitHub tanpa keputusan kelompok.
