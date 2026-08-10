# Keep kotlinx.serialization metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep generic signatures for Room
-keepattributes Signature

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
