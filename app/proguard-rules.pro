-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }

-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

-keep class androidx.lifecycle.** { *; }
-keep class androidx.navigation.** { *; }

-keep class androidx.camera.** { *; }

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Telephony constants used by SmsReader
-keep class android.provider.Telephony { *; }
-keep class android.provider.CallLog { *; }

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
