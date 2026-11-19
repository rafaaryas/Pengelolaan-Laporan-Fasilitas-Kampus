package com.kelompok5.pelaporanfasilitas.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/", "/login", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                .requestMatchers("/mahasiswa", "/mahasiswa/**").hasRole("MAHASISWA")
                .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                .requestMatchers("/petugas", "/petugas/**").hasRole("PETUGAS")
                .anyRequest().denyAll())
            .formLogin(login -> login.loginPage("/login").defaultSuccessUrl("/", false).permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/?logout"));
        // CSRF dan session protection tetap menggunakan default Spring Security.
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Profile("!local")
    public UserDetailsService unavailableProductionUsers() {
        return username -> { throw new UsernameNotFoundException("Autentikasi produksi belum dikonfigurasi"); };
    }
}
