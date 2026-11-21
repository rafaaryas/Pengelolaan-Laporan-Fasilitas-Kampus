package com.kelompok5.pelaporanfasilitas;

import com.kelompok5.pelaporanfasilitas.model.*;
import com.kelompok5.pelaporanfasilitas.service.prioritas.PrioritasBerbasisRisiko;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrioritasBerbasisRisikoTest {
    private final PrioritasBerbasisRisiko strategi = new PrioritasBerbasisRisiko();

    @ParameterizedTest
    @CsvSource({"true,RINGAN,KRITIS", "false,BERAT,TINGGI", "false,SEBAGIAN,SEDANG", "false,RINGAN,RENDAH"})
    void prioritasDitentukanDariPemeriksaan(boolean bahaya, Gangguan tingkat, Prioritas prioritas) {
        var kasus = mock(KasusKerusakan.class);
        when(kasus.isTerverifikasi()).thenReturn(true);
        when(kasus.isBahayaKritis()).thenReturn(bahaya);
        when(kasus.getGangguan()).thenReturn(tingkat);
        assertEquals(prioritas, strategi.tentukan(kasus));
        assertFalse(strategi.jelaskan(kasus).isBlank());
    }

    @Test
    void tidakMenilaiSebelumVerifikasi() {
        var kasus = mock(KasusKerusakan.class);
        assertThrows(IllegalStateException.class, () -> strategi.tentukan(kasus));
    }
}
