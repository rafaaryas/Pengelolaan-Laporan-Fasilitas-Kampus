-- Jalankan pada skema demo setelah Flyway sampai V101.
-- Setiap laporan ditambahkan sekali. Status dan laporan yang sudah ada dipertahankan.
START TRANSACTION;
INSERT INTO pengguna (peran, nama, kontak, nim, fakultas)
SELECT 'MAHASISWA', d.nama, d.email, d.nim, d.fakultas
FROM (
    SELECT 'Nadia Putri' nama, 'nadia@example.invalid' email, '240535600201' nim, 'Teknik' fakultas
    UNION ALL SELECT 'Rizky Pratama', 'rizky.pratama@example.invalid', '240535600202', 'Ekonomi dan Bisnis'
    UNION ALL SELECT 'Alya Rahma', 'alya.rahma@example.invalid', '240535600203', 'Ilmu Pendidikan'
) d
WHERE NOT EXISTS (SELECT 1 FROM pengguna u WHERE u.kontak = d.email OR u.nim = d.nim);

INSERT INTO pengguna (peran, nama, kontak, keahlian, jam_mulai, jam_selesai, kode)
SELECT 'PETUGAS', 'Budi Santoso', 'budi.santoso@example.invalid', 'KELISTRIKAN_AC', '00:00:00', '00:00:00', 'PTG-001'
WHERE NOT EXISTS (SELECT 1 FROM pengguna WHERE kontak = 'budi.santoso@example.invalid');
COMMIT;

DELIMITER //
CREATE PROCEDURE isi_laporan_demo_um(
    IN email VARCHAR(150), IN judul_demo VARCHAR(120), IN uraian VARCHAR(400),
    IN gedung_demo VARCHAR(40), IN lantai_demo INT, IN ruang_demo VARCHAR(40),
    IN keadaan VARCHAR(32), IN tingkat VARCHAR(16), IN pemeriksa_email VARCHAR(150),
    IN petugas_email VARCHAR(150), IN lama_jam INT, IN foto_ac BOOLEAN
)
BEGIN
    DECLARE pelapor BIGINT;
    DECLARE penugas BIGINT;
    DECLARE pemeriksa BIGINT;
    DECLARE petugas BIGINT;
    DECLARE kasus_baru BIGINT;
    DECLARE dilaporkan DATETIME;
    DECLARE sudah_verifikasi BOOLEAN;
    DECLARE sudah_mulai BOOLEAN;
    DECLARE selesai BOOLEAN;
    DECLARE perlu_tindak BOOLEAN;
    DECLARE dialihkan BOOLEAN;

    SET pelapor = (SELECT id FROM pengguna WHERE kontak = email AND peran = 'MAHASISWA');
    SET penugas = (SELECT id FROM pengguna WHERE kontak = 'admin@example.invalid' AND peran = 'ADMIN');
    SET pemeriksa = (SELECT id FROM pengguna WHERE kontak = pemeriksa_email AND peran = 'PETUGAS');
    SET petugas = (SELECT id FROM pengguna WHERE kontak = petugas_email AND peran = 'PETUGAS');
    SET dilaporkan = DATE_SUB(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 7 HOUR), INTERVAL lama_jam HOUR);
    SET sudah_verifikasi = keadaan <> 'MENUNGGU_VERIFIKASI';
    SET sudah_mulai = keadaan IN ('DIKERJAKAN', 'SELESAI');
    SET selesai = keadaan = 'SELESAI';
    SET perlu_tindak = keadaan = 'PERLU_TINDAK_LANJUT';
    SET dialihkan = pemeriksa IS NOT NULL AND petugas IS NOT NULL AND pemeriksa <> petugas;

    IF pelapor IS NOT NULL AND penugas IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM laporan_kerusakan l WHERE l.pelapor_id = pelapor AND l.judul = judul_demo
    ) THEN
        INSERT INTO kasus_kerusakan (
            status, pengelompokan_otomatis, terverifikasi, bahaya_kritis, gangguan, unit_rusak,
            prioritas, alasan_prioritas, pemeriksa_id, verifikator_id, waktu_verifikasi,
            waktu_pemeriksaan, catatan_pemeriksaan
        ) VALUES (
            keadaan, FALSE, sudah_verifikasi, COALESCE(tingkat = 'KRITIS', FALSE),
            CASE tingkat WHEN 'TINGGI' THEN 'BERAT' WHEN 'SEDANG' THEN 'SEBAGIAN'
                         WHEN 'RENDAH' THEN 'RINGAN' WHEN 'KRITIS' THEN 'RINGAN' END,
            IF(tingkat IS NULL, NULL, 1), tingkat,
            CASE tingkat WHEN 'KRITIS' THEN 'Petugas mengonfirmasi bahaya keselamatan kritis.'
                         WHEN 'TINGGI' THEN 'Fasilitas tidak dapat digunakan.'
                         WHEN 'SEDANG' THEN 'Sebagian fungsi fasilitas terganggu.'
                         WHEN 'RENDAH' THEN 'Gangguan penggunaan terbatas.' END,
            pemeriksa, IF(sudah_verifikasi, penugas, NULL),
            IF(sudah_verifikasi, DATE_ADD(dilaporkan, INTERVAL 10 MINUTE), NULL),
            IF(pemeriksa IS NOT NULL, DATE_ADD(dilaporkan, INTERVAL 30 MINUTE), NULL),
            IF(pemeriksa IS NOT NULL, uraian, NULL)
        );
        SET kasus_baru = LAST_INSERT_ID();
        INSERT INTO laporan_kerusakan (pelapor_id, kasus_id, judul, deskripsi, fakultas, gedung, lantai, ruang, waktu_laporan)
        SELECT id, kasus_baru, judul_demo, uraian, fakultas, gedung_demo, lantai_demo, ruang_demo, dilaporkan
        FROM pengguna WHERE id = pelapor;

        IF petugas IS NOT NULL THEN
            INSERT INTO tugas_perbaikan (
                kasus_id, petugas_id, penugas_id, status, waktu_penugasan, waktu_mulai, waktu_selesai,
                catatan_penugasan, catatan, waktu_tindak_lanjut, alasan_tindak_lanjut, kebutuhan_berikutnya,
                waktu_keputusan, keputusan_admin, nama_petugas_sebelumnya, foto_hasil, foto_hasil_nama, foto_hasil_ukuran
            ) VALUES (
                kasus_baru, petugas, penugas, keadaan,
                DATE_ADD(dilaporkan, INTERVAL IF(dialihkan, 50, 20) MINUTE),
                IF(sudah_mulai, DATE_ADD(dilaporkan, INTERVAL 35 MINUTE), NULL),
                IF(selesai, DATE_ADD(dilaporkan, INTERVAL 65 MINUTE), NULL),
                'Periksa kondisi di lokasi.',
                CASE WHEN selesai THEN 'Unit sudah diservis dan diuji kembali. Foto ilustrasi tersedia.'
                     WHEN sudah_mulai THEN 'Penanganan sedang berlangsung.' END,
                IF(perlu_tindak OR dialihkan, DATE_ADD(dilaporkan, INTERVAL 40 MINUTE), NULL),
                CASE WHEN dialihkan THEN 'Perlu teknisi tambahan untuk melanjutkan pekerjaan.'
                     WHEN perlu_tindak AND tingkat = 'KRITIS' THEN 'Area sudah dibatasi dan perlu penanganan tim listrik.'
                     WHEN perlu_tindak THEN 'Komponen pengganti belum tersedia.' END,
                CASE WHEN perlu_tindak AND tingkat = 'KRITIS' THEN 'Tambahan teknisi dan alat pengaman.'
                     WHEN perlu_tindak OR dialihkan THEN 'Suku cadang dan arahan admin.' END,
                IF(dialihkan, DATE_ADD(dilaporkan, INTERVAL 50 MINUTE), NULL),
                IF(dialihkan, 'Tugas dialihkan ke petugas pengganti untuk melanjutkan perbaikan.', NULL),
                IF(dialihkan, (SELECT nama FROM pengguna WHERE id = pemeriksa), NULL),
                IF(foto_ac, 'contoh/ac-ruang.png', NULL),
                IF(foto_ac, 'foto-ilustrasi-perbaikan-ac.png', NULL),
                IF(foto_ac, 121417, NULL)
            );
        END IF;
    END IF;
END//
DELIMITER ;

START TRANSACTION;
CALL isi_laporan_demo_um('nadia@example.invalid', '[Demo] Lampu koridor mati', 'Lampu padam sejak pagi dan koridor menjadi gelap.', 'B11', 2, 'Koridor', 'MENUNGGU_VERIFIKASI', NULL, NULL, NULL, 2, FALSE);
CALL isi_laporan_demo_um('nadia@example.invalid', '[Demo] AC kelas belum diperiksa', 'AC menyala tetapi udara tetap hangat saat kelas penuh.', 'B11', 2, '204', 'DITUGASKAN', NULL, NULL, 'budi.santoso@example.invalid', 7, FALSE);
CALL isi_laporan_demo_um('nadia@example.invalid', '[Demo] Kabel terbuka sedang ditangani', 'Teknisi menemukan kabel terbuka. Area sudah dibatasi dan penanganan langsung berjalan.', 'B11', 2, 'Gudang', 'DIKERJAKAN', 'KRITIS', 'budi.santoso@example.invalid', 'budi.santoso@example.invalid', 13, FALSE);
CALL isi_laporan_demo_um('nadia@example.invalid', '[Demo] Kursi longgar perlu suku cadang', 'Satu kursi masih longgar. Baut pengganti belum tersedia.', 'B11', 2, '203', 'PERLU_TINDAK_LANJUT', 'RENDAH', 'rudi@example.invalid', 'rudi@example.invalid', 23, FALSE);
CALL isi_laporan_demo_um('nadia@example.invalid', '[Demo] AC kelas sudah diperbaiki', 'Aliran udara AC sebelumnya lemah saat perkuliahan.', 'B11', 2, '205', 'SELESAI', 'SEDANG', 'budi.santoso@example.invalid', 'budi.santoso@example.invalid', 29, TRUE);
CALL isi_laporan_demo_um('rizky.pratama@example.invalid', '[Demo] Keran bocor menunggu penugasan', 'Keran terus menetes meskipun sudah ditutup.', 'D10', 1, 'Toilet', 'MENUNGGU_PENUGASAN', NULL, NULL, NULL, 4, FALSE);
CALL isi_laporan_demo_um('rizky.pratama@example.invalid', '[Demo] Panel listrik perlu tindak lanjut', 'Petugas menemukan bagian panel listrik terbuka dan meminta dukungan teknisi.', 'D10', 1, '105', 'PERLU_TINDAK_LANJUT', 'KRITIS', 'budi.santoso@example.invalid', 'budi.santoso@example.invalid', 9, FALSE);
CALL isi_laporan_demo_um('rizky.pratama@example.invalid', '[Demo] Pendingin dialihkan ke Dimas', 'Unit pendingin berhenti total dan ruang perkuliahan tidak dapat digunakan.', 'D10', 2, '208', 'DIKERJAKAN', 'TINGGI', 'budi.santoso@example.invalid', 'dimas@example.invalid', 17, FALSE);
CALL isi_laporan_demo_um('alya.rahma@example.invalid', '[Demo] Kursi retak menunggu verifikasi', 'Sandaran kursi retak di dekat sambungan.', 'D2', 3, '301', 'MENUNGGU_VERIFIKASI', NULL, NULL, NULL, 37, FALSE);
CALL isi_laporan_demo_um('alya.rahma@example.invalid', '[Demo] AC ruang 302 sudah berfungsi', 'AC ruang 302 tidak mendinginkan ruangan dengan merata.', 'D2', 3, '302', 'SELESAI', 'SEDANG', 'dimas@example.invalid', 'dimas@example.invalid', 76, TRUE);
UPDATE pengguna p SET p.sibuk = EXISTS (
    SELECT 1 FROM tugas_perbaikan t WHERE t.petugas_id = p.id AND t.status <> 'SELESAI'
) WHERE p.peran = 'PETUGAS' AND p.kontak IN ('budi.santoso@example.invalid', 'dimas@example.invalid', 'rudi@example.invalid');
COMMIT;
DROP PROCEDURE isi_laporan_demo_um;
