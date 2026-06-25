plugins {
    java
    id("com.gradleup.shadow") version "9.4.2"
}

val targetJavaVersion = 21

allprojects {
    group = "ru.matveylegenda"
    version = "1.4.1"

    repositories {
        mavenCentral()
        maven("https://libraries.minecraft.net")
        maven("https://jitpack.io")
        maven("https://repo.alessiodp.com/releases/")
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

subprojects {
    pluginManager.apply("java")

    dependencies {
        implementation("com.j256.ormlite:ormlite-jdbc:6.1")
        implementation("com.zaxxer:HikariCP:7.1.0")
        implementation("at.favre.lib:bcrypt:0.10.2")
        implementation("de.mkammerer:argon2-jvm:2.12")
        implementation("com.github.ben-manes.caffeine:caffeine:3.2.4")
        implementation("net.elytrium:serializer:1.1.1")
        implementation("dev.samstevens.totp:totp:1.7.1")

        compileOnly("net.kyori:adventure-api:5.1.1")
        compileOnly("net.kyori:adventure-text-minimessage:5.1.1")
        compileOnly("net.kyori:adventure-text-serializer-legacy:5.1.1")

        compileOnly("org.xerial:sqlite-jdbc:3.53.2.0")
        compileOnly("com.h2database:h2:2.4.240")
        compileOnly("com.mysql:mysql-connector-j:9.7.0")
        compileOnly("org.postgresql:postgresql:42.7.11")

        compileOnly("org.projectlombok:lombok:1.18.46")
        annotationProcessor("org.projectlombok:lombok:1.18.46")

        implementation("net.java.dev.jna:jna:5.17.0")
    }

    extensions.configure<JavaPluginExtension> {
        val javaVersion = JavaVersion.toVersion(targetJavaVersion)

        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion

        if (JavaVersion.current() < javaVersion) {
            toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(targetJavaVersion)
    }
}

dependencies {
    implementation(project(":common"))
    implementation(project(":bungee"))
    implementation(project(":velocity"))
}

tasks.shadowJar {
    archiveClassifier.set("")

    relocate("org.bstats", "ru.matveylegenda.tiauth.thirdparty.org.bstats")
    relocate("com.j256.ormlite", "ru.matveylegenda.tiauth.thirdparty.com.j256.ormlite")
    relocate("com.zaxxer.hikari", "ru.matveylegenda.tiauth.thirdparty.com.zaxxer.hikari")
    relocate("at.favre.lib", "ru.matveylegenda.tiauth.thirdparty.at.favre.lib")
    relocate("de.mkammerer.argon2", "ru.matveylegenda.tiauth.thirdparty.de.mkammerer.argon2")
    relocate("com.github.benmanes.caffeine", "ru.matveylegenda.tiauth.thirdparty.com.github.benmanes.caffeine")
    relocate("com.google.errorprone", "ru.matveylegenda.tiauth.thirdparty.com.google.errorprone")
    relocate("org.jspecify", "ru.matveylegenda.tiauth.thirdparty.org.jspecify")
    relocate("net.elytrium.serializer", "ru.matveylegenda.tiauth.thirdparty.net.elytrium.serializer")
    relocate("dev.samstevens.totp", "ru.matveylegenda.tiauth.thirdparty.dev.samstevens.totp")
    relocate("net.byteflux", "ru.matveylegenda.tiauth.thirdparty.net.byteflux")

    exclude("net/kyori/**")
    exclude("org/slf4j/**")
    exclude("META-INF/maven/**")

    mergeServiceFiles()

    minimize {
        exclude(project(":common"))
        exclude(project(":bungee"))
        exclude(project(":velocity"))

        exclude("ru/matveylegenda/.*")
    }
}

tasks.jar {
    enabled = false
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}