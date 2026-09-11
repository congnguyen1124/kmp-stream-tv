import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ktlint)
}

val ktlintCliVersion =
    libs.versions.ktlint.cli
        .get()

allprojects {
    pluginManager.withPlugin("org.jlleitschuh.gradle.ktlint") {
        extensions.configure<KtlintExtension> {
            version.set(ktlintCliVersion)
            outputToConsole.set(true)
            ignoreFailures.set(false)
            reporters {
                reporter(ReporterType.PLAIN)
                reporter(ReporterType.CHECKSTYLE)
            }
            filter {
                exclude("**/build/**")
                exclude("**/generated/**")
            }
        }
    }
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}
