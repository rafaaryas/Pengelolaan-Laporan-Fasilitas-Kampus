package com.kelompok5.pelaporanfasilitas.repository;

import java.util.List;
import java.util.Optional;
import com.kelompok5.pelaporanfasilitas.model.LaporanKerusakan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaporanRepository extends JpaRepository<LaporanKerusakan, Long> {
    @EntityGraph(attributePaths = {"fasilitas", "kasus", "pelapor"})
    List<LaporanKerusakan> findByPelaporIdOrderByWaktuLaporanDescIdDesc(long mahasiswaId);

    @EntityGraph(attributePaths = {"fasilitas", "kasus", "pelapor"})
    Optional<LaporanKerusakan> findByIdAndPelaporId(long id, long mahasiswaId);

    @EntityGraph(attributePaths = {"fasilitas", "kasus", "pelapor"})
    @Query("select l from LaporanKerusakan l order by l.waktuLaporan desc, l.id desc")
    List<LaporanKerusakan> semua();

    @EntityGraph(attributePaths = {"kasus", "pelapor"})
    @Query("select l from LaporanKerusakan l where l.id = :id")
    Optional<LaporanKerusakan> denganKasus(long id);
}
