# ---------------------------------------------------------------------------
# Release obfuscation (R8) — harder to read DEX / reverse-engineer app logic
# Keep mapping.txt from app/build/outputs/mapping/release/mapping.txt for crashes
# ---------------------------------------------------------------------------

-repackageclasses 'o'
-allowaccessmodification
-overloadaggressively
-optimizationpasses 5
-adaptclassstrings
-renamesourcefileattribute SourceFile

# Strip verbose/debug logs from release builds (keep w/e for adb diagnostics)
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

-ignorewarnings
-dontnote

# ---------------------------------------------------------------------------
# Android components (manifest)
# ---------------------------------------------------------------------------
-keep public class * extends android.app.Activity {
    public <init>(...);
}
-keep public class * extends android.app.Service {
    public <init>(...);
}
-keep public class * extends android.content.BroadcastReceiver {
    public <init>(...);
}
-keep public class * extends android.app.Application {
    public <init>(...);
}

-keep public class * extends androidx.fragment.app.Fragment {
    public <init>(...);
}

-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# ---------------------------------------------------------------------------
# Gson (playlist / watchlist / recent JSON)
# ---------------------------------------------------------------------------
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations

-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keepclassmembers class com.chill.with.examsaathi.GitaChapterShlokas$Shloka { <fields>; }
-keepclassmembers class com.chill.with.examsaathi.GitaChapterShlokas$ShlokaDetailResponse { <fields>; }
-keepclassmembers class com.chill.with.examsaathi.LocalWatchlist$Entry { <fields>; }
-keepclassmembers class com.chill.with.examsaathi.LocalRecentViews$Entry { <fields>; }

# ---------------------------------------------------------------------------
# JSON parsing models (org.json field access)
# ---------------------------------------------------------------------------
-keepclassmembers class com.chill.with.examsaathi.GitaDashboard$** { <fields>; }
-keepclassmembers class com.chill.with.examsaathi.GitaChapters$** { <fields>; }
-keepclassmembers class com.chill.with.examsaathi.GitaChapterShlokas$** { <fields>; }

# Do NOT keep ApiConfig / StringVault — names and logic should be obfuscated

# ---------------------------------------------------------------------------
# Third-party (consumer rules from AARs also apply)
# ---------------------------------------------------------------------------
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-dontwarn okhttp3.**
-dontwarn androidx.media3.**

-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Firebase Realtime Database — anonymous GenericTypeIndicator subclasses break after R8 repackage
-keep class com.chill.with.examsaathi.FirebaseSnapshotHelper { *; }
-keep class com.chill.with.examsaathi.FirebaseSnapshotHelper$* { *; }
-keep class * extends com.google.firebase.database.GenericTypeIndicator { *; }

-keep class androidx.media3.common.Player$Listener {
    public *;
}
-keep class androidx.media3.exoplayer.ExoPlayer$Builder { *; }
