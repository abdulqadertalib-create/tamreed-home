# Tamreed Home - Release ProGuard/R8 rules

# Keep Kotlin serialization
-keepattributes *Annotation*,InnerClasses,EnclosingMethod

# Keep Firebase Messaging service
-keep class iq.tamreed.home.TamreedFirebaseMessagingService { *; }

# Keep Supabase/Ktor serialization classes
-keep class kotlinx.serialization.** { *; }
-keep class io.ktor.** { *; }

# Keep application activities
-keep class iq.tamreed.home.MainActivity { *; }
-keep class iq.tamreed.home.NurseLoginActivity { *; }
-keep class iq.tamreed.home.AdminActivity { *; }
-keep class iq.tamreed.home.NurseActivity { *; }
-keep class iq.tamreed.home.MyBookingsActivity { *; }
-keep class iq.tamreed.home.NurseRequestsActivity { *; }
-keep class iq.tamreed.home.NurseSubscriptionActivity { *; }
-keep class iq.tamreed.home.ChatActivity { *; }
-keep class iq.tamreed.home.ProfileActivity { *; }
