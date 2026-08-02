-keepattributes *Annotation*, InnerClasses, Signature
-keepclassmembers class kotlinx.serialization.** { *; }
-keep,includedescriptorclasses class ru.tomilo.lib.mobile.**$$serializer { *; }
-keepclassmembers class ru.tomilo.lib.mobile.** {
    *** Companion;
}
-keepclasseswithmembers class ru.tomilo.lib.mobile.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
