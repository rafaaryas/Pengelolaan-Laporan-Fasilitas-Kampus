CREATE TABLE berkas_foto (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nama_asli VARCHAR(160) NOT NULL,
    mime_type VARCHAR(64) NOT NULL,
    ukuran BIGINT NOT NULL,
    data LONGBLOB NOT NULL,
    waktu_unggah DATETIME(6) NOT NULL
);

ALTER TABLE laporan_kerusakan ADD COLUMN foto_bukti_id BIGINT NULL;
ALTER TABLE laporan_kerusakan ADD CONSTRAINT fk_laporan_foto_bukti FOREIGN KEY (foto_bukti_id) REFERENCES berkas_foto (id);

ALTER TABLE tugas_perbaikan ADD COLUMN foto_hasil_id BIGINT NULL;
ALTER TABLE tugas_perbaikan ADD CONSTRAINT fk_tugas_foto_hasil FOREIGN KEY (foto_hasil_id) REFERENCES berkas_foto (id);
