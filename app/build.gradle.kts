plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id ("kotlin-kapt")
    id("kotlin-parcelize")
    id ("com.google.dagger.hilt.android")
    id ("com.google.gms.google-services")
    id ("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
}

    android {
    namespace = "com.iobits.photo_to_video_slides_maker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.moviemaker.imagestovideo.slowmotion"
        minSdk = 24
        targetSdk = 34
        versionCode = 19
        versionName = "1.1.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Applovin Mediation
            resValue ("string", "APP_LOVIN_MEDIUM_NATIVE", "YOUR_AD_UNIT_ID")
            resValue ("string", "APP_LOVIN_SMALL_NATIVE", "YOUR_AD_UNIT_ID")
            resValue ("string", "APP_LOVIN_INTERSTITIAL", "YOUR_AD_UNIT_ID")
            resValue ("string", "APP_LOVIN_BANNER", "YOUR_AD_UNIT_ID")

            //admob app id
            resValue ("string", "admob_app_id", "ca-app-pub-3940256099942544~3347511713")

            resValue ("string", "ADMOB_BANNER_V2", "ca-app-pub-3940256099942544/6300978111")
            resValue ("string", "ADMOD_OPEN_AD", "ca-app-pub-3940256099942544/9257395921")
            resValue ("string", "ADMOB_INTERSTITIAL_V2", "ca-app-pub-3940256099942544/1033173712")
            resValue ("string", "ADMOB_NATIVE_WITHOUT_MEDIA_V2", "ca-app-pub-3940256099942544/2247696110")
            resValue ("string", "ADMOB_NATIVE_WITH_MEDIA_V2", "ca-app-pub-3940256099942544/2247696110")
            resValue ("string", "ADMOB_REWARD_VIDEO", "ca-app-pub-3940256099942544/5224354917")
            resValue ("string", "ADMOB_REWARD_INTER", "ca-app-pub-3940256099942544/5354046379")
            resValue ("string", "ADMOB_BANNER_COLLAPSIBLE", "ca-app-pub-3940256099942544/2014213617")
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            //  multiDexEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Applovin Mediation
            resValue ("string", "APP_LOVIN_MEDIUM_NATIVE", "YOUR_AD_UNIT_ID")
            resValue ("string", "APP_LOVIN_SMALL_NATIVE", "YOUR_AD_UNIT_ID")
            resValue ("string", "APP_LOVIN_INTERSTITIAL", "")
            resValue ("string", "APP_LOVIN_BANNER", "YOUR_AD_UNIT_ID")

            //admob app id
            resValue ("string", "admob_app_id", "ca-app-pub-8481475782807886~1783522167")

            resValue ("string", "ADMOB_BANNER_V2", "ca-app-pub-8481475782807886/1709647776")
            resValue ("string", "ADMOD_OPEN_AD", "ca-app-pub-8481475782807886/6453394912")
            resValue ("string", "ADMOB_INTERSTITIAL_V2", "ca-app-pub-8481475782807886/7958048273")
            resValue ("string", "ADMOB_NATIVE_WITHOUT_MEDIA_V2", "ca-app-pub-8481475782807886/2705721594")
            resValue ("string", "ADMOB_NATIVE_WITH_MEDIA_V2", "ca-app-pub-8481475782807886/1392639928")
            resValue ("string", "ADMOB_REWARD_VIDEO", "ca-app-pub-8481475782807886/2385177760")
            resValue ("string", "ADMOB_REWARD_INTER", "")
            resValue ("string", "ADMOB_BANNER_COLLAPSIBLE", "ca-app-pub-8481475782807886/1709647776")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    kapt {
        generateStubs = true
        correctErrorTypes = true
    }
    buildFeatures{
        viewBinding=true
        dataBinding=true
        buildConfig =  true
    }
}

dependencies {
    implementation(project(":library"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.activity:activity:1.9.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")

    // Firebase
    // Import the BoM for the Firebase platform

    implementation (platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation ("com.google.firebase:firebase-analytics-ktx")
    implementation ("com.google.firebase:firebase-crashlytics")
    implementation ("com.google.firebase:firebase-auth")
    implementation ("com.google.android.gms:play-services-auth:21.2.0")
    implementation ("com.google.firebase:firebase-storage-ktx")
    implementation ("com.google.firebase:firebase-config-ktx")
    implementation("com.google.firebase:firebase-perf")

    // for minify issue
    implementation ("com.facebook.infer.annotation:infer-annotation:0.18.0")

    //kotlinx-coroutines
    val coroutinesVersion = "1.7.1"
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    //shimmer effect
    implementation ("com.facebook.shimmer:shimmer:0.5.0")

    //shimmer effect
    implementation ("com.facebook.shimmer:shimmer:0.5.0")

    //Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt ("com.google.dagger:hilt-compiler:2.51.1")
    kapt ("com.google.dagger:hilt-android-compiler:2.51.1")


    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    // ViewModel utilities for Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    // Lifecycles only (without ViewModel or LiveData)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    kapt("androidx.hilt:hilt-compiler:1.2.0")
    // viewModelScope:
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation ("com.intuit.sdp:sdp-android:1.1.1")
    implementation ("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation ("androidx.hilt:hilt-navigation-fragment:1.2.0")
    // alternately - if using Java8, use the following instead of lifecycle-compiler
    implementation("androidx.lifecycle:lifecycle-common-java8:2.8.6")
    // Saved state module for ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.8.6")


    val navVersion = "2.8.2"
    // Java language implementation
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")

    // Kotlin
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")

    // Feature module Support

    implementation("androidx.fragment:fragment-ktx:1.8.4")
    // Feature module Support
    // Testing Navigation
    androidTestImplementation("androidx.navigation:navigation-testing:$navVersion")

    // Jetpack Compose Integration
    implementation("androidx.navigation:navigation-compose:$navVersion" )

    //Glide
    implementation ("com.github.bumptech.glide:glide:4.16.0")

    implementation ("androidx.recyclerview:recyclerview:1.3.2")

    // BILLING LIBRARY
    implementation ("com.android.billingclient:billing:7.1.1")

    // Applovin
//    implementation ("com.applovin:applovin-sdk:+")
//    implementation ("com.applovin.mediation:google-adapter:+")
//    implementation ("com.applovin.mediation:facebook-adapter:+")

    implementation ("com.google.android.gms:play-services-ads-identifier:18.1.0")
    implementation ("com.google.android.gms:play-services-base:18.5.0")
    implementation ("com.squareup.picasso:picasso:2.71828")

    // admob ads
    implementation ("com.google.android.gms:play-services-ads:23.4.0")
    // ad consent
    implementation ("com.google.android.ump:user-messaging-platform:3.0.0")

    // life cycle for open ad
    val lifecycleVersionOpenApp = "2.8.6"
    implementation ("androidx.lifecycle:lifecycle-process:$lifecycleVersionOpenApp")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersionOpenApp")
    annotationProcessor ("androidx.lifecycle:lifecycle-compiler:$lifecycleVersionOpenApp")

    implementation ("com.airbnb.android:lottie:6.4.0")

    // google common library
    implementation( "com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava")
    implementation( "com.google.guava:guava:33.0.0-jre")
    implementation ("com.ernestoyaquello.dragdropswiperecyclerview:drag-drop-swipe-recyclerview:1.2.0")


    implementation ("androidx.media3:media3-exoplayer:1.4.1")
    implementation ("androidx.media3:media3-exoplayer-dash:1.4.1")
    implementation ("androidx.media3:media3-ui:1.4.1")

    /** Media 3 */
    implementation ("androidx.media3:media3-transformer:1.4.1")
    implementation ("androidx.media3:media3-effect:1.4.1")
    implementation ("androidx.media3:media3-common:1.4.1")

    //ffmpeg
    implementation ("com.arthenica:ffmpeg-kit-full:5.1")

    implementation ("com.amir-p:GradientSeekBar:1.0.0")

    //compressor utils
    implementation ("com.googlecode.mp4parser:isoparser:1.0.6")

    // for bidding
    implementation  ("com.google.ads.mediation:applovin:12.6.1.0")
    implementation  ("com.google.ads.mediation:facebook:6.18.0.0")
    implementation  ("com.google.ads.mediation:mintegral:16.8.61.0")
    implementation ("com.google.ads.mediation:vungle:7.4.1.0")
    implementation ("com.vungle:vungle-ads:7.4.1")

    implementation ("com.github.ome450901:SimpleRatingBar:1.5.1")
}    