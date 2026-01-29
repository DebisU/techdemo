plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

description = "API Gateway - Location ingestion service"

dependencies {
    implementation(project(":shared-models"))
    
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    // RabbitMQ
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    
    // Micrometer for metrics
    implementation("io.micrometer:micrometer-registry-prometheus")
    
    // Testing
    testImplementation("org.springframework.amqp:spring-rabbit-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
}
