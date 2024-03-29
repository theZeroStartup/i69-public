//import Others.exoplayer
plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("dagger.hilt.android.plugin")
    id("com.onesignal.androidsdk.onesignal-gradle-plugin")
    id("com.apollographql.apollo3")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    kotlin("android")
    kotlin("kapt")
    id("androidx.navigation.safeargs.kotlin")
}

hilt {
    enableAggregatingTask = true
}

android {
    namespace = "com.i69"
    compileSdk = 34
    buildToolsVersion = "34.0.0"

    signingConfigs {
        create("release") {


            storeFile = file("C:\\Users\\theze\\StudioProjects\\i69\\jks\\i69sasu.jks")
//            storeFile = file("/Users/ashish/Downloads/I69-Kotlin-final/jks/i69sasu.jks")
            // storeFile = file("C:\\Users\\hp\\Downloads\\i69_keystore (10).jks")
            storePassword = "Trs@Yv*BJ46L"
            keyAlias = "i69sasu"
            keyPassword = "Trs@Yv*BJ46L"
        }
        getByName("debug") {


        }
    }

    defaultConfig {
        applicationId = Android.appId
        minSdk = Android.minSdk
        targetSdk = Android.targetSdk
        versionCode = Android.versionCode
        versionName = Android.versionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        signingConfig = signingConfigs.getByName("release")
        multiDexEnabled = true
    }

    buildTypes {
        release {
            /*isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )*/
            /* isMinifyEnabled=true
             isShrinkResources=true*/

            signingConfig = signingConfigs.getByName("release")
            isDebuggable = false

            /*buildConfigField("String", "BASE_URL", "\"https://api.i69app.com/\"")
            buildConfigField("String", "BASE_URL_REP", "\"http://95.216.208.1:8000/\"")
            buildConfigField(
                "String",
                "BASE_URL_WEB_SOCKET",
                "\"wss://api.chatadmin-mod.click/ws/graphql\""
            )*/
        }

        debug {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("release")
        }

        flavorDimensions += "default"
        productFlavors {

            create("staging") {
                // Assigns this product flavor to the "version" flavor dimension.
                // If you are using only one dimension, this property is optional,
                // and the plugin automatically assigns all the module's flavors to
                // that dimension.
                applicationId = Android.appId
                dimension = "default"
                //applicationIdSuffix = ".demo"
                //versionNameSuffix = "-demo"
                // /*test url*/
                       buildConfigField("String", "BASE_URL", "\"https://api.chatadmin-mod.click/\"")  //Staging
                       buildConfigField("String", "BASE_URL_REP", "\"http://95.216.208.1:8000/\"")
                       buildConfigField("String", "BASE_URL_WEB_SOCKET", "\"wss://api.chatadmin-mod.click/ws/graphql\"")
                buildConfigField("String", "MAPS_API_KEY", "\"AIzaSyBNDQFHOXjOH-AJH_tvgd7FM_IxLNClDRk\"")
                buildConfigField("boolean", "USE_S3", "true")
                buildConfigField("String", "APPLICATION_ID", "\"$applicationId\"")

             /*   buildConfigField("String ", "BASE_URL", "\"https://api.i69app.com\"")
                buildConfigField("String", "BASE_URL_REP", "\"http://95.216.208.1:8000/\"")
                buildConfigField(
                    "String",
                    "BASE_URL_WEB_SOCKET",
                    "\"wss://api.i69app.com/ws/graphql\""
                )*/

            }
            create("production") {
                applicationId = Android.appId
                dimension = "default"
                /* live url*/
                  /*    buildConfigField("String", "BASE_URL", "\"https://api.chatadmin-mod.click/\"")  //Staging
                      buildConfigField("String", "BASE_URL_REP", "\"http://95.216.208.1:8000/\"")
                      buildConfigField("String", "BASE_URL_WEB_SOCKET", "\"wss://api.chatadmin-mod.click/ws/graphql\"")
*/
                buildConfigField("String", "BASE_URL", "\"https://api.i69app.com/\"")
                buildConfigField("String", "BASE_URL_REP", "\"http://95.216.208.1:8000/\"")
                buildConfigField("String", "BASE_URL_WEB_SOCKET", "\"wss://api.i69app.com/ws/graphql\"")
                buildConfigField("String", "MAPS_API_KEY", "\"AIzaSyBNDQFHOXjOH-AJH_tvgd7FM_IxLNClDRk\"")
                buildConfigField("boolean", "USE_S3", "false")
                buildConfigField("String", "APPLICATION_ID", "\"$applicationId\"")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    viewBinding {
        enable = true
    }

    dataBinding {
        enable = true
    }

    packagingOptions {
        resources.excludes += "META-INF/library_release.kotlin_module"
    }

    kapt {
        this.correctErrorTypes = false
    }

    lint {
        //isAbortOnError = true
        abortOnError = false
        //isExplainIssues = true
        baseline = file("lint-baseline.xml")
    }

}

dependencies {


    // Kotlin
    implementation(Kotlin.coreKtx)
    //implementation(Kotlin.kotlinStdLib)
    implementation(Kotlin.coroutinesCore)
    implementation(Kotlin.coroutinesAndroid)
    implementation(Kotlin.coroutinesPlayServices)

    // UI
    implementation(UI.appCompat)
    implementation(UI.activity)
    implementation(UI.fragment)
    implementation(UI.constraintLayout)
    implementation(UI.material)
    implementation(UI.legacySupport)
    implementation(UI.recyclerView)
    implementation(UI.datastorePref)

    // Navigation
    implementation(UI.navigationFragment)
    implementation(UI.navigationRuntime)
    implementation(UI.navigationUI)

    // Lifecycle
    implementation(Lifecycle.runtime)
    implementation(Lifecycle.viewModel)
    implementation(Lifecycle.liveData)
    implementation(Lifecycle.common)
    implementation(Lifecycle.process)

    // Firebase
//    implementation(platform(Firebase.firebaseBom))
    implementation(Firebase.analytics)
    implementation(Firebase.crashlytics)
    implementation(Firebase.firebaseMessaging)

    // Login
    implementation(Login.google)
    implementation(Login.facebook)
    implementation(Login.facebookGraph)

    // Retrofit
    implementation(Others.retrofit)
    implementation(Others.gsonConverter)
    implementation(Others.scalarsCibverter)
    implementation(Others.loggingInterceptor)

    // Glide
    implementation(Others.glide)
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
//    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.5.1")
//    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")
    annotationProcessor(Others.glideCompiler)
    implementation(Others.glideAnnotation)
    implementation(Others.glideHttp)
//    {
//        exclude(group = "glide-parent")
//    }


    // Quickblox
    //implementation(Quickblox.messages)
    //implementation(Quickblox.chat)
    //implementation(Quickblox.content)

    // Lottie
    implementation(Others.lottie)

    // Timber
    implementation(Others.timber)

    // Hilt
    implementation(Hilt.android)
    kapt(Hilt.compiler)

    kapt("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.5.0")

    // Google Libraries
    implementation(Google.location)
    implementation(Google.billing)
    implementation(Google.playCore)

    // Expandable Layout
    implementation(Others.expandableLayout)

    // Chat UI
    implementation(project(":chatkit"))
//    implementation(Others.chatKit)
//    implementation("com.google.android.flexbox:flexbox:3.0.0")
//    implementation("com.github.stfalcon-studio:Chatkit:0.4.1") {
//        exclude("com.google.android:flexbox")
//    }

    implementation(Others.pixImagePicker)
    implementation(Others.carousel)
//    implementation(Others.permissions)

    // One Signal
    implementation(Others.oneSignal)

    // Worker
    implementation(Others.runtimeWorker)

    // Local Unit Tests
    testImplementation(Testing.junit)
    testImplementation(Testing.truth)
    testImplementation(Testing.archCore)
    testImplementation(HiltTest.hiltAndroidTesting)

    // Instrumentation Tests
    androidTestImplementation(Testing.junitAndroid)
    androidTestImplementation(Testing.espresso)
    androidTestImplementation(Testing.truth)
    androidTestImplementation(Testing.archCore)
    androidTestImplementation(HiltTest.hiltAndroidTesting)

    // Room
    implementation(Kotlin.room)
    implementation(Kotlin.roomCoroutines)
    kapt(Kotlin.roomCompiler)

//    implementation(exoplayer)
//    implementation(Others.exoplayer)
    implementation(Others.media3)
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")


    //Camera2
    implementation(Others.camera2)
    implementation(Others.cameraView)
    implementation(Others.cameraLifeCycle)
    implementation(Others.cameraCore)
    implementation(Others.cameraExtensions)
    implementation(Others.cameraVideo)

    //PayPal
    implementation(Others.paypal)

//    implementation("com.nshmura:recyclertablayout:1.5.0")
    implementation("com.github.nshmura:RecyclerTabLayout:v0.1.3")

    //Google Maps
    implementation(Others.googleMaps)

    // Kotlin + coroutines
    implementation("androidx.work:work-runtime-ktx:2.8.0")

    // The core runtime dependencies
    implementation("com.apollographql.apollo3:apollo-runtime:3.2.2")
    implementation("com.apollographql.apollo3:apollo-api:3.2.2")
    implementation("org.slf4j:slf4j-simple:2.0.6")

    //sdp
    implementation("com.intuit.sdp:sdp-android:1.1.0")
    implementation("com.makeramen:roundedimageview:2.3.0")
    //cropImage
    //implementation("com.github.takusemba:cropme:1.1.3")

    //implementation ("com.github.CanHub:Android-Image-Cropper:${version}")

    //circularprogressindicator
    implementation("com.github.jakob-grabner:Circle-Progress-View:1.4")

    //blurimage
    implementation("jp.wasabeef:blurry:4.0.1")
    implementation("jp.wasabeef:glide-transformations:4.3.0")

    implementation("com.github.skydoves:powermenu:2.2.2")
//    implementation("com.toptoche.searchablespinner:searchablespinnerlibrary:1.3.1")

//    debugImplementation("com.amitshekhar.android:debug-db:1.0.6")
    debugImplementation("com.github.amitshekhariitbhu:Android-Debug-Database:v1.0.6")

//    implementation ("com.stripe:stripe-java:19.45.0")


    implementation("com.stripe:stripe-android:20.19.0")
//    implementation("com.stripe:stripe-android:20.16.2")

    implementation("com.google.android.gms:play-services-auth:20.5.0")
//payment
    implementation("com.google.android.gms:play-services-wallet:19.2.0-beta01")
    implementation("com.google.android.gms:play-services-pay:16.1.0")
    implementation("com.facebook.shimmer:shimmer:0.5.0")

//    implementation("com.arthenica:ffmpeg-kit-min:4.5.1-1")

}

apollo {
    mapScalarToUpload("Upload")
    packageName.set("com.i69")
    generateKotlinModels.set(true)

}