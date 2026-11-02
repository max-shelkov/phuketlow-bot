plugins {
    kotlin("jvm") version "2.2.0"
    application
}

group = "com.phuketlow"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    // Apache POI — чтение xlsx (таблица вилл)
    implementation("org.apache.poi:poi-ooxml:5.2.5")
}

application {
    mainClass.set("com.phuketlow.bot.MainKt")
}

kotlin {
    jvmToolchain(17)
}
