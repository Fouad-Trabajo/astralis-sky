plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.navigation.safeargs.kotlin)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.fouadaha.astralis"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fouadaha.astralis"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.legacy.support.v4)

    //Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    //LiveData
    implementation(libs.androidx.lifecycle.livedata.ktx)
    //Navigation
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui)

    //Koin
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.annotations)
    implementation(libs.androidx.fragment.ktx)
    ksp(libs.koin.ksp)
    //Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter)
    implementation(libs.okhttp.log.interceptor)
    implementation(libs.gson.serializer)
    //Room
    implementation(libs.room.runtime)
    ksp(libs.room.ksp)
    implementation(libs.room.coroutines)
    //coil
    implementation(libs.coil)
    //Skeleton
    implementation (libs.skeletonlayout)
    //Kotlin Coroutines Test
    testImplementation(libs.kotlinx.coroutines.test)
    //Firebase
    implementation(project.dependencies.platform(libs.firebaseBom))
    //Firestore
    implementation(libs.firebase.firestore)



    //MockK
    testImplementation(libs.mockk)
    //JUnit
    testImplementation(libs.junit)
    testImplementation(libs.junit.jupiter)
}