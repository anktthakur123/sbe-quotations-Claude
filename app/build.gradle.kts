plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

// Release workflows pass these properties from the Git tag (for example v1.0.3).
// The defaults keep Android Studio builds reproducible before the first GitHub release.
val appVersionNameValue = providers.gradleProperty("appVersionName").orElse("1.0.3").get()
val appVersionCodeValue = providers.gradleProperty("appVersionCode").orElse("1000003").get().toInt()
val releaseKeystorePath = System.getenv("SBE_KEYSTORE_FILE")
val releaseSigningEnabled = !releaseKeystorePath.isNullOrBlank()

android {
    namespace = "com.sidhbaba.quotemaker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sidhbaba.quotemaker"
        minSdk = 30 // Android 11+
        targetSdk = 34
        versionCode = appVersionCodeValue
        versionName = appVersionNameValue
    }

    signingConfigs {
        create("release") {
            // CI provides the keystore through environment variables. Never store it in Git.
            if (releaseSigningEnabled) {
                storeFile = file(releaseKeystorePath!!)
                storePassword = System.getenv("SBE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SBE_KEY_ALIAS")
                keyPassword = System.getenv("SBE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (releaseSigningEnabled) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    packaging {
        resources.excludes.add("META-INF/*")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.fragment:fragment-ktx:1.8.2")
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    // Room database (lightweight local storage for customers + quotation history)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // PDF generation matching the exact template
    implementation("com.itextpdf:itext7-core:7.2.5")
}
