package com.kelompok5.pelaporanfasilitas;

import java.util.UUID;
import com.kelompok5.pelaporanfasilitas.config.LocalDemoUsersConfig;
import com.kelompok5.pelaporanfasilitas.controller.PageController;
import com.kelompok5.pelaporanfasilitas.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PageController.class)
@Import({SecurityConfig.class, LocalDemoUsersConfig.class})
@ActiveProfiles("local")
class PageSecurityTest {
    private static final String PASSWORD = UUID.randomUUID().toString();
    @Autowired private MockMvc mvc;

    @DynamicPropertySource
    static void demoPasswords(DynamicPropertyRegistry registry) {
        for (String role : new String[]{"MAHASISWA", "ADMIN", "PETUGAS"}) {
            registry.add("DEMO_" + role + "_PASSWORD", () -> PASSWORD);
        }
    }

    @Test
    void landingIsPublicAndRendered() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("index"))
            .andExpect(content().string(containsString("Bersama merawat")))
            .andExpect(content().string(containsString("href=\"/css/app.css\"")));
        mvc.perform(get("/login")).andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @ParameterizedTest
    @CsvSource({"/mahasiswa,MAHASISWA,Mahasiswa", "/admin,ADMIN,Admin", "/petugas,PETUGAS,Petugas"})
    void roleRoutesRequireTheCorrectRole(String route, String role, String label) throws Exception {
        mvc.perform(get(route)).andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
        mvc.perform(get(route).with(user("anggota").roles(role))).andExpect(status().isOk())
            .andExpect(content().string(containsString(label)))
            .andExpect(content().string(containsString("anggota")));
        mvc.perform(get(route).with(user("lain").roles("OTHER"))).andExpect(status().isForbidden());
    }

    @Test
    void mahasiswaCannotOpenAdminAndPetugasPages() throws Exception {
        for (String route : new String[]{"/admin", "/petugas"}) {
            mvc.perform(get(route).with(user("mahasiswa").roles("MAHASISWA"))).andExpect(status().isForbidden());
        }
    }

    @ParameterizedTest
    @CsvSource({"mahasiswa", "admin", "petugas"})
    void localAccountsCanActuallyLogin(String username) throws Exception {
        mvc.perform(formLogin().user(username).password(PASSWORD)).andExpect(authenticated().withUsername(username));
    }

    @Test
    void incorrectPasswordFails() throws Exception {
        mvc.perform(formLogin().user("admin").password(UUID.randomUUID().toString())).andExpect(unauthenticated());
    }

    @Test
    void csrfIsRequiredForLoginAndLogout() throws Exception {
        mvc.perform(post("/login").param("username", "admin").param("password", PASSWORD)).andExpect(status().isForbidden());
        mvc.perform(post("/logout").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(post("/logout").with(user("admin").roles("ADMIN")).with(csrf()))
            .andExpect(status().is3xxRedirection()).andExpect(unauthenticated());
    }

    @Test
    void staticAssetsArePublic() throws Exception {
        mvc.perform(get("/css/app.css")).andExpect(status().isOk()).andExpect(content().string(containsString(":root")));
        mvc.perform(get("/js/app.js")).andExpect(status().isOk()).andExpect(content().string(containsString("password-toggle")));
    }
}
