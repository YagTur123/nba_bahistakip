# NBA Live Bets ProGuard Rules
-keep class com.yagsports.nbalivebets.** { *; }
-keep class retrofit2.** { *; }
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn com.google.gson.**
