plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

description = "Realtime Processor - Processes location events in real-time"

dependencies {
    implementation(project(":shared-models"))
    
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // RabbitMQ
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    
    // Redis for caching/real-time stats
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    
    // Micrometer
    implementation("io.micrometer:micrometer-registry-prometheus")
    
    // Testing
    testImplementation("org.springframework.amqp:spring-rabbit-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
}
