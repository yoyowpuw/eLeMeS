package com.elemes.certification.infrastructure

import com.elemes.common.TenantContextFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain

/**
 * Ch.16 ADR-026 — see course-management's SecurityConfig for the full
 * rationale. One deliberate deviation here: Ch.26 §6 requires a certificate
 * be independently verifiable by a third party (an auditor, a regulator)
 * *without platform access* — gating `/verify` and `/public-key` behind a
 * token would directly contradict that requirement, so those two stay
 * public while everything else (lookup, revoke) requires authentication.
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
                    .requestMatchers("/api/v1/certificates/public-key").permitAll()
                    .requestMatchers("/api/v1/certificates/*/verify").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { it.jwt {} }
            // Ch.12 §2: runs after Bearer-token auth resolves the JWT, so it
            // can read tenant_id and set TenantContext for Postgres RLS.
            .addFilterAfter(TenantContextFilter(), BearerTokenAuthenticationFilter::class.java)
        return http.build()
    }
}
