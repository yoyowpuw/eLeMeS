package com.elemes.common

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Runs after Spring Security's OAuth2 resource server filter has already
 * populated `SecurityContextHolder` (unordered `Filter` beans run after
 * Security's own filter, which Spring Boot registers at a fixed early
 * order) — so the JWT principal, and therefore `tenant_id`, is available
 * here. Sets [TenantContext] for the duration of the request only; always
 * cleared in `finally`, since the underlying thread is pooled and reused
 * for unrelated requests.
 */
class TenantContextFilter : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        try {
            val principal = SecurityContextHolder.getContext().authentication?.principal
            if (principal is Jwt) {
                TenantContext.set(principal.tenantId().value)
            }
            filterChain.doFilter(request, response)
        } finally {
            TenantContext.clear()
        }
    }
}
