plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

// e.g. "v1.0.2-5-g0170a68", or empty when no tags exist
val gitDescribe: String =
    providers
        .exec {
            commandLine("sh", "-c", "git describe --tags --long HEAD 2>/dev/null || true")
        }.standardOutput.asText
        .get()
        .trim()

val latestTag: String = gitDescribe.substringBefore("-").removePrefix("v")
val commitsSinceTag: Int =
    gitDescribe
        .substringAfter("-", "")
        .substringBefore("-")
        .toIntOrNull() ?: 0

val appVersionName: String =
    when {
        latestTag.isEmpty() -> "1.0.0"
        commitsSinceTag > 0 -> "$latestTag-dev"
        else -> latestTag
    }

val appVersionCode: Int =
    appVersionName.substringBefore("-").split(".").let { parts ->
        fun part(i: Int) = parts.getOrElse(i) { "0" }.toIntOrNull() ?: 0
        part(0) * 1_000_000 + part(1) * 1_000 + part(2)
    }

val devKeystoreExists: Provider<Boolean> =
    providers
        .exec {
            commandLine("sh", "-c", "[ -f app/dev-release.keystore ] && echo yes || echo no")
        }.standardOutput.asText
        .map { it.trim() == "yes" }

android {
    namespace = "io.github.paulsnuff.betternightlight"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "io.github.paulsnuff.betternightlight"
        minSdk = 29
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        create("dev") {
            storeFile = rootProject.file("app/dev-release.keystore")
            storePassword = "betternightlight"
            keyAlias = "dev"
            keyPassword = "betternightlight"
        }

        create("release") {
            storeFile = providers
                .environmentVariable("SIGNING_STORE_FILE")
                .orNull
                ?.let { rootProject.file(it) }
                ?.takeIf { it.exists() } ?: return@create
            storePassword = providers.environmentVariable("SIGNING_STORE_PASSWORD").orNull
            keyAlias = providers.environmentVariable("SIGNING_KEY_ALIAS").orNull
            keyPassword = providers.environmentVariable("SIGNING_KEY_PASSWORD").orNull
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

            signingConfig =
                signingConfigs.getByName("release").takeIf { it.storeFile != null }
                    ?: signingConfigs.getByName("dev").takeIf { devKeystoreExists.get() }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }
    buildFeatures {
        compose = true
        aidl = true
    }
    bundle {
        language {
            enableSplit = false
        }
    }
}

abstract class GenerateLibrariesResourceTask : DefaultTask() {
    @get:Input
    abstract val modulesText: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val content =
            buildString {
                appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
                appendLine("<resources>")
                append("    <string name=\"about_libraries_list\" translatable=\"false\" formatted=\"false\">")
                append(modulesText.get())
                appendLine("</string>")
                appendLine("</resources>")
            }

        val file = outputDirectory.file("values/libraries.xml").get().asFile
        file.parentFile.mkdirs()
        file.writeText(content)
    }
}

// Computes the direct runtime dependencies (group:name:version) of the given
// variant configuration lazily; the provider chain is serialized by the
// configuration cache, so no script object references leak into the task action.
fun librariesTextProvider(configuration: Configuration): Provider<String> =
    provider {
        configuration.incoming.resolutionResult.root.dependencies
            .asSequence()
            .filterIsInstance<ResolvedDependencyResult>()
            .map { it.selected }
            .map { dependency ->
                val module =
                    requireNotNull(dependency.moduleVersion) {
                        "Dependency without module version: $dependency"
                    }
                "${module.group}:${module.name}:${module.version}"
            }.distinct()
            .sorted()
            .joinToString("\\n")
    }

androidComponents {
    onVariants { variant ->
        val generateLibrariesResource =
            tasks.register<GenerateLibrariesResourceTask>("generate${variant.name.replaceFirstChar { it.uppercase() }}LibrariesResource") {
                modulesText.set(librariesTextProvider(variant.runtimeConfiguration))
                outputDirectory.set(layout.buildDirectory.dir("generated/librariesResource/${variant.name}"))
            }
        variant.sources.res?.addGeneratedSourceDirectory(
            generateLibrariesResource,
            GenerateLibrariesResourceTask::outputDirectory,
        )
        variant.outputs.forEach { output ->
            output.outputFileName.set("BetterNightLight-$appVersionName.apk")
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.libsu.core)
    implementation(libs.commons.suncalc)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.work)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

configurations.all {
    resolutionStrategy.eachDependency {
        val forbiddenPrefixes =
            listOf(
                "com.google.android.gms",
                "com.google.firebase",
                "com.google.android.play",
                "com.google.android.ads",
                "com.google.ads",
            )
        if (forbiddenPrefixes.any { requested.group.startsWith(it) }) {
            throw GradleException(
                "BLOCKED DEPENDENCY: '${requested.group}:${requested.name}' is prohibited! " +
                    "This project is configured to remain 100% independent of Google Play Services.",
            )
        }
    }
}
