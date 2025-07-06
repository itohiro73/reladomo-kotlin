package io.github.kotlinreladomo.spring.repository

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.core.annotation.AliasFor
import java.lang.annotation.Inherited
import kotlin.reflect.KClass

/**
 * Annotation to enable Reladomo repositories.
 * Similar to @EnableJpaRepositories for familiarity.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
@Import(ReladomoRepositoriesRegistrar::class)
annotation class EnableReladomoRepositories(
    /**
     * Base packages to scan for annotated components.
     */
    @get:AliasFor("basePackages")
    vararg val value: String = [],
    
    /**
     * Base packages to scan for annotated components.
     */
    @get:AliasFor("value")
    val basePackages: Array<String> = [],
    
    /**
     * Type-safe alternative to basePackages for specifying the packages to scan.
     */
    val basePackageClasses: Array<KClass<*>> = [],
    
    /**
     * Specifies which types are eligible for component scanning.
     */
    val includeFilters: Array<ComponentScan.Filter> = [],
    
    /**
     * Specifies which types are not eligible for component scanning.
     */
    val excludeFilters: Array<ComponentScan.Filter> = [],
    
    /**
     * Returns the postfix to be used when looking up custom repository implementations.
     */
    val repositoryImplementationPostfix: String = "Impl",
    
    /**
     * Configures the name of the DataSource bean to use.
     */
    val dataSourceRef: String = "",
    
    /**
     * Whether to enable automatic custom implementation detection.
     */
    val enableDefaultTransactions: Boolean = true
)