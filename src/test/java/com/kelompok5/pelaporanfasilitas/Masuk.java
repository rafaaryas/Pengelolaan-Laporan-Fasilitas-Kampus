package com.kelompok5.pelaporanfasilitas;

import com.kelompok5.pelaporanfasilitas.security.AkunMasuk;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Bantuan tes: masuk lewat form /login yang sebenarnya, lalu pakai sesi yang sama. */
final class Masuk {
    private Masuk() {}

    static MockHttpSession sebagai(MockMvc mvc, String identitas) throws Exception {
        return lanjut(mvc, new MockHttpSession(), identitas);
    }

    static MockHttpSession lanjut(MockMvc mvc, MockHttpSession sesi, String identitas) throws Exception {
        mvc.perform(post("/login").session(sesi).with(csrf()).param("identitas", identitas))
                .andExpect(status().isOk()).andExpect(view().name("masuk-berhasil"));
        return sesi;
    }

    static AkunMasuk akun(MockHttpSession sesi) {
        var konteks = (SecurityContext) sesi.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        return (AkunMasuk) konteks.getAuthentication().getPrincipal();
    }
}
