plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.monumentmapper"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.monumentmapper"
        minSdk = 24
        targetSdk = 33
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }

}

dependencies {

    val nav_version = "2.5.3"   // most recent one won't work with older Android versions

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.navigation:navigation-fragment-ktx:$nav_version")
    implementation("androidx.navigation:navigation-ui-ktx:$nav_version")
    implementation("com.github.pengrad:mapscaleview:1.6.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // MINE
    // How to import OSM from: https://medium.com/@mr.appbuilder/how-to-integrate-and-work-with-open-street-map-osm-in-an-android-app-kotlin-564b38590bfe
    implementation("org.osmdroid:osmdroid-android:6.1.14")

    // WikiData import values from: https://github.com/Wikidata/Wikidata-Toolkit-Examples/blob/master/pom.xml
    val wikidataToolkitVersion = "0.14.4"
    implementation("org.wikidata.wdtk:wdtk-datamodel:${wikidataToolkitVersion}")
    implementation("org.wikidata.wdtk:wdtk-dumpfiles:${wikidataToolkitVersion}")
    implementation("org.wikidata.wdtk:wdtk-rtf:${wikidataToolkitVersion}")
    implementation("org.wikidata.wdtk:wdtk-wikibaseapi:${wikidataToolkitVersion}")
}