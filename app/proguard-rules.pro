# Project-specific ProGuard rules.
# Strip verbose/debug Timber calls from release builds if minification is enabled.
-assumenosideeffects class timber.log.Timber* {
    public static *** v(...);
    public static *** d(...);
}
