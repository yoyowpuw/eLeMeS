package com.elemes.common

import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

/**
 * Wraps Spring Boot's autoconfigured `dataSource` bean in
 * [TenantAwareDataSource] — still a single underlying HikariCP pool, this
 * only decorates `getConnection()` — so every connection anything in the
 * application uses (`JdbcTemplate`, Flyway migrations, everything) goes
 * through it.
 *
 * A `BeanPostProcessor`, not an `@Primary @Bean` wrapping a `@Qualifier`ed
 * dependency, because `@Import` on a `@SpringBootApplication` class
 * registers this as "user" configuration, which Spring Boot's
 * bean-*definition* scanning processes before autoconfiguration classes —
 * so at that point there is no `dataSource` bean *definition* yet for a
 * `@Qualifier`/`@Lazy`-injected parameter to resolve, `@Lazy` only defers
 * *instantiation* of an already-locatable definition, not definition
 * visibility. A `BeanPostProcessor` sidesteps the ordering question
 * entirely: it intercepts the bean named `dataSource` as part of its
 * normal initialization lifecycle, whenever that happens to occur.
 *
 * Each service opts in via `@Import(TenantDataSourceConfig::class)` on its
 * `@SpringBootApplication` class — this lives in `common` because it's
 * identical everywhere, unlike SecurityConfig/AuthorizationConfig which
 * have real per-service differences.
 */
@Configuration
class TenantDataSourceConfig {

    @Bean
    fun tenantAwareDataSourcePostProcessor(): BeanPostProcessor = object : BeanPostProcessor {
        override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
            if (beanName == "dataSource" && bean is DataSource) {
                return TenantAwareDataSource(bean)
            }
            return bean
        }
    }
}
