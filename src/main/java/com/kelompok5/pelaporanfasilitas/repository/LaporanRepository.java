package com.kelompok5.pelaporanfasilitas.repository;

import java.util.List;
import java.util.Optional;
import com.kelompok5.pelaporanfasilitas.model.LaporanKerusakan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaporanRepository extends JpaRepository<LaporanKerusakan, Long> {
    @EntityGraph(attributePaths = {"fasilitas", "kasus", "pelapor"})
    List<LaporanKerusakan> findByPelaporIdOrderByWaktuLaporanDescIdDesc(long mahasiswaId);

    @EntityGraph(attributePaths = {"fasilitas", "kasus", "pelapor"})
    Optional<LaporanKerusakan> findByIdAndPelaporId(long id, long mahasiswaId);
}
