# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class ar.trenar.app.**$$serializer { *; }
-keepclassmembers class ar.trenar.app.** {
    *** Companion;
}
-keep class ar.trenar.app.data.remote.dto.** { *; }
-keep class ar.trenar.app.data.model.** { *; }
