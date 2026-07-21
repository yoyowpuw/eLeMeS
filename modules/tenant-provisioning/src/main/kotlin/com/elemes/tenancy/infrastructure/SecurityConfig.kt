package com.elemes.tenancy.infrastructure

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

/**
 * Ch.16 ADR-026: this service only validates tokens issued by the CIAM
 * platform (Keycloak locally) — it never issues or checks passwords itself.
 * No TenantContextFilter/RLS here — the tenants table isn't itself
 * RLS-protected (see its migration's doc comment).
 */
@Configuration
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { }
            .authorizeHttpRequests {
                it.requestMatchers("/actuator/**").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { it.jwt {} }
        return http.build()
    }
}
