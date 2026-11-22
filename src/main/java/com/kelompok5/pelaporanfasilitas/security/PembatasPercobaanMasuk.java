package com.kelompok5.pelaporanfasilitas.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Pembatasan ringan percobaan masuk per alamat IP dalam jendela waktu bergeser. */
@Component
public class PembatasPercobaanMasuk {
    private final int batas;
    private final Duration jendela;
    private final Clock jam;
    private final Map<String, Deque<Instant>> catatan = new ConcurrentHashMap<>();

    public PembatasPercobaanMasuk(@Value("${umpeduli.masuk.batas-percobaan:20}") int batas,
                                  @Value("${umpeduli.masuk.jendela-menit:5}") long menit, Clock jam) {
        this.batas = batas;
        this.jendela = Duration.ofMinutes(menit);
        this.jam = jam;
    }

    public boolean izinkan(String kunci) {
        Instant sekarang = jam.instant();
        Deque<Instant> riwayat = catatan.computeIfAbsent(kunci, k -> new ArrayDeque<>());
        synchronized (riwayat) {
            while (!riwayat.isEmpty() && riwayat.peekFirst().isBefore(sekarang.minus(jendela))) riwayat.pollFirst();
            if (riwayat.size() >= batas) return false;
            riwayat.addLast(sekarang);
            return true;
        }
    }
}
