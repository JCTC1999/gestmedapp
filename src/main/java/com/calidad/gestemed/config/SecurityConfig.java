package com.calidad.gestemed.config;

import com.calidad.gestemed.domain.RolePolicy;
import com.calidad.gestemed.service.impl.AuthzService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;

import java.util.function.Predicate;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // Servicio que lee los flags de la tabla role_policies
    private final AuthzService authz;

    // === Usuarios en memoria (igual que ya tenías) ===
    @Bean
    public InMemoryUserDetailsManager users() {
        UserDetails admin     = User.withUsername("admin").password("{noop}admin")
                .roles("ADMIN","LEASING","WAREHOUSE","TECH","AUDIT","MANAGER").build();
        UserDetails leasing   = User.withUsername("leasing").password("{noop}123").roles("LEASING").build();
        UserDetails tech      = User.withUsername("tech").password("{noop}123").roles("TECH").build();
        UserDetails wh        = User.withUsername("warehouse").password("{noop}123").roles("WAREHOUSE").build();
        UserDetails audit     = User.withUsername("audit").password("{noop}123").roles("AUDIT").build();
        UserDetails manager   = User.withUsername("manager").password("{noop}123").roles("MANAGER").build();
        return new InMemoryUserDetailsManager(admin, leasing, tech, wh, audit, manager);
    }

    // === Autorización dinámica por flags ===
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // públicos
                        .requestMatchers("/login","/h2-console/**","/","/files/**","/css/**","/js/**").permitAll()
                        .requestMatchers("/tracking", "/api/gps/**").hasAnyRole("ADMIN","SUPPORT")
                        // admin UI para editar roles/políticas
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // ---- ASSETS ----
                        .requestMatchers(HttpMethod.GET,    "/assets/**")
                        .access((authSupplier, ctx) -> decision(authSupplier.get(), RolePolicy::isCanAssetsRead))
                        .requestMatchers(HttpMethod.POST,   "/assets/**")
                        .access((authSupplier, ctx) -> decision(authSupplier.get(), RolePolicy::isCanAssetsWrite))
                        .requestMatchers(HttpMethod.PUT,    "/assets/**")
                        .access((authSupplier, ctx) -> decision(authSupplier.get(), RolePolicy::isCanAssetsWrite))
                        .requestMatchers(HttpMethod.DELETE, "/assets/**")
                        .access((authSupplier, ctx) -> decision(authSupplier.get(), RolePolicy::isCanAssetsWrite))

                        // ---- CONTRACTS ----
                        .requestMatchers(HttpMethod.GET,    "/contracts/**")
                        .access((a, c) -> decision(a.get(), RolePolicy::isCanContractsRead))
                        .requestMatchers(HttpMethod.POST,   "/contracts/**")
                        .access((a, c) -> decision(a.get(), RolePolicy::isCanContractsWrite))
                        .requestMatchers(HttpMethod.PUT,    "/contracts/**")
                        .access((a, c) -> decision(a.get(), RolePolicy::isCanContractsWrite))
                        .requestMatchers(HttpMethod.DELETE, "/contracts/**")
                        .access((a, c) -> decision(a.get(), RolePolicy::isCanContractsWrite))

                        // ---- INVENTORY ----
                        .requestMatchers(HttpMethod.GET,    "/inventory/**")
                        .access((a, c) -> decision(a.get(), RolePolicy::isCanInventoryRead))
                        .requestMatchers(HttpMethod.POST,   "/inventory/**")
                        .access((a, c) -> decision(a.get(), RolePolicy::isCanInventoryWrite))
                        .requestMatchers(HttpMethod.PUT,    "/inventory/**")
                        .access((a, c) -> decision(a.get(), RolePolicy::isCanInventoryWrite))
                        .requestMatchers(HttpMethod.DELETE, "/inventory/**")
                        .access((a, c) -> decision(a.get(), RolePolicy::isCanInventoryWrite))

                        // ---- MAINTENANCE ----
                        .requestMatchers(HttpMethod.GET,    "/maintenance/**")
                        .access((a, c) -> decision(a.get(), RolePolicy::isCanMaintenanceRead))
                        .requestMatchers(HttpMethod.POST,   "/maintenance/**")
                        .access((a, c) -> decision(a.get(), RolePolicy::isCanMaintenanceWrite))
                        .requestMatchers(HttpMethod.PUT,    "/maintenance/**")
                        .access((a, c) -> decision(a.get(), RolePolicy::isCanMaintenanceWrite))
                        .requestMatchers(HttpMethod.DELETE, "/maintenance/**")
                        .access((a, c) -> decision(a.get(), RolePolicy::isCanMaintenanceWrite))

                        // ---- REPORTS (solo lectura) ----
                        .requestMatchers("/reports/**")
                        .access((a, c) -> decision(a.get(), RolePolicy::isCanReportsRead))

                        .anyRequest().authenticated()
                )
                .formLogin(Customizer.withDefaults())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                .headers(h -> h.frameOptions(f -> f.disable()));

        return http.build();
    }

    // Admin siempre pasa; para otros, miramos su primer rol y validamos contra role_policies
    private AuthorizationDecision decision(Authentication auth, Predicate<RolePolicy> checker) {
        if (auth == null) return new AuthorizationDecision(false);

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(ga -> "ROLE_ADMIN".equals(ga.getAuthority()));
        if (isAdmin) return new AuthorizationDecision(true);

        String role = auth.getAuthorities().stream()
                .map(ga -> ga.getAuthority().replace("ROLE_", ""))
                .filter(r -> !"ADMIN".equals(r))
                .findFirst().orElse(null);

        boolean allowed = (role != null) && authz.has(role, checker);
        return new AuthorizationDecision(allowed);
    }
}
