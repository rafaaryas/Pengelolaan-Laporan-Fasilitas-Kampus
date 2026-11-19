package com.kelompok5.pelaporanfasilitas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/** Akun khusus pengembangan. Password berasal dari environment lokal. */
@Configuration
@Profile("local")
public class LocalDemoUsersConfig {
    @Bean
    public UserDetailsService localUsers(PasswordEncoder encoder,
            @Value("${DEMO_MAHASISWA_PASSWORD}") String mahasiswaPassword,
            @Value("${DEMO_ADMIN_PASSWORD}") String adminPassword,
            @Value("${DEMO_PETUGAS_PASSWORD}") String petugasPassword) {
        return new InMemoryUserDetailsManager(
                User.withUsername("mahasiswa").password(encode(encoder, mahasiswaPassword)).roles("MAHASISWA").build(),
                User.withUsername("admin").password(encode(encoder, adminPassword)).roles("ADMIN").build(),
                User.withUsername("petugas").password(encode(encoder, petugasPassword)).roles("PETUGAS").build());
    }

    private String encode(PasswordEncoder encoder, String password) {
        if (password == null || password.isBlank() || password.startsWith("CHANGE_ME") || password.length() < 12) {
            throw new IllegalArgumentException("Isi password demo lokal yang unik minimal 12 karakter di .env");
        }
        return encoder.encode(password);
    }
}
