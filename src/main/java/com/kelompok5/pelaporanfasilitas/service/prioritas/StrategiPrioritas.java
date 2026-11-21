package com.kelompok5.pelaporanfasilitas.service.prioritas;

import com.kelompok5.pelaporanfasilitas.model.KasusKerusakan;
import com.kelompok5.pelaporanfasilitas.model.Prioritas;

public interface StrategiPrioritas {
    Prioritas tentukan(KasusKerusakan kasus);
    String jelaskan(KasusKerusakan kasus);
}
