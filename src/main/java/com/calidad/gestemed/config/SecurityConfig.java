package com.calidad.gestemed.config;

import com.calidad.gestemed.domain.RolePolicy;
import com.calidad.gestemed.service.impl.AuthzService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.function.Predicate;


//Esta clase es muy importante ya que es la clase que permite la autenticación de los usuarios


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // Servicio que lee los flags de la tabla role_policies
    private final AuthzService authz;
    // El UserDetailsService para cargar usuarios desde la BD
    private final UserDetailsService userDetailsService;

    /**
     * Bean para codificar y verificar contraseñas.
     * Usa el algoritmo BCrypt, que es el estándar recomendado.
     */
    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Define el "proveedor de autenticación".
     * Le dice a Spring Security que debe usar nuestro UserDetailsService para buscar usuarios
     * y nuestro PasswordEncoder para comprobar las contraseñas.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Configuración principal de la seguridad HTTP.
     */
    //Autorización dinámica por flags

    //HttpSecurity: Es el objeto principal de Spring Security que se usa para configurar la protección de las solicitudes HTTP.

    // authorizeHttpRequests(...): Esta sección define las reglas de acceso a las diferentes URLs de la aplicación.

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(authenticationProvider())
                .authorizeHttpRequests(auth -> auth
                        // --- URLs Públicas ---
                        .requestMatchers("/login", "/", "/css/**", "/js/**", "/files/**", "/h2-console/**").permitAll()
                        .requestMatchers("/api/geofences").permitAll()

                        // --- URLs por Rol ---
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/tracking", "/api/gps/**").hasAnyRole("ADMIN", "SUPPORT")

                        // --- URLs con Lógica Dinámica ---
                        .requestMatchers(HttpMethod.GET, "/assets/**").access((a, c) -> decision(a.get(), RolePolicy::isCanAssetsRead))
                        .requestMatchers(HttpMethod.GET, "/assets/new").access((a, c) -> decision(a.get(), RolePolicy::isCanAssetsWrite))
                        .requestMatchers(HttpMethod.POST, "/assets/**").access((a, c) -> decision(a.get(), RolePolicy::isCanAssetsWrite))
                        .requestMatchers(HttpMethod.PUT, "/assets/**").access((a, c) -> decision(a.get(), RolePolicy::isCanAssetsWrite))
                        .requestMatchers(HttpMethod.DELETE, "/assets/**").access((a, c) -> decision(a.get(), RolePolicy::isCanAssetsWrite))

                        // resto de reglas para contracts, inventory, etc
                        .requestMatchers(HttpMethod.GET,    "/contracts/**").access((a, c) -> decision(a.get(), RolePolicy::isCanContractsRead))
                        .requestMatchers(HttpMethod.POST,   "/contracts/**").access((a, c) -> decision(a.get(), RolePolicy::isCanContractsWrite))
                        .requestMatchers(HttpMethod.PUT,    "/contracts/**").access((a, c) -> decision(a.get(), RolePolicy::isCanContractsWrite))
                        .requestMatchers(HttpMethod.DELETE, "/contracts/**").access((a, c) -> decision(a.get(), RolePolicy::isCanContractsWrite))
                        .requestMatchers(HttpMethod.GET,    "/inventory/**").access((a, c) -> decision(a.get(), RolePolicy::isCanInventoryRead))
                        .requestMatchers(HttpMethod.POST,   "/inventory/**").access((a, c) -> decision(a.get(), RolePolicy::isCanInventoryWrite))
                        .requestMatchers(HttpMethod.PUT,    "/inventory/**").access((a, c) -> decision(a.get(), RolePolicy::isCanInventoryWrite))
                        .requestMatchers(HttpMethod.DELETE, "/inventory/**").access((a, c) -> decision(a.get(), RolePolicy::isCanInventoryWrite))
                        .requestMatchers(HttpMethod.GET,    "/maintenance/**").access((a, c) -> decision(a.get(), RolePolicy::isCanMaintenanceRead))
                        .requestMatchers(HttpMethod.POST,   "/maintenance/**").access((a, c) -> decision(a.get(), RolePolicy::isCanMaintenanceWrite))
                        .requestMatchers(HttpMethod.PUT,    "/maintenance/**").access((a, c) -> decision(a.get(), RolePolicy::isCanMaintenanceWrite))
                        .requestMatchers(HttpMethod.DELETE, "/maintenance/**").access((a, c) -> decision(a.get(), RolePolicy::isCanMaintenanceWrite))
                        .requestMatchers("/reports/**").access((a, c) -> decision(a.get(), RolePolicy::isCanReportsRead))
                        .requestMatchers(HttpMethod.GET,    "/acts/**").access((a,c)->decision(a.get(), RolePolicy::isCanContractsRead))
                        .requestMatchers(HttpMethod.POST,   "/acts/**").access((a,c)->decision(a.get(), RolePolicy::isCanContractsWrite))
                        .requestMatchers(HttpMethod.PUT,    "/acts/**").access((a,c)->decision(a.get(), RolePolicy::isCanContractsWrite))
                        .requestMatchers(HttpMethod.DELETE, "/acts/**").access((a,c)->decision(a.get(), RolePolicy::isCanContractsWrite))


                        // --- Regla Final ---
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error=true")
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .csrf(csrf -> csrf.disable())
                .headers(h -> h.frameOptions(f -> f.disable()));

        return http.build();
    }

    //Lógica de decisión para la autorización dinámica.
    // este metodo AuthorizationDecision es el corazon de la logica de autorizacion.
    // este método determina si el usuario que está autenticado tiene permiso para realizar la acción
    // toma el rol del usuario autenticado y busca en la base de datos si ese rol tiene permiso para hacer determinada acción
    // Admin siempre pasa; para otros, miramos su primer rol y validamos contra role_policies

    private AuthorizationDecision decision(Authentication auth, Predicate<RolePolicy> checker) {

        // si el objeto auth es nulo significa que no hay usuario autenticado y devuelve el AuthorizationDecision con el valor false

        if (auth == null || !auth.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }

        // El admin siempre tiene acceso.
        // En caso de que el usuario autenticado tenga el role de admin entonces se permite hacer todo de una vez
        // auth.getAuthorities().stream lo que hace es traer todos los roles que tiene el usuario autenticado.
        //se recorren esos roles y si alguno coincide con el role de admin entonces el método nmediatamente devuelve un AuthorizationDecision con el valor true.
        // Esto significa que un administrador siempre tiene acceso completo, sin importar las demás políticas.
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(ga -> "ROLE_ADMIN".equals(ga.getAuthority()));
        if (isAdmin) {
            return new AuthorizationDecision(true);
        }

        // Comprueba si CUALQUIERA de los roles del usuario cumple con el permiso.
        boolean allowed = auth.getAuthorities().stream()
                .map(ga -> ga.getAuthority().replace("ROLE_", ""))
                .anyMatch(role -> authz.has(role, checker));

        return new AuthorizationDecision(allowed);
    }
}