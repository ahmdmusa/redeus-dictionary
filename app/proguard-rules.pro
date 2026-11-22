# Keep line numbers for clean crash stack traces
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# Moshi reflection & KSP generated JsonAdapters
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class *JsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
    public <init>(com.squareup.moshi.Moshi, java.lang.reflect.Type[]);
}
-keep class com.example.api.** { *; }
-keep class com.example.data.** { *; }
-keep class com.example.domain.** { *; }

# Retrofit & OkHttp
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Open Spaced Repetition FSRS
-keep class io.github.openspacedrepetition.** { *; }
-dontwarn io.github.openspacedrepetition.**

# Launcher activity aliases & receivers
-keep class com.example.MainActivity { *; }
-keep class com.example.Launcher* { *; }
-keep class com.example.reminder.** { *; }

