plugins {
	kotlin("jvm") version "2.2.21" apply false
	kotlin("plugin.spring") version "2.2.21" apply false
	kotlin("plugin.jpa") version "2.2.21" apply false
	id("org.springframework.boot") version "3.2.12" apply false
	id("io.spring.dependency-management") version "1.1.7" apply false
	id("org.jetbrains.dokka") version "1.9.20"
}

repositories {
	mavenCentral()
}

// Forzar versión compatible de Jackson para todas las configuraciones de Dokka
configurations.matching { it.name.contains("dokka", ignoreCase = true) }.all {
	resolutionStrategy.eachDependency {
		if (requested.group.startsWith("com.fasterxml.jackson")) {
			useVersion("2.15.3")
			because("Dokka requiere Jackson 2.15.x para compatibilidad")
		}
	}
}

subprojects {
	apply(plugin = "org.jetbrains.kotlin.jvm")
	apply(plugin = "org.jetbrains.kotlin.plugin.spring")
	apply(plugin = "io.spring.dependency-management")
	apply(plugin = "org.jetbrains.dokka")

	group = "com.verisure.techdemo"
	version = "0.0.1-SNAPSHOT"

	repositories {
		mavenCentral()
	}

	configure<JavaPluginExtension> {
		toolchain {
			languageVersion.set(JavaLanguageVersion.of(17))
		}
	}

	// Configurar Dokka para evitar conflictos de classpath
	tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
		dokkaSourceSets.configureEach {
			skipEmptyPackages.set(true)
			reportUndocumented.set(false)
			// Limpiar classpath - solo usar stdlib de Kotlin
			classpath.setFrom(files())
		}
	}

	dependencies {
		val implementation by configurations
		val testImplementation by configurations
		val testRuntimeOnly by configurations

		implementation("org.jetbrains.kotlin:kotlin-reflect")
		implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
		implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
		
		testImplementation("org.springframework.boot:spring-boot-starter-test")
		testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
		testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	}

	tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
		compilerOptions {
			freeCompilerArgs.add("-Xjsr305=strict")
			jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
		}
	}

	tasks.withType<Test> {
		useJUnitPlatform()
	}
}
