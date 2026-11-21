INSERT INTO pengguna (peran, nama, kontak) VALUES
    ('MAHASISWA', 'Andi', 'andi@example.invalid'),
    ('MAHASISWA', 'Budi', 'budi@example.invalid'),
    ('MAHASISWA', 'Sinta', 'sinta@example.invalid'),
    ('ADMIN', 'Administrator UM', 'admin@example.invalid');

INSERT INTO pengguna (peran, nama, kontak, keahlian, jam_mulai, jam_selesai) VALUES
    ('PETUGAS', 'Dimas Pratama', 'dimas@example.invalid', 'PENDINGIN', '00:00:00', '00:00:00'),
    ('PETUGAS', 'Raka Santoso', 'raka@example.invalid', 'LISTRIK', '00:00:00', '00:00:00'),
    ('PETUGAS', 'Rudi Hartono', 'rudi@example.invalid', 'PERABOT', '00:00:00', '00:00:00'),
    ('PETUGAS', 'Siti Rahmawati', 'siti@example.invalid', 'SANITASI', '00:00:00', '00:00:00'),
    ('PETUGAS', 'Agus Setiawan', 'agus@example.invalid', 'PENDINGIN', '08:00:00', '16:00:00');

INSERT INTO fasilitas (nama, jenis, gedung, ruang, jumlah_unit, kondisi) VALUES
    ('AC B13', 'PENDINGIN', 'Gedung B13', 'Ruang 204', 4, 'BAIK'),
    ('Kursi kuliah', 'PERABOT', 'Gedung B13', 'Ruang 204', 36, 'BAIK'),
    ('Lampu ruang kelas', 'LISTRIK', 'Gedung D10', 'Ruang 105', 8, 'BAIK'),
    ('Instalasi listrik', 'LISTRIK', 'Gedung B13', 'Lantai 2', 6, 'BAIK'),
    ('Keran wastafel', 'SANITASI', 'Gedung B12', 'Toilet lantai 1', 3, 'BAIK');
