"""Membuat V100__data_demo_layar.sql: data persis seperti 13 layar acuan UM Peduli."""
import sys, random, datetime as dt
from collections import Counter, defaultdict

OUT = sys.argv[1]
rnd = random.Random(20261120)
D = dt.datetime

# ---------- Pengguna ----------
ADMIN = "admin@example.invalid"
petugas = [  # kode, nama, kontak, bidang, baru?
    ("PTG-001", "Budi Santoso", "budi.santoso@example.invalid", "KELISTRIKAN_AC", True),
    ("PTG-002", "Agus Setiawan", "agus@example.invalid", "BANGUNAN", False),
    ("PTG-003", "Siti Rahmawati", "siti@example.invalid", "SANITASI", False),
    ("PTG-004", "Rudi Hartono", "rudi@example.invalid", "PERABOT", False),
    ("PTG-005", "Dewi Lestari", "dewi.lestari@example.invalid", "JARINGAN", True),
    ("PTG-006", "Dimas Pratama", "dimas@example.invalid", "BANGUNAN", False),
    ("PTG-007", "Raka Santoso", "raka@example.invalid", "SANITASI", False),
    ("PTG-008", "Hendra Wijaya", "hendra.wijaya@example.invalid", "KELISTRIKAN_AC", True),
    ("PTG-009", "Lukman Hakim", "lukman.hakim@example.invalid", "SANITASI", True),
    ("PTG-010", "Yusuf Ramadhan", "yusuf.ramadhan@example.invalid", "PERABOT", True),
    ("PTG-011", "Rina Kartika", "rina.kartika@example.invalid", "JARINGAN", True),
    ("PTG-012", "Bayu Prasetyo", "bayu.prasetyo@example.invalid", "BANGUNAN", True),
]
P = {k: kontak for k, _, kontak, _, _ in petugas}

FAK = {"Teknik": ["B11", "B12", "B13"], "Ekonomi dan Bisnis": ["D10"], "Ilmu Pendidikan": ["D2"],
       "Sastra": ["E7"], "Matematika dan IPA": ["B20"], "Ilmu Sosial": ["I1"]}

mhs = [  # nama, kontak, nim, fakultas
    ("Nadia Putri", "nadia@example.invalid", "240535600201", "Teknik"),
    ("Rizky Pratama", "rizky.pratama@example.invalid", "240535600202", "Ekonomi dan Bisnis"),
    ("Alya Rahma", "alya.rahma@example.invalid", "240535600203", "Ilmu Pendidikan"),
    ("Dimas Saputra", "dimas.saputra@example.invalid", "240535600204", "Sastra"),
    ("Siti Aisyah", "siti.aisyah@example.invalid", "240535600205", "Teknik"),
    ("Fajar Maulana", "fajar.maulana@example.invalid", "240535600206", "Matematika dan IPA"),
    ("Intan Permata", "intan.permata@example.invalid", "240535600207", "Ilmu Sosial"),
]
lain = ["Andini Lestari", "Bagas Wicaksono", "Citra Maharani", "Dani Firmansyah", "Elsa Novianti", "Farhan Akbar",
        "Gita Paramitha", "Hafiz Ramadhan", "Indah Sari", "Joko Susilo", "Kirana Putri", "Laila Nurhaliza",
        "Mario Gunawan", "Nina Oktaviani", "Oki Saputra", "Putri Ayuningtyas", "Raihan Maulana", "Salsa Amelia",
        "Taufik Hidayat", "Utami Rahayu", "Vino Aditya", "Wulan Sari", "Yoga Pratama", "Zahra Aulia"]
for i, n in enumerate(lain):
    mhs.append((n, n.lower().replace(" ", ".") + "@example.invalid", f"2405356003{i:02d}", rnd.choice(list(FAK))))
M = {m[0]: m for m in mhs}

# ---------- Isi laporan ----------
POOL = [  # judul, kalimat tambahan, bidang
    ("AC ruang kuliah tidak dingin", "Ruangan terasa panas saat perkuliahan siang.", "KELISTRIKAN_AC"),
    ("Lampu ruang kelas mati", "Dua lampu di bagian belakang tidak menyala sejak pagi.", "KELISTRIKAN_AC"),
    ("Stopkontak dinding longgar", "Steker mudah lepas dan sempat memercikkan api kecil.", "KELISTRIKAN_AC"),
    ("Kipas angin tidak berputar", "Kipas menyala tetapi baling-baling tidak bergerak.", "KELISTRIKAN_AC"),
    ("Plafon ruang kelas retak", "Ada retakan memanjang di dekat lampu tengah.", "BANGUNAN"),
    ("Pintu kelas sulit ditutup", "Engsel bawah longgar sehingga pintu menyeret lantai.", "BANGUNAN"),
    ("Jendela kaca tidak bisa dikunci", "Pengunci jendela patah dan kaca bergeser saat angin.", "BANGUNAN"),
    ("Keramik lantai pecah", "Pecahan keramik di depan pintu bisa melukai kaki.", "BANGUNAN"),
    ("Keran toilet terus mengalir", "Air tidak berhenti walaupun keran sudah ditutup.", "SANITASI"),
    ("Wastafel tersumbat", "Air menggenang dan tidak turun sama sekali.", "SANITASI"),
    ("Flush toilet tidak berfungsi", "Tombol flush ditekan tetapi air tidak keluar.", "SANITASI"),
    ("Kursi kuliah patah", "Sandaran kursi baris depan lepas dari rangkanya.", "PERABOT"),
    ("Meja dosen goyang", "Salah satu kaki meja lebih pendek sehingga meja miring.", "PERABOT"),
    ("Papan tulis berlubang", "Permukaan papan rusak sehingga spidol tidak membekas.", "PERABOT"),
    ("Wi-Fi ruang kelas terputus", "Koneksi hilang setiap beberapa menit selama kuliah.", "JARINGAN"),
    ("Proyektor tidak menampilkan gambar", "Proyektor menyala tetapi layar tetap biru.", "JARINGAN"),
    ("Port HDMI meja dosen rusak", "Kabel HDMI tidak terbaca oleh proyektor.", "JARINGAN"),
]
BIDANG_PETUGAS = defaultdict(list)
for kode, _, _, b, _ in petugas:
    BIDANG_PETUGAS[b].append(kode)

AC = ("contoh/ac-ruang.png", "Foto ilustrasi: Antonio Mette, CC BY-SA 4.0, dipotong")
pin = {
    248: dict(p="Nadia Putri", f="Teknik", g="B11", l=2, r="204", j="AC ruang kuliah tidak dingin",
              d="AC di ruang kuliah 204 tidak terasa dingin meskipun sudah dinyalakan. Mohon diperiksa karena mengganggu kegiatan perkuliahan.",
              w=D(2026, 11, 20, 8, 42), s="M", foto=("ac-ruang-204.jpg", 1258291)),
    247: dict(p="Rizky Pratama", f="Ekonomi dan Bisnis", g="D10", l=1, r="105", j="Lampu ruang kuliah berkedip",
              d="Lampu ruang kuliah berkedip dan sering mati saat digunakan.", w=D(2026, 11, 20, 7, 40), s="T_DIT",
              ver=D(2026, 11, 20, 9, 5), tug=D(2026, 11, 20, 9, 15), pt="PTG-001",
              cp="Periksa lampu dan sambungan listrik."),
    246: dict(p="Alya Rahma", f="Ilmu Pendidikan", g="D2", l=3, r="301", j="AC ruang kelas tidak dingin",
              d="AC ruang 301 menyala, tetapi udara yang keluar tidak dingin. Kondisi ruangan terasa panas saat perkuliahan berlangsung.",
              w=D(2026, 11, 19, 8, 10), s="D", ver=D(2026, 11, 19, 9, 50), tug=D(2026, 11, 19, 10, 30), pt="PTG-001",
              mulai=D(2026, 11, 19, 13, 5), cat="Pemeriksaan awal: filter AC kotor. Sedang dilakukan pembersihan dan pengecekan pendingin.",
              foto=("ac-ruang-301.jpg", 1187840)),
    245: dict(p="Dimas Saputra", f="Sastra", g="E7", l=1, r="108", j="Kursi kuliah patah", w=D(2026, 11, 19, 7, 50), s="S"),
    244: dict(p="Siti Aisyah", f="Teknik", g="B11", l=1, r="102", j="Keran toilet terus mengalir", w=D(2026, 11, 18, 13, 40), s="S"),
    243: dict(p="Fajar Maulana", f="Matematika dan IPA", g="B20", l=2, r="207", j="Meja dosen goyang", w=D(2026, 11, 18, 8, 15), s="D", pt="PTG-004"),
    242: dict(p="Intan Permata", f="Ilmu Sosial", g="I1", l=1, r="110", j="Pintu kelas sulit ditutup", w=D(2026, 11, 17, 14, 10), s="S"),
    235: dict(p="Nadia Putri", f="Teknik", g="B11", l=1, r=None, j="Keran wastafel bocor",
              d="Keran wastafel di toilet lantai 1 bocor dan air menetes terus ke lantai.", w=D(2026, 11, 17, 8, 30), s="D", pt="PTG-007"),
    229: dict(p="Nadia Putri", f="Teknik", g="B12", l=1, r="103", j="Stopkontak tidak berfungsi",
              d="Stopkontak di dinding dekat meja depan tidak mengalirkan listrik ke laptop maupun pengisi daya.", w=D(2026, 11, 14, 10, 5), s="T_MP"),
    218: dict(p="Nadia Putri", f="Teknik", g="B12", l=2, r="201", j="Proyektor tidak menyala",
              d="Proyektor di ruang 201 tidak menyala walaupun kabel daya sudah terpasang.", w=D(2026, 11, 10, 9, 20), s="S"),
    214: dict(p="Nadia Putri", f="Teknik", g="B13", l=1, r=None, j="Pintu toilet tidak bisa dikunci",
              d="Kunci pintu toilet perempuan lantai 1 rusak sehingga pintu tidak bisa dikunci dari dalam.", w=D(2026, 11, 8, 11, 0), s="M"),
    207: dict(p="Nadia Putri", f="Teknik", g="B12", l=3, r="305", j="Wi-Fi ruang kelas terputus",
              d="Wi-Fi di ruang 305 terputus setiap beberapa menit sehingga presentasi daring terganggu.", w=D(2026, 11, 4, 9, 30), s="D", pt="PTG-006"),
    196: dict(p="Nadia Putri", f="Teknik", g="B11", l=2, r=None, j="Lampu koridor lantai 2 mati",
              d="Lampu koridor lantai 2 dekat tangga mati sehingga lorong gelap pada sore hari.", w=D(2026, 10, 27, 15, 20), s="S"),
    181: dict(p="Nadia Putri", f="Teknik", g="B12", l=1, r="104", j="Kursi kuliah patah",
              d="Dua kursi di baris belakang ruang 104 patah pada bagian sandaran.", w=D(2026, 10, 14, 10, 45), s="S"),
}

# ---------- Distribusi bulan ----------
BULAN = [  # (tahun, bulan, id awal, id akhir)
    (2026, 1, 1, 8), (2026, 2, 9, 16), (2026, 3, 17, 24), (2026, 4, 25, 32), (2026, 5, 33, 41),
    (2026, 6, 42, 63), (2026, 7, 64, 95), (2026, 8, 96, 122), (2026, 9, 123, 165), (2026, 10, 166, 200), (2026, 11, 201, 248)]
SELESAI_PER_BULAN = {(2026, 6): 16, (2026, 7): 24, (2026, 8): 21, (2026, 9): 33, (2026, 10): 29, (2026, 11): 39}
SEBELUM_JUNI = 186 - sum(SELESAI_PER_BULAN.values())  # 24
assert SEBELUM_JUNI == 24

import calendar
waktu = {}
for th, bl, a, b in BULAN:
    n = b - a + 1
    akhir = 20 if (th, bl) == (2026, 11) else calendar.monthrange(th, bl)[1]
    mulai, selesai = D(th, bl, 1, 7, 30), D(th, bl, akhir, 15, 30)
    for k, i in enumerate(range(a, b + 1)):
        waktu[i] = mulai + (selesai - mulai) * (k / max(1, n - 1))
# Jangkar November (layar) lalu interpolasi linier di antaranya.
for i, v in pin.items():
    waktu[i] = v["w"]
jangkar = sorted([201] + [i for i in pin if i >= 201])
waktu[201] = D(2026, 11, 1, 8, 0)
for a, b in zip(jangkar, jangkar[1:]):
    for i in range(a + 1, b):
        t = (i - a) / (b - a)
        waktu[i] = waktu[a] + (waktu[b] - waktu[a]) * t
# Oktober: id 181 dan 196 dipatok, sisanya disebar ulang di antaranya.
for a, b in [(166, 181), (181, 196), (196, 200)]:
    if a == 166:
        waktu[166] = D(2026, 10, 1, 8, 10)
    if b == 200:
        waktu[200] = D(2026, 10, 31, 14, 0)
    for i in range(a + 1, b):
        waktu[i] = waktu[a] + (waktu[b] - waktu[a]) * ((i - a) / (b - a))
for i in range(2, 249):
    assert waktu[i] > waktu[i - 1], (i, waktu[i - 1], waktu[i])
    waktu[i] = waktu[i].replace(second=0, microsecond=0)

# ---------- Status ----------
status = {i: v["s"] for i, v in pin.items()}
calon = [i for i in range(248, 0, -1) if i not in pin]
butuh = {"M": 24, "T_MP": 11, "T_DIT": 1, "D": 26}
for s in ("M", "T_MP", "D"):
    kurang = butuh[s] - sum(1 for v in status.values() if v == s)
    for _ in range(kurang):
        status[calon.pop(0)] = s
for i in calon:
    status[i] = "S"
c = Counter(status.values())
assert c == Counter({"S": 186, "D": 26, "M": 24, "T_MP": 11, "T_DIT": 1}), c

# ---------- Tanggal selesai ----------
selesai_ids = sorted(i for i in status if status[i] == "S")
slot = ["pre"] * SEBELUM_JUNI
for (th, bl), n in SELESAI_PER_BULAN.items():
    slot += [(th, bl)] * n
waktu_selesai = {}
for i, s in zip(selesai_ids, slot):
    lap = waktu[i]
    if s == "pre":
        cand = lap + dt.timedelta(days=2, hours=3)
        assert cand < D(2026, 6, 1), i
    else:
        th, bl = s
        awal = D(th, bl, 1, 9, 0)
        akhir = D(th, bl, 20, 16, 0) if (th, bl) == (2026, 11) else D(th, bl, calendar.monthrange(th, bl)[1], 16, 0)
        cand = max(lap + dt.timedelta(days=1, hours=rnd.randint(1, 30)), awal)
        if cand > akhir:
            cand = max(lap + dt.timedelta(hours=4), awal)
        assert awal <= cand <= akhir and cand > lap, (i, lap, s, cand)
    waktu_selesai[i] = cand.replace(second=0, microsecond=0)

# ---------- Petugas per tugas ----------
pelaksana = {}
for i, v in pin.items():
    if "pt" in v:
        pelaksana[i] = v["pt"]
d_bebas = [i for i in status if status[i] == "D" and i not in pelaksana]
for k, i in enumerate(sorted(d_bebas)):
    pelaksana[i] = "PTG-006" if k % 2 == 0 else "PTG-007"
nov_selesai = sorted(i for i in selesai_ids if waktu_selesai[i] >= D(2026, 11, 1))
for k, i in enumerate(nov_selesai):
    if k < 14:
        pelaksana[i] = "PTG-001"
lainnya = ["PTG-002", "PTG-003", "PTG-004", "PTG-005", "PTG-008", "PTG-009", "PTG-010", "PTG-011", "PTG-012", "PTG-006", "PTG-007"]
for k, i in enumerate(i for i in selesai_ids if i not in pelaksana):
    pelaksana[i] = lainnya[k % len(lainnya)]
for i in (i for i in status if status[i] == "T_DIT"):
    assert i in pelaksana

aktif = Counter(pelaksana[i] for i in status if status[i] in ("D", "T_DIT"))
assert aktif["PTG-001"] == 2 and aktif["PTG-004"] == 1 and len(aktif) == 4, aktif
assert sum(1 for i in nov_selesai if pelaksana[i] == "PTG-001") == 14

# ---------- Pelapor dan lokasi ----------
nama_lain = [m[0] for m in mhs if m[0] not in ("Nadia Putri",)]
data = {}
for i in range(1, 249):
    if i in pin:
        v = pin[i]
        judul = v["j"]
        des = v.get("d") or (judul + ". " + next(t for t in POOL if t[0] == judul)[1] if any(t[0] == judul for t in POOL) else judul + ".")
        data[i] = dict(p=v["p"], f=v["f"], g=v["g"], l=v["l"], r=v["r"], j=judul, d=des)
        continue
    pel = rnd.choice(nama_lain[6:])
    fak = M[pel][3]
    judul, tambah, _ = rnd.choice(POOL)
    g = rnd.choice(FAK[fak])
    l = rnd.randint(1, 4)
    r = f"{l}{rnd.randint(1, 12):02d}" if rnd.random() < 0.75 else None
    tempat = f"ruang {r}" if r else f"lantai {l}"
    data[i] = dict(p=pel, f=fak, g=g, l=l, r=r, j=judul, d=f"{judul} di {tempat} gedung {g}. {tambah}")

nadia = [i for i in data if data[i]["p"] == "Nadia Putri"]
assert sorted(nadia) == sorted([248, 235, 229, 218, 214, 207, 196, 181]), nadia
cn = Counter(status[i][0] for i in nadia)
assert cn == Counter({"M": 2, "T": 1, "D": 2, "S": 3}), cn

# Grafik (cek terhadap layar)
masuk = Counter((waktu[i].year, waktu[i].month) for i in range(1, 249))
tuntas = Counter((w.year, w.month) for w in waktu_selesai.values())
grafik = [(bl, masuk[(2026, bl)], tuntas[(2026, bl)]) for bl in range(6, 12)]
assert grafik == [(6, 22, 16), (7, 32, 24), (8, 27, 21), (9, 43, 33), (10, 35, 29), (11, 48, 39)], grafik

# ---------- SQL ----------
def q(s):
    return "NULL" if s is None else "'" + str(s).replace("\\", "\\\\").replace("'", "''") + "'"

def t(w):
    return "NULL" if w is None else "'" + w.strftime("%Y-%m-%d %H:%M:%S") + "'"

def uid(kontak):
    return f"(SELECT id FROM (SELECT id, kontak FROM pengguna) p WHERE p.kontak = {q(kontak)})"

L = []
w = L.append
w("-- Data demo yang sama dengan 13 layar acuan (Untitled.zip). Dibuat oleh skrip generator; jangan disunting tangan.")
w("-- Hanya dimuat aplikasi (spring.flyway.locations menyertakan classpath:db/demo), tidak oleh tes.")
w("-- Ringkasan: 248 laporan = 24 menunggu verifikasi, 12 terverifikasi, 26 sedang diperbaiki, 186 selesai.")
w("")
w("DELETE FROM tugas_perbaikan;")
w("DELETE FROM laporan_kerusakan;")
w("DELETE FROM kasus_kerusakan;")
w("DELETE FROM pengguna WHERE peran = 'MAHASISWA' AND kontak IN ('andi@example.invalid', 'budi@example.invalid', 'sinta@example.invalid');")
w("")
w("-- Petugas: lima di daftar layar + tujuh lainnya (total 12).")
for kode, nama, kontak, bidang, baru in petugas:
    if baru:
        w(f"INSERT INTO pengguna (peran, nama, kontak, keahlian, jam_mulai, jam_selesai, sibuk, kode) VALUES ('PETUGAS', {q(nama)}, {q(kontak)}, {q(bidang)}, '00:00:00', '00:00:00', FALSE, {q(kode)});")
    else:
        w(f"UPDATE pengguna SET nama = {q(nama)}, keahlian = {q(bidang)}, jam_mulai = '00:00:00', jam_selesai = '00:00:00', sibuk = FALSE, kode = {q(kode)} WHERE kontak = {q(kontak)};")
w("")
w("-- Mahasiswa pelapor.")
for nama, kontak, nim, fak in mhs:
    w(f"INSERT INTO pengguna (peran, nama, kontak, nim, fakultas, sibuk) VALUES ('MAHASISWA', {q(nama)}, {q(kontak)}, {q(nim)}, {q(fak)}, FALSE);")
w("")
w("SET @admin = (SELECT id FROM pengguna WHERE kontak = 'admin@example.invalid');")
w("")
STATUS_KASUS = {"M": "MENUNGGU_VERIFIKASI", "T_MP": "MENUNGGU_PENUGASAN", "T_DIT": "DITUGASKAN", "D": "DIKERJAKAN", "S": "SELESAI"}
for i in range(1, 249):
    s = status[i]
    v = data[i]
    lap = waktu[i]
    pv = pin.get(i, {})
    ver = None if s == "M" else pv.get("ver", lap + dt.timedelta(hours=1, minutes=(i * 7) % 50))
    if s == "S":
        ver = min(ver, waktu_selesai[i] - dt.timedelta(hours=3))
        ver = max(ver, lap + dt.timedelta(minutes=20))
    w(f"INSERT INTO kasus_kerusakan (id, status, pengelompokan_otomatis, terverifikasi, bahaya_kritis, verifikator_id, waktu_verifikasi) VALUES "
      f"({i}, '{STATUS_KASUS[s]}', FALSE, {'FALSE' if s == 'M' else 'TRUE'}, FALSE, {'NULL' if s == 'M' else '@admin'}, {t(ver)});")
    foto = pv.get("foto")
    fs, fn, fu, kr = ("NULL",) * 4
    if foto:
        fs, fn, fu, kr = q(AC[0]), q(foto[0]), str(foto[1]), q(AC[1])
    w(f"INSERT INTO laporan_kerusakan (id, pelapor_id, kasus_id, judul, fakultas, gedung, lantai, ruang, deskripsi, waktu_laporan, foto_bukti, foto_bukti_nama, foto_bukti_ukuran, foto_kredit) VALUES "
      f"({i}, {uid(M[v['p']][1])}, {i}, {q(v['j'])}, {q(v['f'])}, {q(v['g'])}, {v['l']}, {q(v['r'])}, {q(v['d'])}, {t(lap)}, {fs}, {fn}, {fu}, {kr});")
    if s in ("T_DIT", "D", "S"):
        tug = pv.get("tug", ver + dt.timedelta(minutes=25))
        mulai = None
        selesai = None
        cat = None
        st = "DITUGASKAN"
        if s == "D":
            st = "DIKERJAKAN"
            mulai = pv.get("mulai", tug + dt.timedelta(hours=2))
            cat = pv.get("cat", "Pemeriksaan awal selesai. Menunggu suku cadang pengganti.")
        if s == "S":
            st = "SELESAI"
            selesai = waktu_selesai[i]
            tug = min(tug, selesai - dt.timedelta(hours=2))
            mulai = tug + (selesai - tug) / 2
            cat = "Perbaikan selesai. Fasilitas sudah diuji dan berfungsi normal."
        w(f"INSERT INTO tugas_perbaikan (kasus_id, petugas_id, penugas_id, status, waktu_penugasan, waktu_mulai, waktu_selesai, catatan_penugasan, catatan) VALUES "
          f"({i}, {uid(P[pelaksana[i]])}, @admin, '{st}', {t(tug)}, {t(mulai)}, {t(selesai)}, {q(pv.get('cp'))}, {q(cat)});")
w("")
w("-- Ketersediaan petugas mengikuti tugas aktif.")
w("UPDATE pengguna SET sibuk = TRUE WHERE peran = 'PETUGAS' AND kode IN (" + ", ".join(q(k) for k in sorted(aktif)) + ");")
w("ALTER TABLE laporan_kerusakan AUTO_INCREMENT = 249;")
w("ALTER TABLE kasus_kerusakan AUTO_INCREMENT = 249;")
open(OUT, "w", encoding="utf-8", newline="\n").write("\n".join(L) + "\n")
print("ok", len(L), "baris; grafik", grafik, "; aktif", dict(aktif))
