# SadhanaLock blocker engine — ProGuard / R8 keep rules.
# CRITICAL: Google Play removes apps whose declared AccessibilityService class is
# stripped/renamed by R8 (the manifest <service android:name> must resolve). Keep the
# service, its receivers, and the Expo module entry point intact.

-keep class expo.modules.sadhanablocker.service.SadhanaAccessibilityService { *; }
-keep class expo.modules.sadhanablocker.service.BootReceiver { *; }
-keep class expo.modules.sadhanablocker.schedule.ScheduleAlarmReceiver { *; }
-keep class expo.modules.sadhanablocker.work.HealthCheckWorker { *; }
-keep class expo.modules.sadhanablocker.SadhanaBlockerModule { *; }

# AccessibilityService config + lifecycle callbacks reached reflectively by the system.
-keepclassmembers class * extends android.accessibilityservice.AccessibilityService {
    public <methods>;
}

# WorkManager instantiates workers by name.
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
