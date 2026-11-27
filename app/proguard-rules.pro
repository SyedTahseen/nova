# Xray / gomobile bindings must keep their names for JNI.
-keep class libv2ray.** { *; }
-keep class go.** { *; }
-dontwarn libv2ray.**
-dontwarn go.**
