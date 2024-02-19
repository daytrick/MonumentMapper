plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.monumentmapper"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.monumentmapper"
        minSdk = 26
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

    // Fix issues with META-INF/DEPENDENCIES when adding Apache Jena as a dependency: https://github.com/auth0/Auth0.Android/issues/598#issuecomment-1486738419
    packaging {
        resources.excludes.add("META-INF/ASL2.0")
        resources.excludes.add("META-INF/DEPENDENCIES")
        resources.excludes.add("META-INF/LICENSE")
        resources.excludes.add("META-INF/LICENSE.txt")
        resources.excludes.add("META-INF/license.txt")
        resources.excludes.add("META-INF/NOTICE")
        resources.excludes.add("META-INF/NOTICE.txt")
        resources.excludes.add("META-INF/notice.txt")
        resources.excludes.add("META-INF/*.kotlin_module")

        // Get rid of some duplicate class errors
        // From: https://github.com/sbrunk/jena-android/issues/1#issuecomment-321363532
        resources.pickFirsts.add("org/apache/**")
        resources.pickFirsts.add("etc/**")
        resources.pickFirsts.add("jena-log4j.properties")
        resources.pickFirsts.add("ont-policy.rdf")

    }

}

repositories {
    google()
    // MAVEN BELONGS HERE!!!

    // From: https://stackoverflow.com/a/72960966
    mavenCentral()
    maven { url = uri("https://jitpack.io") }

    mavenLocal()
}

dependencies {

    implementation("androidx.annotation:annotation:1.7.1")
    val navVersion = "2.5.3"   // most recent one won't work with older Android versions

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")
    implementation("com.github.pengrad:mapscaleview:1.6.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // MINE
    // How to import OSM from: https://medium.com/@mr.appbuilder/how-to-integrate-and-work-with-open-street-map-osm-in-an-android-app-kotlin-564b38590bfe
    implementation("org.osmdroid:osmdroid-android:6.1.14")
    // OSMBonus for the markers: https://github.com/MKergall/osmbonuspack/wiki/HowToInclude
    implementation("com.github.MKergall:osmbonuspack:6.9.0")
    // SVG loading: https://github.com/osmdroid/osmdroid/issues/1393#issuecomment-527144698
    implementation("com.caverock:androidsvg:1.4")

    // Apache Jena from: https://jena.apache.org/download/maven.html
    // implementation("org.apache.jena:apache-jena-libs:2.11.0")
    implementation("mobi.seus.jena:jena-android-arq:2.13.0") {

        // Duplicate class com.hp.hpl.jena.Jena
        // found in modules jetified-jena-android-core-2.13.0 (mobi.seus.jena:jena-android-core:2.13.0)
        // and jetified-jena-core-2.13.0 (org.apache.jena:jena-core:2.13.0)
        exclude("org.apache.jena", "jena-core")

        // Duplicate class org.apache.jena.iri.IRI
        // found in modules jetified-jena-android-iri-1.1.2 (mobi.seus.jena:jena-android-iri:1.1.2)
        // and jetified-jena-iri-1.1.2 (org.apache.jena:jena-iri:1.1.2)
        exclude("org.apache.jena", "jena-iri")

        // Duplicate class org.apache.commons.logging.Log
        // found in modules jetified-commons-logging-1.1 (commons-logging:commons-logging:1.1.1)
        // and jetified-jcl-over-slf4j-1.7.6 (org.slf4j:jcl-over-slf4j:1.7.6)
        exclude("commons-logging", "commons-logging")

        // Duplicate class org.apache.html.dom.CollectionIndex
        // found in modules jetified-xerces-android-2.11.0 (mobi.seus.jena:xerces-android:2.11.0)
        // and jetified-xercesImpl-2.11.0 (xerces:xercesImpl:2.11.0)
        exclude("xerces", "xercesImpl")

        // Duplicate class org.w3c.dom.ElementTraversal
        // found in modules jetified-xerces-android-2.11.0 (mobi.seus.jena:xerces-android:2.11.0)
        // and jetified-xml-apis-1.4.01 (xml-apis:xml-apis:1.4.01)
        exclude("xml-apis", "xml-apis")

    }


    // Solve NoSuchMethodError: org.apache.http.conn.ssl.SSLConnectionSocketFactory: https://stackoverflow.com/a/38233795
    //implementation("commons-logging:commons-logging:1.1.1:provided")
    //org.apache.http.impl.client.HttpClientBuilder

    //implementation("org.apache.http.impl.client:httpclient:4.5.2") {
//    implementation("org.apache.httpcomponents:httpclient:4.5.2") {
//        exclude("commons-logging")
//    }




    //    val kotlinVersion = "1.8.0"
//    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
//    constraints {
//        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion") {
//            because("kotlin-stdlib-jdk7 is now a part of kotlin-stdlib and to not fail builds due duplicate classes")
//        }
//        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion") {
//            because("kotlin-stdlib-jdk8 is now a part of kotlin-stdlib and to not fail builds due duplicate classes")
//        }
//    }



}