package com.kelompok5.pelaporanfasilitas.repository;

import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import com.kelompok5.pelaporanfasilitas.model.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface KasusRepository extends JpaRepository<KasusKerusakan, Long> {
    Optional<KasusKerusakan> findFirstByFasilitasIdAndJenisKerusakanAndPengelompokanOtomatisTrueAndStatusNotOrderById(
            long fasilitasId, JenisKerusakan jenis, StatusKasus status);

    @Query("select f.id from KasusKerusakan k join k.fasilitas f where k.id = :id")
    Optional<Long> cariIdFasilitas(long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select k from KasusKerusakan k where k.id = :id")
    Optional<KasusKerusakan> kunci(long id);

    @EntityGraph(attributePaths = {"fasilitas", "daftarLaporan", "daftarLaporan.pelapor", "daftarLaporan.fotoBukti", "pemeriksa"})
    @Query("select k from KasusKerusakan k where k.id = :id")
    Optional<KasusKerusakan> detail(long id);

    @EntityGraph(attributePaths = {"fasilitas", "daftarLaporan"})
    @Query("select k from KasusKerusakan k")
    List<KasusKerusakan> ringkasan();

    List<KasusKerusakan> findByFasilitasIdAndStatusNot(long fasilitasId, StatusKasus status);
}
