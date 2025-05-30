plugins {
    `java-library`
    `maven-publish`
}


group = "org.winlogon.retrohue"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("net.kyori:adventure-api:4.21.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.21.0")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    api(libs.commons.math3)

    testImplementation(libs.junit.jupiter)
    implementation(libs.guava)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}


publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "retrohue"
        }
    }
    repositories {
        maven {
            name = "Gitea"
            url = uri("https://winlogon.ddns.net/api/packages/winlogon/maven")
            authentication {
                create("header", HttpHeaderAuthentication::class)
            }
            credentials(HttpHeaderCredentials::class) {
                name = "Authorization"
                value = "token ${System.getenv("ACTIONS_TOKEN")}"
            }
        }
    }
}
