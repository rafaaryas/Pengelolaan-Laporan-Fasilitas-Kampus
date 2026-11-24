package com.kelompok5.pelaporanfasilitas.security;

import java.util.List;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository konteks,
                                            RequestCache tujuanAwal, CsrfTokenRepository csrf) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/petugas/**").hasRole("PETUGAS")
                        .requestMatchers("/mahasiswa/**").hasRole("MAHASISWA")
                        .requestMatchers("/berkas/**").authenticated()
                        // Beranda, /login, /akses-ditolak, /error, dan aset statis bersifat publik.
                        .anyRequest().permitAll())
                .csrf(c -> c.csrfTokenRepository(csrf))
                .securityContext(s -> s.securityContextRepository(konteks))
                .requestCache(r -> r.requestCache(tujuanAwal))
                .sessionManagement(s -> s.sessionFixation(f -> f.changeSessionId()))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                        .accessDeniedPage("/akses-ditolak"))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // "Masuk dengan akun lain" mengirim lanjut=login; tujuan tetap, bukan dari masukan pengguna.
                .logout(l -> l.logoutUrl("/logout").logoutSuccessHandler((request, response, auth) ->
                        response.sendRedirect(request.getContextPath()
                                + ("login".equals(request.getParameter("lanjut")) ? "/login" : "/?keluar"))))
                .build();
    }

    @Bean
    SecurityContextRepository securityContextRepository() { return new HttpSessionSecurityContextRepository(); }

    @Bean
    RequestCache requestCache() { return new HttpSessionRequestCache(); }

    @Bean
    CsrfTokenRepository csrfTokenRepository() { return new HttpSessionCsrfTokenRepository(); }

    /** Session fixation protection dan token CSRF baru setelah masuk. */
    @Bean
    SessionAuthenticationStrategy strategiSesiMasuk(CsrfTokenRepository csrf) {
        return new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(), new CsrfAuthenticationStrategy(csrf)));
    }
}
