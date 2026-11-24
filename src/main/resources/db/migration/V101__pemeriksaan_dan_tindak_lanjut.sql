ALTER TABLE kasus_kerusakan ADD COLUMN waktu_pemeriksaan DATETIME NULL;

ALTER TABLE tugas_perbaikan MODIFY COLUMN status VARCHAR(24) NOT NULL;
ALTER TABLE tugas_perbaikan ADD COLUMN alasan_tindak_lanjut VARCHAR(1000) NULL;
ALTER TABLE tugas_perbaikan ADD COLUMN kebutuhan_berikutnya VARCHAR(1000) NULL;
ALTER TABLE tugas_perbaikan ADD COLUMN waktu_tindak_lanjut DATETIME NULL;
ALTER TABLE tugas_perbaikan ADD COLUMN keputusan_admin VARCHAR(1000) NULL;
ALTER TABLE tugas_perbaikan ADD COLUMN waktu_keputusan DATETIME NULL;
ALTER TABLE tugas_perbaikan ADD COLUMN nama_petugas_sebelumnya VARCHAR(120) NULL;
