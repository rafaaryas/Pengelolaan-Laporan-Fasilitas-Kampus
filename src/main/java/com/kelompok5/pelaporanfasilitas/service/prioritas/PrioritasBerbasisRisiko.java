package com.kelompok5.pelaporanfasilitas.service.prioritas;

import com.kelompok5.pelaporanfasilitas.model.KasusKerusakan;
import com.kelompok5.pelaporanfasilitas.model.Prioritas;
import org.springframework.stereotype.Component;

@Component
public class PrioritasBerbasisRisiko implements StrategiPrioritas {
    @Override
    public Prioritas tentukan(KasusKerusakan kasus) {
        if (!kasus.isSudahDiperiksa()) throw new IllegalStateException("Catat pemeriksaan sebelum menentukan prioritas.");
        if (kasus.isBahayaKritis()) return Prioritas.KRITIS;
        return switch (kasus.getGangguan()) {
            case BERAT -> Prioritas.TINGGI;
            case SEBAGIAN -> Prioritas.SEDANG;
            case RINGAN -> Prioritas.RENDAH;
        };
    }

    @Override
    public String jelaskan(KasusKerusakan kasus) {
        return switch (tentukan(kasus)) {
            case KRITIS -> "Petugas mengonfirmasi bahaya keselamatan kritis.";
            case TINGGI -> "Pemeriksaan menunjukkan fasilitas tidak dapat digunakan dan tidak ada bahaya keselamatan kritis.";
            case SEDANG -> "Pemeriksaan menunjukkan sebagian fungsi fasilitas terganggu.";
            case RENDAH -> "Pemeriksaan menunjukkan kerusakan dengan gangguan penggunaan terbatas.";
        };
    }
}
