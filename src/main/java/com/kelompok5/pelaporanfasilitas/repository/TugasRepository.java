package com.kelompok5.pelaporanfasilitas.repository;

import java.util.List;
import java.util.Optional;
import com.kelompok5.pelaporanfasilitas.model.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TugasRepository extends JpaRepository<TugasPerbaikan, Long> {
    @EntityGraph(attributePaths = {"kasus", "kasus.fasilitas"})
    List<TugasPerbaikan> findByPetugasIdOrderByWaktuPenugasanDescIdDesc(long petugasId);

    @EntityGraph(attributePaths = {"kasus", "kasus.fasilitas", "kasus.daftarLaporan", "kasus.daftarLaporan.pelapor", "petugas", "penugas"})
    Optional<TugasPerbaikan> findByIdAndPetugasId(long id, long petugasId);

    @EntityGraph(attributePaths = {"petugas", "penugas"})
    Optional<TugasPerbaikan> findByKasusId(long kasusId);

    boolean existsByPetugasIdAndStatusNot(long petugasId, StatusTugas status);

    @Query("select t.kasus.fasilitas.id from TugasPerbaikan t where t.id = :id and t.petugas.id = :petugasId")
    Optional<Long> cariIdFasilitasTugas(long id, long petugasId);
}
