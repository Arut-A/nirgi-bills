# kotlinx-serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class ee.household.bills.**$$serializer { *; }
-keepclassmembers class ee.household.bills.** {
    *** Companion;
}
-keepclasseswithmembers class ee.household.bills.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# retrofit
-keepattributes Signature, Exceptions
-dontwarn okhttp3.**
-dontwarn retrofit2.**
