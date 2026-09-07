plugins {
    id("fabric-loom") version "1.13.6"
    id("maven-publish")
}

version = "${property("mod.version")}+${sc.current.version}"
group = property("mod.group") as String
base.archivesName = property("mod.id") as String

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    mappings("net.fabricmc:yarn:${sc.properties["deps.yarn"] as String}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${sc.properties["deps.fabric_api"] as String}")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = requiredJava.majorVersion.toInt()
}

java {
    sourceCompatibility = requiredJava
    targetCompatibility = requiredJava
    withSourcesJar()

    toolchain {
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        val props = mapOf(
            "version" to (sc.properties["mod.version"] as String),
            "minecraft" to (sc.properties["mod.mc_compat"] as String)
        )
        inputs.properties(props)
        filesMatching("fabric.mod.json") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        inputs.property("mixinJava", mixinJava)
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }
    }

    val modId = sc.properties["mod.id"] as String
    val modVersion = sc.properties["mod.version"] as String

    named<Jar>("jar") {
        from("../../LICENSE") { rename { "${it}_$modId" } }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the mod jar for this version and copies it into build/libs/{mod version}/"
        from(named("remapJar").flatMap { (it as AbstractArchiveTask).archiveFile })
        from(named("remapSourcesJar").flatMap { (it as AbstractArchiveTask).archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs/$modVersion"))
    }
}
