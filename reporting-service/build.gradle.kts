plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

description = "Reporting Service - Stores and reports location data"

dependencies {
    implementation(project(":shared-models"))
    
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    
    // RabbitMQ
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    
    // PostgreSQL
    runtimeOnly("org.postgresql:postgresql")
    
    // Redis for caching
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    
    // Micrometer
    implementation("io.micrometer:micrometer-registry-prometheus")
    
    // Testing
    testImplementation("org.springframework.amqp:spring-rabbit-test")
    testImplementation("com.h2database:h2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
}
