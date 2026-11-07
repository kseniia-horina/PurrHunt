# Room, Hilt and Compose ship their own consumer rules.
# Keep enum names used as persisted keys (stored in Room/DataStore by name).
-keepclassmembers enum app.purrhunt.domain.** { *; }
