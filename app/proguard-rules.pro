-dontoptimize
-dontpreverify

-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions,AnnotationDefault

-keep class d.d.meshenger.** { *; }
-keepclassmembers class d.d.meshenger.** { *; }

-keepclasseswithmembernames class * { native <methods>; }

-keep class org.libsodium.jni.** { *; }
-dontwarn org.libsodium.jni.**

-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }
-dontwarn com.google.zxing.**
-dontwarn com.journeyapps.**

-keep class com.google.android.material.** { *; }
-keep class androidx.** { *; }
-dontwarn com.google.android.material.**
-dontwarn androidx.**

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class **$Companion { *; }
-dontwarn kotlin.**

-keep class d.d.meshenger.BuildConfig { *; }

# R8: keep all resource id classes
-keepclassmembers class **.R$* { public static <fields>; }
