-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod

# Preserve IOC Lens domain models and kotlinx.serialization serializers
-keep class io.github.mrcoder20.ioclens.domain.model.** { *; }
-keep class io.github.mrcoder20.ioclens.domain.vision.** { *; }
-keepclassmembers class io.github.mrcoder20.ioclens.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# kotlinx.serialization
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class kotlinx.serialization.json.** { *; }
-dontwarn kotlinx.serialization.**
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# Ktor Client CIO & Coroutines
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn kotlinx.atomicfu.**
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
-dontwarn sun.misc.Unsafe

# ZXing & CameraX
-keep class com.google.zxing.** { *; }
-keep class androidx.camera.** { *; }