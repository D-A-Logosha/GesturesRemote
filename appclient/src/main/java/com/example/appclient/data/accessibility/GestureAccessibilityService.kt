package com.example.appclient.data.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GestureAccessibilityService : AccessibilityService() {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var swipeJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("Accessibility", "Accessibility: connected")

        findChromeNode()?.let {
            jobSwipes(it)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        Log.d("Accessibility", "Event: ${event.eventType}, package: ${event.packageName}")

        if (event.packageName?.toString() == "com.android.chrome") {
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> event.source?.let { jobSwipes(it) }
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> event.source?.let { jobSwipes(it) }
                else -> {}
            }
        }
    }

    override fun onInterrupt() {
        Log.d("Accessibility", "Service interrupted")
    }

    fun performSwipe(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long) {
        Log.d("Accessibility", "Gesture starting")
        val path = Path()
        path.moveTo(startX, startY)
        path.lineTo(endX, endY)

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, duration))
        val gestureDescription = gestureBuilder.build()

        dispatchGesture(gestureDescription, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                Log.d("Accessibility", "Gesture completed")
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                Log.e("Accessibility", "Gesture cancelled")
            }
        }, null)
    }

    private fun jobSwipes(nodeInfo: AccessibilityNodeInfo) {
        if (nodeInfo.isVisibleToUser) { //if (isChromeVisibleAndFocused(event))
            if (!nodeInfo.isFocused) {
                Log.d("Accessibility", "open Chrome")
                openChrome()
            }
            if ((swipeJob?.isActive != true)) {
                findChromeNode()?.let {
                    if (it.isFocused && it.isFocused) Log.d("Accessibility", "jobSwipes start")
                    startSwipes()
                }
            }
        } else swipeJob.run {
            Log.d("Accessibility", "jobSwipe stop")
            stopSwipes()
        }
    }

    private fun openChrome() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage("com.android.chrome")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            applicationContext.startActivity(intent)
        } catch (e: Exception) {
            Log.e("ClientViewModel", "Chrome not found", e)
        }
    }

    private fun startSwipes() {
        swipeJob = coroutineScope.launch(Dispatchers.IO) {
            val chromeBounds = Rect()
            var chromeNode : AccessibilityNodeInfo?
            var x: Float
            var h: Float
            while (isActive) {
                try {
                    delay(2222L)
                    chromeNode = findChromeNode(rootInActiveWindow)
                    if (chromeNode == null) this.cancel()
                    else {
                        chromeNode.getBoundsInScreen(chromeBounds)
                        Log.d(
                            "Accessibility",
                            "Accessibility: swipe up. Window size ${chromeBounds.width()}x${chromeBounds.height()}"
                        )
                        x = chromeBounds.centerX().toFloat()
                        h = chromeBounds.height().toFloat() / 3
                        performSwipe(
                            x, chromeBounds.bottom - h, x, chromeBounds.top + h, 555L
                        )
                    }
                    delay(2222L)
                    chromeNode = findChromeNode(rootInActiveWindow)
                    if (chromeNode == null) this.cancel()
                    else {
                        chromeNode.getBoundsInScreen(chromeBounds)
                        Log.d(
                            "Accessibility",
                            "Accessibility: swipe down. Window size ${chromeBounds.width()}x${chromeBounds.height()}"
                        )
                        x = chromeBounds.centerX().toFloat()
                        h = chromeBounds.height().toFloat() / 3
                        performSwipe(
                            x, chromeBounds.top + h, x, chromeBounds.bottom - h, 555L
                        )
                    }
                } catch (e: Exception) {
                    Log.e("Accessibility", "Error: ${e.message}", e)
                }
            }
        }
    }

    private fun stopSwipes() {
        swipeJob?.cancel()
        swipeJob = null
    }

    private fun isChromeVisibleAndFocused(event: AccessibilityEvent): Boolean {
        val sourceNode = event.source ?: return false
        if (event.packageName?.toString() != "com.android.chrome") return false
        return sourceNode.isVisibleToUser
    }

    private fun findChromeNode(rootNode: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (rootNode == null) return null
        if (rootNode.packageName?.toString() == "com.android.chrome") {
            return rootNode
        } else {
            for (i in 0 until rootNode.childCount) {
                findChromeNode(rootNode.getChild(i))?.let {
                    return it
                }
            }
        }
        return null
    }

    private fun findChromeNode(): AccessibilityNodeInfo? {
        val windows = windows
        for (window in windows) {
            findChromeNode(window.root)?.let {
                return it
            }
        }
        return null
    }
}
