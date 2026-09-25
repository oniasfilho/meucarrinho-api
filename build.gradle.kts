// One module: layers and adapters are packages under app.meucarrinho, and the
// ArchUnit rules in ArchitectureTest keep them apart (spec §3, §11).
plugins {
    java
    `java-test-fixtures`
    `jvm-test-suite`
    alias(libs.plugins.spring.boot)
}

group = "app.meucarrinho"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Boot's BOM manages versions everywhere; the Boot plugin only packages bootJar.
    val bom = platform(libs.spring.boot.bom)
    implementation(bom)
    testFixturesImplementation(bom)
    testImplementation(bom)

    implementation(libs.spring.boot.starter)
    implementation(libs.jspecify)

    testFixturesApi(libs.jspecify)
    testFixturesApi(libs.junit.jupiter)
    testFixturesApi(libs.assertj)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testImplementation(libs.archunit)
    testImplementation(libs.jqwik)
    testImplementation(libs.jackson.databind)
    testRuntimeOnly(libs.junit.platform.launcher)
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }
        // Testcontainers runs of every adapter land here from session 2 on (`make it`).
        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter()
            dependencies {
                implementation(project())
                implementation(testFixtures(project()))
                implementation(platform(libs.spring.boot.bom))
                implementation(libs.assertj)
            }
            targets.all {
                testTask.configure { shouldRunAfter(test) }
            }
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing", "-Werror"))
}

tasks.withType<Test>().configureEach {
    // The shared quick-add vectors live at the repo root, outside the resources.
    systemProperty("carrinho.contracts.dir", rootProject.file("contracts").absolutePath)
}
