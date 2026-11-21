package com.kelompok5.pelaporanfasilitas.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import com.kelompok5.pelaporanfasilitas.model.Fasilitas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface FasilitasRepository extends JpaRepository<Fasilitas, Long> {
    List<Fasilitas> findAllByOrderByGedungAscNamaAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Fasilitas f where f.id = :id")
    Optional<Fasilitas> kunci(long id);
}
