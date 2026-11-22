-- Login berbasis email atau NIM (mode demo akademik).
-- Kolom kontak sudah UNIQUE dan berisi email; nilainya dinormalisasi ke huruf kecil
-- agar pencocokan email konsisten di MySQL maupun H2.
UPDATE pengguna SET kontak = LOWER(TRIM(kontak));

-- NIM hanya dimiliki mahasiswa. Format demo: 12 digit angka.
ALTER TABLE pengguna ADD COLUMN nim VARCHAR(12) NULL;
CREATE UNIQUE INDEX ux_pengguna_nim ON pengguna (nim);

UPDATE pengguna SET nim = '240535600101' WHERE peran = 'MAHASISWA' AND kontak = 'andi@example.invalid';
UPDATE pengguna SET nim = '240535600102' WHERE peran = 'MAHASISWA' AND kontak = 'budi@example.invalid';
UPDATE pengguna SET nim = '240535600103' WHERE peran = 'MAHASISWA' AND kontak = 'sinta@example.invalid';
