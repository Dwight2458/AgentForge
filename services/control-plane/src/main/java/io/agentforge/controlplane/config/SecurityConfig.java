package io.agentforge.controlplane.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpStatus;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AgentForgeProperties properties) throws Exception {
        if (properties.security().devAuth()) {
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        } else {
            CookieCsrfTokenRepository tokens = CookieCsrfTokenRepository.withHttpOnlyFalse();
            CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
            requestHandler.setCsrfRequestAttributeName(null);
            AuthenticationEntryPoint apiUnauthorized = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);

            http.authorizeHttpRequests(authorize -> authorize
                            .requestMatchers(
                                    "/actuator/health/**",
                                    "/actuator/info",
                                    "/api/v1/auth/session",
                                    "/api/v1/auth/csrf",
                                    "/oauth2/**",
                                    "/login/**").permitAll()
                            .anyRequest().authenticated())
                    .csrf(csrf -> csrf
                            .csrfTokenRepository(tokens)
                            .csrfTokenRequestHandler(requestHandler))
                    .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                            apiUnauthorized, new AntPathRequestMatcher("/api/**")))
                    .oauth2Login(login -> login.defaultSuccessUrl("/projects", true))
                    .logout(logout -> logout.logoutSuccessUrl("/"));
        }

        return http.build();
    }
}
