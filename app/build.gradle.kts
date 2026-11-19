import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.io.File

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.prismbrowser.k4m9qx"
    minSdk = 26
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Do not remove or modify these signingConfigs. They are necessary for building, installing,
  // and updating Android apps in AI Studio.
  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  packaging {
    jniLibs {
      // Ensures uncompressed 16 KB aligned native libraries for Android 15+ devices
      useLegacyPackaging = false
    }
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Wire Rust cdylib compilation via cargo-ndk with Android 15+ 16 KB ELF page-size alignment.
val buildRustCore by tasks.registering {
  group = "build"
  description = "Compiles rust/ crate via cargo-ndk for arm64-v8a with 16 KB page-size alignment."

  val rustDir = rootProject.file("rust")
  val jniLibsDir = file("src/main/jniLibs")
  val requireRust = project.findProperty("requireRust")?.toString()?.toBoolean() ?: false

  inputs.files(fileTree(rustDir) { include("Cargo.toml", "Cargo.lock", ".cargo/**", "src/**") })
  outputs.dir(jniLibsDir)

  doLast {
    val pathEnv = System.getenv("PATH") ?: ""
    val cargoBin = pathEnv.split(File.pathSeparator)
      .map { File(it, "cargo") }
      .firstOrNull { it.exists() && it.canExecute() }
    val cargoNdkBin = pathEnv.split(File.pathSeparator)
      .map { File(it, "cargo-ndk") }
      .firstOrNull { it.exists() && it.canExecute() }

    if (cargoBin == null || cargoNdkBin == null) {
      val msg = "Rust cargo/cargo-ndk not found on PATH; skipping native libprism_core.so compilation (Kotlin mirror fallback active). Pass -PrequireRust=true to enforce."
      if (requireRust) {
        throw GradleException(msg)
      } else {
        logger.warn(msg)
        return@doLast
      }
    }

    jniLibsDir.mkdirs()
    val process = ProcessBuilder(
      cargoBin.absolutePath,
      "ndk",
      "-t", "arm64-v8a",
      "-o", jniLibsDir.absolutePath,
      "build"
    )
      .directory(rustDir)
      .redirectErrorStream(true)
      .apply {
        val existingFlags = environment()["RUSTFLAGS"]?.takeIf { it.isNotBlank() }
        val pageAlignFlag = "-C link-arg=-Wl,-z,max-page-size=16384"
        environment()["RUSTFLAGS"] = listOfNotNull(existingFlags, pageAlignFlag).joinToString(" ")
      }
      .start()

    val output = process.inputStream.bufferedReader().readText()
    val exitCode = process.waitFor()
    if (exitCode != 0) {
      throw GradleException("cargo-ndk failed (exit $exitCode):\n$output")
    }
    logger.lifecycle("Successfully built libprism_core.so (arm64-v8a, 16 KB aligned).")
  }
}

tasks.named("preBuild") {
  dependsOn(buildRustCore)
}

// Unused dependencies remain commented out to minimize APK size and keep builds fast.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.webkit)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.okhttp)
  // implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  // implementation(libs.androidx.datastore.preferences)
  // implementation(libs.androidx.navigation.compose)
  // implementation(libs.androidx.room.ktx)
  // implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  // implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  // implementation(libs.firebase.firestore)
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  // implementation(libs.firebase.appcheck.recaptcha)
  // implementation(libs.firebase.appcheck.debug)
  // implementation(libs.logging.interceptor)
  // implementation(libs.moshi.kotlin)
  // implementation(libs.play.services.location)
  // implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
