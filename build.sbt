scalaVersion := "2.11.8"
name := "Sudoku Solver"

androidBuild
useSupportVectors

versionCode := Some(1)
version := "0.1-SNAPSHOT"

instrumentTestRunner :=
  "android.support.test.runner.AndroidJUnitRunner"


minSdkVersion := "21" // Android Lollipop 5.0
targetSdkVersion := "21"
platformTarget := "android-24"

javacOptions in Compile ++= "-source" :: "1.7" :: "-target" :: "1.7" :: Nil

libraryDependencies ++=
  "com.android.support" % "appcompat-v7" % "24.0.0" ::
    "com.android.support.test" % "runner" % "0.5" % "androidTest" ::
    "com.android.support.test.espresso" % "espresso-core" % "2.2.2" % "androidTest" ::
    "org.bytedeco" % "javacv" % "1.2" ::
    // "org.scalatest" %% "scalatest" % "3.0.0" % "test" ::
    Nil

classpathTypes += "maven-plugin"


// Override the run task with the android:run
run <<= run in Android

// Activate proguard for Scala
proguardScala in Android := true

// Activate proguard for Android
useProguard in Android := true

// Set proguard options
proguardOptions in Android ++= Seq(
  "-ignorewarnings",
  "-keep class scala.Dynamic")

javaCppPlatform := Seq("android-arm")

android.dsl.apkExclude(
  "META-INF/maven/org.bytedeco.javacpp-presets/opencv/pom.properties",
  "META-INF/maven/org.bytedeco.javacpp-presets/opencv/pom.xml",
  "META-INF/maven/org.bytedeco.javacpp-presets/videoinput/pom.properties",
  "META-INF/maven/org.bytedeco.javacpp-presets/videoinput/pom.xml",
  "META-INF/maven/org.bytedeco.javacpp-presets/ffmpeg/pom.properties",
  "META-INF/maven/org.bytedeco.javacpp-presets/ffmpeg/pom.xml"
)