# Meshenger release shrink rules — keep only what runtime needs

-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# App entry points (manifest components are auto-kept; reinforce package)
-keep class d.d.meshenger.** { *; }

# libsodium-jni (JNI + static natives)
-keep class org.libsodium.jni.** { *; }
-keepclassmembers class org.libsodium.jni.** {
    native <methods>;
    public *;
}
-dontwarn org.libsodium.jni.**

# WebRTC (heavy JNI; observers called from native)
-keep class org.webrtc.** { *; }
-keep class org.webrtc.audio.** { *; }
-keepclassmembers class org.webrtc.** {
    native <methods>;
    *;
}
-dontwarn org.webrtc.**
-dontwarn org.webrtc.audio.**

# ZXing (QR scan/show)
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }
-dontwarn com.google.zxing.**
-dontwarn com.journeyapps.**

# Serializable models
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Kotlin
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class **$Companion { *; }

# Material / AndroidX — rely on library consumer rules; strip unused via R8
-dontwarn com.google.android.material.**
-dontwarn androidx.**

# Optional: drop verbose logging in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
-assumenosideeffects class d.d.meshenger.Log {
    public static void d(...);
}
