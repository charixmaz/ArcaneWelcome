plugins {
    java
    id("com.gradleup.shadow") version "9.2.2"
}
group = "dev.arcane"
version = "0.1.0"
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}
dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    implementation("org.xerial:sqlite-jdbc:3.50.3.0")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }
tasks.test { useJUnitPlatform() }
tasks.shadowJar { archiveClassifier.set(""); mergeServiceFiles() }
tasks.jar { archiveClassifier.set("unbundled") }
tasks.build { dependsOn(tasks.shadowJar) }
