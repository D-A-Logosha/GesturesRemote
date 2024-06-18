package com.example.appclient.data.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager
import com.example.appclient.domain.GestureAccessibilityManager
import org.koin.core.annotation.Single

@Single
class AndroidAccessibilityManager(
    private val context: Context
) : GestureAccessibilityManager {

    private val accessibilityManager =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

    private var gestureAccessibilityService: GestureAccessibilityService? = null

    override fun performSwipe(
        startX: Float, startY: Float, endX: Float, endY: Float, duration: Long
    ) {
        val gestureService = getGestureAccessibilityService()
        gestureService?.performSwipe(startX, startY, endX, endY, duration)
    }

    override fun isServiceEnabled(): Boolean {
        val enabledServices =
            accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        gestureAccessibilityService = enabledServices.mapNotNull { it.resolveInfo.serviceInfo }
            .firstOrNull { it.name == GestureAccessibilityService::class.qualifiedName }
            ?.let { context.getSystemService(it.name) as? GestureAccessibilityService }
        return gestureAccessibilityService != null
    }

    private fun getGestureAccessibilityService(): GestureAccessibilityService? {
        if (gestureAccessibilityService == null) {
            isServiceEnabled()
        }
        return gestureAccessibilityService
    }
}
