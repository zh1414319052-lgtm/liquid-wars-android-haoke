# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Applications/Utilities/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# These clases contain references to cordova webView
-keep class org.apache.cordova.** {*; }
-keep class org.apache.cordova.*

-keep class org.apache.cordova.** { *; }
-keep public class * extends org.apache.cordova.CordovaPlugin

# JNI exports use the original Java namespace even in the independent Haoke app ID.
-keep,allowoptimization class com.xenris.liquidwarsos.NativeInterface { *; }
# Android XML android:onClick handlers, including inherited LocalizedActivity.uiBack.
-keepclassmembers class com.xenris.liquidwarsos.activities.** {
    public void *(android.view.View);
}
# EdDSA receives an EdDSAPublicKey directly. The unreachable JDK-only X509Key
# fallback is absent on Android; keep this exception narrowly scoped.
-dontwarn sun.security.x509.X509Key
