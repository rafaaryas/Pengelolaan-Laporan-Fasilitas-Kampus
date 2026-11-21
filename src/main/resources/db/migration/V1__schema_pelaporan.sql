CREATE TABLE pengguna (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    peran VARCHAR(31) NOT NULL,
    nama VARCHAR(100) NOT NULL,
    kontak VARCHAR(150) NOT NULL UNIQUE,
    keahlian VARCHAR(24),
    jam_mulai TIME,
    jam_selesai TIME,
    sibuk BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE fasilitas (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nama VARCHAR(100) NOT NULL,
    jenis VARCHAR(24) NOT NULL,
    gedung VARCHAR(40) NOT NULL,
    ruang VARCHAR(80) NOT NULL,
    jumlah_unit INTEGER NOT NULL,
    kondisi VARCHAR(24) NOT NULL,
    CHECK (jumlah_unit > 0)
);

CREATE TABLE kasus_kerusakan (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    fasilitas_id BIGINT NOT NULL,
    jenis_kerusakan VARCHAR(24) NOT NULL,
    status VARCHAR(32) NOT NULL,
    pengelompokan_otomatis BOOLEAN NOT NULL,
    terverifikasi BOOLEAN NOT NULL,
    bahaya_kritis BOOLEAN NOT NULL,
    gangguan VARCHAR(16),
    unit_rusak INTEGER,
    prioritas VARCHAR(16),
    alasan_prioritas VARCHAR(500),
    pemeriksa_id BIGINT,
    waktu_verifikasi DATETIME(6),
    catatan_pemeriksaan VARCHAR(1000),
    FOREIGN KEY (fasilitas_id) REFERENCES fasilitas (id),
    FOREIGN KEY (pemeriksa_id) REFERENCES pengguna (id)
);

CREATE INDEX ix_kasus_pengelompokan ON kasus_kerusakan (fasilitas_id, jenis_kerusakan, pengelompokan_otomatis, status);

CREATE TABLE laporan_kerusakan (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    pelapor_id BIGINT NOT NULL,
    fasilitas_id BIGINT NOT NULL,
    kasus_id BIGINT NOT NULL,
    jenis_kerusakan VARCHAR(24) NOT NULL,
    deskripsi VARCHAR(1600) NOT NULL,
    waktu_laporan DATETIME(6) NOT NULL,
    FOREIGN KEY (pelapor_id) REFERENCES pengguna (id),
    FOREIGN KEY (fasilitas_id) REFERENCES fasilitas (id),
    FOREIGN KEY (kasus_id) REFERENCES kasus_kerusakan (id)
);

CREATE INDEX ix_laporan_pelapor ON laporan_kerusakan (pelapor_id, waktu_laporan);

CREATE TABLE tugas_perbaikan (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    kasus_id BIGINT NOT NULL UNIQUE,
    petugas_id BIGINT NOT NULL,
    penugas_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    waktu_penugasan DATETIME(6) NOT NULL,
    waktu_mulai DATETIME(6),
    waktu_selesai DATETIME(6),
    catatan_penugasan VARCHAR(1000),
    catatan VARCHAR(1000),
    FOREIGN KEY (kasus_id) REFERENCES kasus_kerusakan (id),
    FOREIGN KEY (petugas_id) REFERENCES pengguna (id),
    FOREIGN KEY (penugas_id) REFERENCES pengguna (id)
);

CREATE INDEX ix_tugas_petugas ON tugas_perbaikan (petugas_id, status);
