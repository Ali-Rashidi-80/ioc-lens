# Desktop Minimalist Safety Rules (Ported from PortX for Windows JVM Stability)
# Disabling aggressive obfuscation, optimization, and shrinking to prevent Skiko/AWT JNI context nullptrs

-dontobfuscate
-dontoptimize
-dontshrink

# Keep all classes and members to guarantee 100% JVM/JNI/Skiko stability
-keep class io.github.mrcoder20.ioclens.** { *; }
-keep class ** { *; }

# Ignore all warnings to prevent build failures
-dontwarn **
