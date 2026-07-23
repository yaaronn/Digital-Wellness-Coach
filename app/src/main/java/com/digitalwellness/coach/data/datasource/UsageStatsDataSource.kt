package com.digitalwellness.coach.data.datasource

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.util.Log
import com.digitalwellness.coach.domain.model.AppUsage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "UsageStats"

// ─── Packages that are never user-facing ─────────────────────────────────────
// Keep this list SHORT. Only pure background services with zero UI.
// Never add Chrome, YouTube, Twitter/X, Gmail, Maps — those are user apps.
private val EXCLUDED_PACKAGES = setOf(
    "android",
    "com.android.systemui",
    "com.android.launcher",
    "com.android.launcher3",
    "com.miui.home",                            // Xiaomi launcher
    "com.miui.securitycenter",
    "miui.systemui.plugin",
    "com.android.updater",                      // MIUI Updater (background)
    "com.google.android.permissioncontroller",
    "com.google.android.networkstack",
    "com.google.android.networkstack.tethering",
    "com.android.providers.downloads",
    "com.android.providers.media",
    "com.android.providers.contacts",
    "com.android.providers.settings",
    "com.android.phone",
    "com.android.bluetooth",
    "com.android.keychain",
    "com.android.inputmethod.latin",
    "com.google.android.inputmethod.latin",
    "com.samsung.android.honeyboard",
    "com.android.server.telecom",
)

@Singleton
class UsageStatsDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager = context.packageManager

    // ─── Permission check ─────────────────────────────────────────────────────

    fun hasUsagePermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    // ─── Primary: queryUsageStats (INTERVAL_BEST) ─────────────────────────────
    //
    // MIUI blocks many MOVE_TO_FOREGROUND/BACKGROUND events for user apps
    // (YouTube, Chrome, X, etc.) — so queryEvents() misses them entirely.
    //
    // queryUsageStats(INTERVAL_BEST) reads from Android's own aggregated
    // usage database which MIUI cannot suppress. It returns totalTimeInForeground
    // for every app used in the requested window — this is the same data
    // shown in Android's built-in Digital Wellbeing app.
    //
    // queryEvents() is used as a SUPPLEMENT to get open counts and fill in
    // apps that queryUsageStats might miss (rare).

    fun getAppUsagesForDay(dayStartMs: Long): List<AppUsage> {
        if (!hasUsagePermission()) {
            Log.w(TAG, "Usage permission not granted")
            return emptyList()
        }

        val dayEndMs = minOf(dayStartMs + 24 * 3_600_000L, System.currentTimeMillis())

        // ── Strategy A: queryUsageStats INTERVAL_BEST (primary, MIUI-safe) ────
        val statsMap    = mutableMapOf<String, Long>()  // pkg → foreground ms
        val lastUsedMap = mutableMapOf<String, Long>()  // pkg → last used ts

        try {
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST, dayStartMs, dayEndMs
            )
            stats?.forEach { stat ->
                if (stat.totalTimeInForeground > 0L) {
                    // Some devices double-report across intervals; take the max
                    val existing = statsMap[stat.packageName] ?: 0L
                    statsMap[stat.packageName] =
                        maxOf(existing, stat.totalTimeInForeground)
                    val existingLast = lastUsedMap[stat.packageName] ?: 0L
                    if (stat.lastTimeUsed > existingLast) {
                        lastUsedMap[stat.packageName] = stat.lastTimeUsed
                    }
                }
            }
            Log.d(TAG, "queryUsageStats returned ${statsMap.size} apps")
        } catch (e: Exception) {
            Log.e(TAG, "queryUsageStats failed", e)
        }

        // ── Strategy B: queryEvents for open counts (supplementary) ───────────
        val openCountMap  = mutableMapOf<String, Int>()
        val fgStartMap    = mutableMapOf<String, Long>()
        val eventUsageMap = mutableMapOf<String, Long>()

        try {
            val events = usageStatsManager.queryEvents(dayStartMs, dayEndMs)
            val event  = UsageEvents.Event()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName ?: continue
                if (pkg == context.packageName) continue

                when (event.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        fgStartMap[pkg]   = event.timeStamp
                        openCountMap[pkg] = (openCountMap[pkg] ?: 0) + 1
                        if ((lastUsedMap[pkg] ?: 0L) < event.timeStamp) {
                            lastUsedMap[pkg] = event.timeStamp
                        }
                    }
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        val start = fgStartMap.remove(pkg) ?: continue
                        val dur   = event.timeStamp - start
                        if (dur in 500L..43_200_000L) {
                            eventUsageMap[pkg] = (eventUsageMap[pkg] ?: 0L) + dur
                        }
                    }
                }
            }

            // Still-open apps
            val now = System.currentTimeMillis()
            fgStartMap.forEach { (pkg, startTs) ->
                val dur = now - startTs
                if (dur in 500L..43_200_000L) {
                    eventUsageMap[pkg] = (eventUsageMap[pkg] ?: 0L) + dur
                }
            }

            Log.d(TAG, "queryEvents captured ${eventUsageMap.size} apps")
        } catch (e: Exception) {
            Log.e(TAG, "queryEvents failed", e)
        }

        // ── Merge: statsMap is primary, eventUsageMap fills gaps ──────────────
        // For apps that queryEvents captured but queryUsageStats missed (rare on
        // stock Android), add the event-based time.
        eventUsageMap.forEach { (pkg, ms) ->
            if (!statsMap.containsKey(pkg) && ms >= 1_000L) {
                statsMap[pkg] = ms
                Log.d(TAG, "Event-only app added: $pkg ${ms / 1000}s")
            }
        }

        Log.d(TAG, "Merged total: ${statsMap.size} apps")

        // ── Build AppUsage objects ─────────────────────────────────────────────
        return statsMap
            .filter { (pkg, ms) ->
                ms >= 1_000L &&                  // ignore < 1 second
                pkg !in EXCLUDED_PACKAGES &&     // skip system services
                pkg != context.packageName       // skip self
            }
            .mapNotNull { (pkg, ms) ->
                // Extra check: skip pure system services without a launcher icon
                if (isSystemServiceWithNoUI(pkg)) return@mapNotNull null
                try {
                    val info    = packageManager.getApplicationInfo(pkg, 0)
                    val appName = packageManager.getApplicationLabel(info).toString()
                    val icon    = runCatching {
                        packageManager.getApplicationIcon(pkg)
                    }.getOrNull()
                    AppUsage(
                        packageName = pkg,
                        appName     = appName,
                        icon        = icon,
                        usageTimeMs = ms,
                        lastUsed    = lastUsedMap[pkg] ?: 0L,
                        openCount   = openCountMap[pkg] ?: 0
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }
            .sortedByDescending { it.usageTimeMs }
    }

    // ─── Today totals ─────────────────────────────────────────────────────────

    fun getTodayTotalScreenTime(): Long {
        if (!hasUsagePermission()) return 0L
        return getAppUsagesForDay(todayMidnight()).sumOf { it.usageTimeMs }
    }

    fun getTodayUnlockCount(): Int {
        if (!hasUsagePermission()) return 0
        val start = todayMidnight()
        val end   = System.currentTimeMillis()
        var unlocks     = 0
        var lastUnlockTs = 0L
        val DEBOUNCE    = 2_000L
        return try {
            val events = usageStatsManager.queryEvents(start, end)
            val event  = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) {
                    if (event.timeStamp - lastUnlockTs > DEBOUNCE) {
                        unlocks++
                        lastUnlockTs = event.timeStamp
                    }
                }
            }
            unlocks
        } catch (e: Exception) {
            Log.e(TAG, "getTodayUnlockCount failed", e)
            0
        }
    }

    fun getLongestContinuousSession(): Long {
        if (!hasUsagePermission()) return 0L
        val start = todayMidnight()
        val end   = System.currentTimeMillis()
        return try {
            val events = usageStatsManager.queryEvents(start, end)
            val event  = UsageEvents.Event()
            var sessionStart   = 0L
            var longestSession = 0L
            var screenOn       = false
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                when (event.eventType) {
                    UsageEvents.Event.SCREEN_INTERACTIVE -> {
                        sessionStart = event.timeStamp; screenOn = true
                    }
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                        if (screenOn && sessionStart > 0) {
                            val len = event.timeStamp - sessionStart
                            if (len > longestSession) longestSession = len
                        }
                        screenOn = false
                    }
                }
            }
            if (screenOn && sessionStart > 0) {
                val len = end - sessionStart
                if (len > longestSession) longestSession = len
            }
            longestSession
        } catch (e: Exception) {
            Log.e(TAG, "getLongestContinuousSession failed", e)
            0L
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Returns midnight (00:00:00.000) of today in epoch ms. */
    private fun todayMidnight(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Returns true only for system packages that have NO user-facing activity.
     * A system app WITH a launcher intent (e.g. Chrome, Play Store) returns false.
     */
    private fun isSystemServiceWithNoUI(packageName: String): Boolean {
        return try {
            val info       = packageManager.getApplicationInfo(packageName, 0)
            val isSystem   = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val hasLauncher = packageManager
                .getLaunchIntentForPackage(packageName) != null
            // Exclude ONLY system apps that have no launchable activity
            isSystem && !hasLauncher
        } catch (e: Exception) {
            false // If we can't determine, include it
        }
    }
}
