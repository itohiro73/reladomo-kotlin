package io.github.kotlinreladomo.gradle

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

/**
 * Extension for configuring the Kotlin Reladomo plugin.
 */
interface KotlinReladomoExtension {
    /**
     * Directory containing Reladomo XML files.
     * Default: src/main/resources/reladomo
     */
    val xmlDirectory: DirectoryProperty
    
    /**
     * Output directory for generated Kotlin files.
     * Default: build/generated/kotlin
     */
    val outputDirectory: DirectoryProperty
    
    /**
     * Base package name for generated classes.
     * Default: ${project.group}.kotlin
     */
    val packageName: Property<String>
    
    /**
     * Whether to generate repository classes.
     * Default: true
     */
    val generateRepositories: Property<Boolean>
    
    /**
     * Whether to generate bitemporal support.
     * Default: true
     */
    val generateBiTemporalSupport: Property<Boolean>
}