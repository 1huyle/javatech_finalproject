package com.example.demo.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Public pages & resources
                .requestMatchers(
                    "/", "/index", "/about", "/contact", "/login", "/register", "/forgot-password",
                    "/auth-redirect", "/error/**", "/assets/**", "/css/**", "/js/**",
                    "/images/**", "/fonts/**", "/favicon.ico", "/api/auth/**", "/api/users/verify", 
                    "/.well-known/**", "/swagger-ui/**", "/api-docs/**"
                ).permitAll()

                // Account pages:
                .requestMatchers("/account").authenticated() // General /account page needs authentication
                .requestMatchers("/account/**").hasRole("REALTOR") // Deeper /account paths for REALTORs

                // Transaction forms & user-specific transaction data - USER only
                .requestMatchers(
                    "/property/*/purchase",
                    "/property/*/lease",
                    "/transactions/purchase",
                    "/transactions/lease",
                    "/transactions/my-requests",
                    "/transactions/api/user-transactions",
                    "/payment" // Payment page for USER
                ).hasRole("USER")
                
                // Transaction endpoints for REALTOR
                .requestMatchers(
                    "/transactions/realtor-requests"
                ).hasRole("REALTOR")
                
                // Transaction details - accessible to authenticated users (both USER and REALTOR)
                .requestMatchers("/transactions/*").authenticated()

                // General property browsing pages - accessible to anonymous and USER.
                .requestMatchers(
                    "/listings",
                    "/apartments", "/villas", "/condominiums", "/townhouses", "/offices", "/land",
                    "/sales", "/rentals"
                ).access(new WebExpressionAuthorizationManager("isAnonymous() or hasRole('USER')"))

                // Property detail page - accessible to anonymous, USER, and REALTOR for now.
                .requestMatchers("/listing-detail/**")
                .access(new WebExpressionAuthorizationManager("isAnonymous() or hasRole('USER') or hasRole('REALTOR')"))
                
                // Specific property detail page (e.g. /property/15)
                .requestMatchers("/property/*").access(new WebExpressionAuthorizationManager("isAnonymous() or hasRole('USER')"))
                // Root /property page if it's a listing page
                .requestMatchers("/property").access(new WebExpressionAuthorizationManager("isAnonymous() or hasRole('USER')"))

                // Transaction API checks - public for now
                .requestMatchers("/transactions/api/check-availability").permitAll()
                // Original /transactions/** permitAll - review if still needed or too broad
                // For now, let's assume specific transaction endpoints are covered above or require auth
                // .requestMatchers("/transactions/**").permitAll() // Commenting out, be more specific

                // API for properties (add, update, delete by realtor)
                .requestMatchers("/api/properties/add", "/api/properties/update/**", "/api/properties/delete/**").hasRole("REALTOR")
                // API for realtor requests
                .requestMatchers("/api/realtor-requests").hasRole("REALTOR") // Explicitly for realtor requests API

                // Authenticated specific transaction endpoints that might be used by JS after page load
                .requestMatchers("/api/transactions/user/**").hasRole("USER") // Example if there's a common base path for user transactions api
                .requestMatchers("/api/transactions/realtor/**").hasRole("REALTOR")// Example for realtor transaction api

                // Other general authenticated APIs (accessible to both USER and REALTOR, or secured with @PreAuthorize)
                .requestMatchers("/api/users/profile", "/api/users/update-profile", "/api/users/change-password", "/api/users/avatar").authenticated()
                .requestMatchers("/api/property-types").permitAll() // Assuming property types are public

                // All other requests require authentication (catch-all)
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/") // Default, but might be overridden by CustomAuthenticationSuccessHandler
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout") // Explicit logout URL
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
