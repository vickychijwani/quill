# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# for debugging ONLY
# -keepattributes LocalVariableTable,LocalVariableTypeTable

# WebView JS interface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# v7 appcompat
-keep public class android.support.v7.widget.** { *; }
-keep public class android.support.v7.internal.widget.** { *; }
-keep public class android.support.v7.internal.view.menu.** { *; }

-keep public class * extends android.support.v4.view.ActionProvider {
    public <init>(android.content.Context);
}

# v7 cardview
# http://stackoverflow.com/a/29698051/504611
-keep class android.support.v7.widget.RoundRectDrawable { *; }

# Otto
-keepattributes *Annotation*
-keepclassmembers class ** {
    @com.squareup.otto.Subscribe public *;
    @com.squareup.otto.Produce public *;
}

# Retrofit
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-dontwarn retrofit.**
-keep class retrofit.** { *; }

# Butterknife
-keepattributes *Annotation*
-keep class butterknife.** { *; }
-dontwarn butterknife.internal.**
-keep class **$$ViewBinder { *; }
-keepclasseswithmembernames class * {
    @butterknife.* <fields>;
}
-keepclasseswithmembernames class * {
    @butterknife.* <methods>;
}
-keepnames class * {
    @butterknife.Bind *;
}


# GSON
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes EnclosingMethod
-keep class sun.misc.Unsafe { *; }
#-keep class com.google.gson.stream.** { *; }
-keep class me.vickychijwani.spectre.model.** { *; }

# Crashlytics
-keep class com.crashlytics.** { *; }
-keepattributes SourceFile,LineNumberTable

# Realm
-keep @io.realm.annotations.RealmModule class *
-keep class io.realm.** { *; }
-keep class me.vickychijwani.spectre.model.** { *; }
-dontwarn javax.**
-dontwarn io.realm.**

# Retrolambda
-dontwarn java.lang.invoke.*

# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.squareup.okhttp.** { *; }
-keep interface com.squareup.okhttp.** { *; }
-dontwarn com.squareup.okhttp.**
-dontwarn java.nio.file.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# RxJava
-dontwarn rx.internal.util.**
-keep class rx.schedulers.Schedulers {
    public static <methods>;
}
-keep class rx.schedulers.ImmediateScheduler {
    public <methods>;
}
-keep class rx.schedulers.TestScheduler {
    public <methods>;
}
-keep class rx.schedulers.Schedulers {
    public static ** test();
}

# prettytime
-keep class org.ocpsoft.prettytime.i18n.**
-keepnames class ** implements org.ocpsoft.prettytime.TimeUnit
