# The Trusted Web Activity library finds some of its components by class name (ComponentName, manifest entries).
# Renaming them crashes the app on launch, so the library keeps its names.
-keep class com.google.androidbrowserhelper.** { *; }
