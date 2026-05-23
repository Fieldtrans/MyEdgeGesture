# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class com.example.myedgegesture.MainHook { *; }
-keep class com.example.myedgegesture.MainHook$* { *; }
-keep class com.example.myedgegesture.GestureConfig { *; }
-keep class com.example.myedgegesture.RuntimeGestureConfig { *; }
-keep class com.example.myedgegesture.OneHandPointer { *; }
-keep class com.example.myedgegesture.GestureActionDispatcher { *; }
-keep class com.example.myedgegesture.EdgeGestureDetector { *; }
-keep class com.example.myedgegesture.DeviceState { *; }
-keep class com.example.myedgegesture.DebugLog { *; }
-keep class com.example.myedgegesture.HookHealth { *; }
