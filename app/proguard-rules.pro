# Keep data models that are parsed from JSON
-keep class online.expensage.android.data.LastExpense { *; }
-keep class online.expensage.android.data.TodaySummary { *; }
-keep class online.expensage.android.data.SubmitExpenseResult { *; }
-keep class online.expensage.android.data.UserSettings { *; }
-keep class online.expensage.android.ui.SuccessState { *; }

# OkHttp and Retrofit (if used) often need these
-keepattributes Signature, InnerClasses, EnclosingMethod
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
# A resource is loaded at runtime for some compilers:
-keep class com.google.common.io.Resources { *; }

# Kotlinx Serialization (keep generated serializers)
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-dontwarn kotlinx.serialization.**
-keep,includedescriptorclasses class online.expensage.android.**$$serializer { *; }
-keepclassmembers class online.expensage.android.** {
    *** Companion;
}
-keepclasseswithmembers class online.expensage.android.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
