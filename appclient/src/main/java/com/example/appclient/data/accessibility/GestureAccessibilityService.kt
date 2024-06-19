package com.example.appclient.data.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.appclient.domain.GestureServiceHandler
import com.example.common.domain.GestureData
import com.example.common.domain.SwipeArea
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class GestureAccessibilityService : AccessibilityService() {

    private val gestureServiceHandler: GestureServiceHandler by inject()

    private var isChromeVisibleToUser = false
    private var isChromeFocused = false
    private var chromeSwipeArea = SwipeArea()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var swipeJob: Job? = null
    private var openChromeJob: Job? = null
    private var monitorChromeJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("Accessibility", "Accessibility: connected")

        findChromeNode()?.let { monitorChromeState(it) }
        gestureServiceHandler.onServiceStateChanged(true)
        openChromeJob = coroutineScope.launch(Dispatchers.IO) {
            gestureServiceHandler.openChrome.collect { _ ->
                openChrome()
            }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d("Accessibility", "Accessibility: disconnected")
        gestureServiceHandler.onServiceStateChanged(false)
        swipeJob?.cancel()
        swipeJob = null
        openChromeJob?.cancel()
        openChromeJob = null
        monitorChromeJob?.cancel()
        monitorChromeJob = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        //Log.d("Accessibility", "Event: ${event.eventType}, package: ${event.packageName}")

        if (event.packageName?.toString() == "com.android.chrome") {
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> event.source?.let { monitorChromeState(it) }

                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> event.source?.let { monitorChromeState(it) }

                else -> {}
            }
        }
    }

    override fun onInterrupt() {
        Log.d("Accessibility", "Service interrupted")
    }

    private fun performSwipe(gesture: GestureData) {
        Log.d("Accessibility", "Gesture starting")
        val path = Path()
        path.moveTo(gesture.start.x.toFloat(), gesture.start.y.toFloat())
        path.lineTo(gesture.end.x.toFloat(), gesture.end.y.toFloat())

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, gesture.duration))
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

    private fun monitorChromeState(nodeInfo: AccessibilityNodeInfo) {
        if (monitorChromeJob != null) return
        monitorChromeJob = coroutineScope.launch(Dispatchers.IO) {
            while (isActive) {
                val chromeNode = findChromeNode(rootInActiveWindow)
                if (chromeNode != null) {
                    if (isChromeVisibleToUser != chromeNode.isVisibleToUser) {
                        isChromeVisibleToUser = chromeNode.isVisibleToUser
                        gestureServiceHandler.onChromeVisibleToUserChanged(isChromeVisibleToUser)
                    }
                    val findIsChromeFocused = isChromeFocused()
                    if (isChromeFocused != findIsChromeFocused) {
                        isChromeFocused = findIsChromeFocused
                        gestureServiceHandler.onChromeFocusedChanged(findIsChromeFocused)
                    }
                    if (isChromeVisibleToUser || isChromeFocused) { //if (isChromeVisibleAndFocused(event))
                        val chromeBounds = Rect()
                        chromeNode.getBoundsInScreen(chromeBounds)
                        if (chromeSwipeArea != chromeBounds) {
                            chromeSwipeArea = chromeBounds
                            gestureServiceHandler.onSwipeAreaChanged(chromeSwipeArea)
                        }
                        if (isChromeVisibleToUser && isChromeFocused) {
                            if (swipeJob?.isActive != true) {
                                Log.d("Accessibility", "monitorChromeJob start")
                                startSwipes()
                            }
                        } else swipeJob?.run {
                            Log.d("Accessibility", "monitorChromeJob stop")
                            stopSwipes()
                            monitorChromeJob?.cancel()
                            monitorChromeJob = null
                        }
                    }
                } else {
                    isChromeVisibleToUser = false
                    gestureServiceHandler.onChromeVisibleToUserChanged(isChromeVisibleToUser)
                    isChromeFocused = false
                    gestureServiceHandler.onChromeFocusedChanged(isChromeFocused)
                    chromeSwipeArea = SwipeArea()
                    gestureServiceHandler.onSwipeAreaChanged(chromeSwipeArea)
                    swipeJob?.run {
                        Log.d("Accessibility", "monitorChromeJob stop")
                        stopSwipes()
                        monitorChromeJob?.cancel()
                        monitorChromeJob = null
                    }
                }
                delay(111L)
            }
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
            gestureServiceHandler.swipeCommand.collect { gesture ->
                performSwipe(gesture)
            }
        }
    }

    private fun stopSwipes() {
        swipeJob?.cancel()
        swipeJob = null
    }

    private fun isChromeFocused(): Boolean {
        return findChromeFocused(rootInActiveWindow) != null
    }

    private fun findChromeFocused(rootNode: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (rootNode == null) return null
        if (rootNode.packageName?.toString() == "com.android.chrome") {
            return rootNode
        } else {
            for (i in 0 until rootNode.childCount) {
                findChromeNode(rootNode.getChild(i))?.let {
                    return if (it.isFocused && it.isVisibleToUser) it
                    else null
                }
            }
        }
        return null
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
