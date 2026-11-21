package com.kelompok5.pelaporanfasilitas.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import com.kelompok5.pelaporanfasilitas.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface PenggunaRepository extends JpaRepository<Pengguna, Long> {
    @Query("select m from Mahasiswa m order by m.id")
    List<Mahasiswa> mahasiswaDemo();

    @Query("select p from Petugas p order by p.nama, p.id")
    List<Petugas> petugasDemo();

    @Query("select a from Admin a order by a.id")
    List<Admin> adminDemo();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Petugas p where p.id = :id")
    Optional<Petugas> kunciPetugas(long id);
}
