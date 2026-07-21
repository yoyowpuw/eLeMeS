package com.elemes.common

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Ch.14: the React frontend (a browser SPA on its own origin, `:5173` in
 * dev) needs CORS to call these APIs directly — every other caller so far
 * has been a server-to-server call or curl, neither of which is subject to
 * the browser's same-origin policy. `.cors {}` (empty customizer) in each
 * service's `SecurityConfig` auto-discovers this bean by type. Any
 * `localhost` port is allowed rather than a single fixed one — Vite picks
 * the next free port if `5173` is taken, and there's no production origin
 * to lock this down to yet (Ch.14's BFF layer, not yet built, would be the
 * actual production-facing origin).
 */
@Configuration
class CorsConfig {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOriginPatterns = listOf("http://localhost:*")
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("Authorization", "Content-Type")
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}
