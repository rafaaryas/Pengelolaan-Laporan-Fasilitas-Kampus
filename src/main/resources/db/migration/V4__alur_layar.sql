-- Alur sesuai layar acuan UM Peduli:
-- laporan berisi lokasi (fakultas, gedung, lantai, ruang) dan foto bukti, admin memverifikasi,
-- petugas melampirkan foto hasil saat menyelesaikan tugas.

-- Bidang petugas mengikuti layar (lima bidang).
UPDATE pengguna SET keahlian = 'KELISTRIKAN_AC' WHERE keahlian IN ('PENDINGIN', 'LISTRIK');
UPDATE fasilitas SET jenis = 'KELISTRIKAN_AC' WHERE jenis IN ('PENDINGIN', 'LISTRIK');

-- Mahasiswa mencatat fakultas, petugas punya kode PTG-xxx.
ALTER TABLE pengguna ADD COLUMN fakultas VARCHAR(60) NULL;
ALTER TABLE pengguna ADD COLUMN kode VARCHAR(12) NULL;

-- Laporan berbasis lokasi: fasilitas dan jenis kerusakan menjadi opsional.
ALTER TABLE laporan_kerusakan MODIFY COLUMN fasilitas_id BIGINT NULL;
ALTER TABLE laporan_kerusakan MODIFY COLUMN jenis_kerusakan VARCHAR(24) NULL;
ALTER TABLE laporan_kerusakan ADD COLUMN judul VARCHAR(120) NULL;
ALTER TABLE laporan_kerusakan ADD COLUMN fakultas VARCHAR(60) NULL;
ALTER TABLE laporan_kerusakan ADD COLUMN gedung VARCHAR(40) NULL;
ALTER TABLE laporan_kerusakan ADD COLUMN lantai INTEGER NULL;
ALTER TABLE laporan_kerusakan ADD COLUMN ruang VARCHAR(40) NULL;
ALTER TABLE laporan_kerusakan ADD COLUMN foto_bukti VARCHAR(120) NULL;
ALTER TABLE laporan_kerusakan ADD COLUMN foto_bukti_nama VARCHAR(160) NULL;
ALTER TABLE laporan_kerusakan ADD COLUMN foto_bukti_ukuran BIGINT NULL;
ALTER TABLE laporan_kerusakan ADD COLUMN foto_kredit VARCHAR(200) NULL;

ALTER TABLE kasus_kerusakan MODIFY COLUMN fasilitas_id BIGINT NULL;
ALTER TABLE kasus_kerusakan MODIFY COLUMN jenis_kerusakan VARCHAR(24) NULL;
ALTER TABLE kasus_kerusakan ADD COLUMN verifikator_id BIGINT NULL;
ALTER TABLE kasus_kerusakan ADD CONSTRAINT fk_kasus_verifikator FOREIGN KEY (verifikator_id) REFERENCES pengguna (id);

ALTER TABLE tugas_perbaikan ADD COLUMN foto_hasil VARCHAR(120) NULL;
ALTER TABLE tugas_perbaikan ADD COLUMN foto_hasil_nama VARCHAR(160) NULL;
ALTER TABLE tugas_perbaikan ADD COLUMN foto_hasil_ukuran BIGINT NULL;

CREATE INDEX ix_laporan_waktu ON laporan_kerusakan (waktu_laporan);
