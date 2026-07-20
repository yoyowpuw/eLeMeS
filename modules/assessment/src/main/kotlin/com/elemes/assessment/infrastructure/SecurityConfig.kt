package com.elemes.assessment.infrastructure

import com.elemes.common.TenantContextFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain

/** Ch.16 ADR-026 — see course-management's SecurityConfig for the full rationale. */
@Configuration
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers("/actuator/**").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { it.jwt {} }
            // Ch.12 §2: runs after Bearer-token auth resolves the JWT, so it
            // can read tenant_id and set TenantContext for Postgres RLS.
            .addFilterAfter(TenantContextFilter(), BearerTokenAuthenticationFilter::class.java)
        return http.build()
    }
}
