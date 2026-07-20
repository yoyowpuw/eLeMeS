package com.elemes.enrollment.infrastructure

import com.elemes.common.OpaAuthorizer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AuthorizationConfig {

    @Bean
    fun opaAuthorizer(@Value("\${opa.base-url}") opaBaseUrl: String): OpaAuthorizer = OpaAuthorizer(opaBaseUrl)
}
