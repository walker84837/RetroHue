plugins {
    `java-library`
    `maven-publish`
}

group = "org.winlogon"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("net.kyori:adventure-api:4.21.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.21.0")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.junit.jupiter)
    testImplementation("net.kyori:adventure-api:4.21.0")
    testImplementation("net.kyori:adventure-text-minimessage:4.21.0")
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
            groupId = "org.winlogon"
            artifactId = "retrohue"
            pom {
                name = "RetroHue"
                description = "Convert legacy ampersand color codes into MiniMessage tags"
                url = "https://github.com/walker84837/RetroHue"
                licenses {
                    license {
                        name = "MIT License"
                        url = "https://opensource.org/licenses/MIT"
                    }
                }
                developers {
                    developer {
                        id = "walker84837"
                        name = "winlogon"
                        email = "walker84837@gmail.com"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/walker84837/RetroHue.git"
                    developerConnection = "scm:git:ssh://github.com/walker84837/RetroHue.git"
                    url = "https://github.com/walker84837/RetroHue"
                }
            }
        }
    }
    repositories {
        maven {
            name = "winlogon-libs"
            url = uri("https://maven.winlogon.org/releases")
            credentials {
                username = (project.findProperty("reposiliteUser") as String?) ?: System.getenv("MAVEN_USERNAME")
                password = (project.findProperty("reposilitePassword") as String?) ?: System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}
