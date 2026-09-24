# Safe release rules — do not strip JNI, WebRTC, or app code

-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions
-renamesourcefileattribute SourceFile

# All app code (Activities, Service, crypto, call stack)
-keep class d.d.meshenger.** { *; }
-keepclassmembers class d.d.meshenger.** { *; }

# Any native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# libsodium-jni
-keep class org.libsodium.jni.** { *; }
-dontwarn org.libsodium.jni.**

# WebRTC (native callbacks)
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# ZXing
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }
-dontwarn com.google.zxing.**
-dontwarn com.journeyapps.**

# Material / AppCompat (themes resolve by name)
-keep class com.google.android.material.** { *; }
-keep class androidx.appcompat.** { *; }
-dontwarn com.google.android.material.**

# Enums used across IPC / state
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Kotlin
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class **$Companion { <methods>; <fields>; }
-dontwarn kotlin.**

# BuildConfig
-keep class d.d.meshenger.BuildConfig { *; }
