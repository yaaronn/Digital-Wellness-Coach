package com.digitalwellness.coach.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import dagger.hilt.android.AndroidEntryPoint

/**
 * Optional accessibility service used for detecting the foreground app
 * and blocking apps that have exceeded their usage limit.
 *
 * Requires user to enable in Settings > Accessibility.
 */
@AndroidEntryPoint
class WellnessAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 500
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            // Future: check if packageName is in blocked list and show overlay
        }
    }

    override fun onInterrupt() { /* no-op */ }
}
