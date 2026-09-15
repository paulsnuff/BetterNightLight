# Disable obfuscation (keep original class, method, and field names)
-dontobfuscate

# Shizuku loads ShizukuPermissionWorker by class name in a separate process,
# and uses reflection to find its constructors. Keep the whole package.
-keep class io.github.threefreetree.betternightlight.shizuku.** { *; }

# commons-suncalc references FindBugs annotations that are only
# compile-time (provided) dependencies; safe to ignore at runtime.
-dontwarn edu.umd.cs.findbugs.annotations.Nullable