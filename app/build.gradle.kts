import java.util.Properties

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("com.google.devtools.ksp")
}

// Optional local release signing. Create android/keystore.properties (not committed):
//   storeFile=.../release.keystore
//   storePassword=...
//   keyAlias=...
//   keyPassword=...
// When absent (e.g. F-Droid builds), release is left unsigned and the builder signs it.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
  if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { load(it) }
  }
}

android {
  namespace = "online.expensage.android"
  compileSdk = 35

  signingConfigs {
    if (keystorePropertiesFile.exists()) {
      create("release") {
        storeFile = file(keystoreProperties.getProperty("storeFile"))
        storePassword = keystoreProperties.getProperty("storePassword")
        keyAlias = keystoreProperties.getProperty("keyAlias")
        keyPassword = keystoreProperties.getProperty("keyPassword")
      }
    }
  }

  defaultConfig {
    applicationId = "online.expensage.android"
    minSdk = 26
    targetSdk = 35
    versionCode = 7
    versionName = "1.0.3"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables {
      useSupportLibrary = true
    }
    resourceConfigurations += setOf("en")
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
      if (keystorePropertiesFile.exists()) {
        signingConfig = signingConfigs.getByName("release")
      }
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlin {
    compilerOptions {
      jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
  }

  buildFeatures {
    compose = true
  }

  testOptions {
    unitTests.isReturnDefaultValues = true
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

dependencies {
  implementation(platform("androidx.compose:compose-bom:2025.02.00"))
  implementation("androidx.core:core-ktx:1.15.0")
  implementation("androidx.activity:activity-compose:1.10.1")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
  implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.material:material-icons-extended")
  implementation("androidx.browser:browser:1.8.0")
  implementation("androidx.work:work-runtime-ktx:2.11.2")
  implementation("com.google.android.material:material:1.13.0")
  implementation("androidx.security:security-crypto:1.1.0-alpha06")
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

  val roomVersion = "2.6.1"
  implementation("androidx.room:room-runtime:$roomVersion")
  implementation("androidx.room:room-ktx:$roomVersion")
  ksp("androidx.room:room-compiler:$roomVersion")

  debugImplementation("androidx.compose.ui:ui-tooling")

  testImplementation("junit:junit:4.13.2")
  testImplementation("org.mockito:mockito-core:5.11.0")
  testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
  testImplementation("org.robolectric:robolectric:4.12.1")
  testImplementation("androidx.test:core:1.6.1")
}
