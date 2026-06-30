plugins {
    java
    jacoco
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.team10"
version = "0.0.1-SNAPSHOT"
description = "backend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    val querydslVersion = "7.1"

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-h2console")

    // QueryDSL
    implementation("io.github.openfeign.querydsl:querydsl-jpa:$querydslVersion")
    annotationProcessor("io.github.openfeign.querydsl:querydsl-apt:$querydslVersion:jpa")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")

    // Swagger(OpenAPI)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")

    // Development
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Database
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    runtimeOnly("org.flywaydb:flyway-mysql")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("com.mysql:mysql-connector-j")

    // Spring Security
    implementation("org.springframework.boot:spring-boot-starter-security")
    testImplementation("org.springframework.security:spring-security-test")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // WebSocket
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // Retry
    implementation("org.springframework.retry:spring-retry:2.0.12")
    implementation("org.aspectj:aspectjweaver")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.testcontainers:junit-jupiter:1.21.3")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // actuator
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
}

jacoco {
    toolVersion = "0.8.13"
}

val jacocoExcludePatterns = listOf(
    // bootstrap
    "**/*Application.class",

    // querydsl
    "**/generated/**",
    "**/Q*.class",

    // configuration
    "**/*Config.class",
    "**/*Properties.class",
    "**/*Constants.class",
    "**/*Constants$*.class",

    // data carriers
    "**/dto/**",
    "**/*Req.class",
    "**/*Res.class",
    "**/*Event.class",

    // exception
    "**/exception/**",

    // simple declarations
    "**/type/**",
    "**/annotation/**",

    // base entity
    "**/BaseEntity.class"
)

/**
 * ./gradlew test 수행 시
 * JUnit test 종료 후 jacocoTestReport 자동 실행
 */
tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.named("jacocoTestReport"))
}

/**
 * 테스트 커버리지 리포트 생성 설정
 * */
tasks.named<JacocoReport>("jacocoTestReport") {

    /** 테스트 커버리지 검사에서 제외할 경로 명시 */
    filterJacocoClasses(classDirectories)

    reports {
        xml.required.set(true)  // SonarQube 등의 외부 툴에서 사용
        html.required.set(true) // build/reports/jacoco/test/html/index.html
    }
}

/**
 * 테스트 커버리지 Quality Gate 정의
 * */
tasks.jacocoTestCoverageVerification {

    /** 테스트 커버리지 검사에서 제외할 경로 명시 */
    filterJacocoClasses(classDirectories)

    /** 품질 검사 통과 기준 정의 */
    violationRules {
//        특정 경로에 대해서만 CLASS를 적용해 엄격한 검사를 수행할 수 있다
//        rule {
//            enabled = true
//            element = "CLASS"
//
//            includes = listOf("com.team.service.*")
//
//            limit {
//
//            }
//        }

        rule {
            enabled = true // 규칙 활성 여부
            element = "BUNDLE" // 커버리지 검사 단위 지정 CLASS : 각 클래스 별

            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = 0.80.toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = 0.70.toBigDecimal()
            }
            limit {
                counter = "METHOD"
                value = "COVEREDRATIO"
                minimum = 0.70.toBigDecimal()
            }
            limit {
                counter = "CLASS"
                value = "COVEREDRATIO"
                minimum = 0.70.toBigDecimal()
            }
        }
    }
}

/**
 * build 시 check -> test 수행
 * 이떄 finalizedBy 설정에 의해 테스트 수행 후 jacocoTestReport 자동 실행
 * 이후 아래 설정에 의해 jacocoTestCoverageVerification 품질 게이트 검증 실행
 * 기준 만족 시에만 BUILD SUCCESS
 * */
//tasks.check {
//    dependsOn(tasks.jacocoTestCoverageVerification)
//}


/** JaCoCo 커버리지 측정/검증 제외 경로 지정 헬퍼 메서드 */
fun filterJacocoClasses(classDirectories: ConfigurableFileCollection) {
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(jacocoExcludePatterns)
            }
        })
    )
}
