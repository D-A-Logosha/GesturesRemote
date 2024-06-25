package com.example.appclient.data.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.appclient.domain.interfaces.GestureServiceHandler
import com.example.common.domain.GestureData
import com.example.common.domain.GestureResult
import com.example.common.domain.PerformedGesture
import com.example.common.domain.SwipeArea
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.android.ext.android.inject
import org.koin.core.annotation.Single

@Single
class GestureAccessibilityService : AccessibilityService() {

    private val gestureServiceHandler: GestureServiceHandler by inject()

    private var isChromeVisibleToUser = false
    private var isChromeFocused = false
    private var chromeSwipeArea = SwipeArea()

    private val coroutineScope = CoroutineScope(SupervisorJob())
    private var swipeJob: Job? = null
    private var openChromeJob: Job? = null
    private var monitorChromeJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("Accessibility", "Accessibility: connected")

        findChromeNode()?.let { monitorChromeState(it) }
        gestureServiceHandler.onServiceState.update { true }
        openChromeJob = coroutineScope.launch(Dispatchers.IO) {
            gestureServiceHandler.openChrome.collect { _ ->
                gestureServiceHandler.openChromeResult.emit(openChrome())
            }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d("Accessibility", "Accessibility: disconnected")
        gestureServiceHandler.onServiceState.update { false }
        swipeJob?.cancel()
        swipeJob = null
        openChromeJob?.cancel()
        openChromeJob = null
        monitorChromeJob?.cancel()
        monitorChromeJob = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Log.d("Accessibility", "Event: ${event.eventType}, package: ${event.packageName}")

        if (event.packageName?.toString() == "com.android.chrome") {
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> event.source?.let {
                    monitorChromeState(
                        it
                    )
                }

                //AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> event.source?.let { monitorChromeState(it) }

                else -> {}
            }
        }
    }

    override fun onInterrupt() {
        Log.d("Accessibility", "Service interrupted")
    }

    private suspend fun performSwipe(gesture: GestureData) {
        Log.d("Accessibility", "Gesture starting: $gesture")
        val path = Path()
        path.moveTo(gesture.start.x.toFloat(), gesture.start.y.toFloat())
        path.lineTo(gesture.end.x.toFloat(), gesture.end.y.toFloat())

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, gesture.duration))
        val gestureDescription = gestureBuilder.build()
        val timestamp = System.currentTimeMillis()
        val callbackChannel = Channel<GestureResult>(capacity = 1)

        dispatchGesture(gestureDescription, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                coroutineScope.launch(Dispatchers.IO) {
                    callbackChannel.send(GestureResult.Completed)
                    gestureServiceHandler.onPerformedGestures.emit(
                        PerformedGesture(timestamp, gesture, GestureResult.Completed)
                    )
                }
                Log.d("Accessibility", "Gesture completed")
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                coroutineScope.launch(Dispatchers.IO) {
                    callbackChannel.send(GestureResult.Cancelled)
                    gestureServiceHandler.onPerformedGestures.emit(
                        PerformedGesture(timestamp, gesture, GestureResult.Completed)
                    )
                }
                Log.e("Accessibility", "Gesture cancelled")
            }
        }, null)

        coroutineScope.launch(Dispatchers.IO) {
            if (withTimeoutOrNull(gesture.duration + 999L) {
                    callbackChannel.receive()
                } == null) {
                Log.d("Accessibility", "Gesture timeout")
                gestureServiceHandler.onPerformedGestures.emit(
                    PerformedGesture(timestamp, gesture, GestureResult.TimeOut)
                )
            }
        }
    }


    private fun monitorChromeState(nodeInfo: AccessibilityNodeInfo) {
        if (monitorChromeJob != null) return
        monitorChromeJob = coroutineScope.launch(Dispatchers.IO) {
            while (isActive) {
                val chromeNode = findChromeNode()
                if (chromeNode != null) {
                    if (isChromeVisibleToUser != chromeNode.isVisibleToUser) {
                        isChromeVisibleToUser = chromeNode.isVisibleToUser
                        gestureServiceHandler.onChromeVisibleToUser.update { isChromeVisibleToUser }
                    }
                    val findIsChromeFocused = isChromeFocused()
                    if (isChromeFocused != findIsChromeFocused) {
                        isChromeFocused = findIsChromeFocused
                        gestureServiceHandler.onChromeFocused.update { findIsChromeFocused }
                    }
                    if (isChromeVisibleToUser || isChromeFocused) { //if (isChromeVisibleAndFocused(event))
                        val chromeBounds = Rect()
                        chromeNode.getBoundsInScreen(chromeBounds)
                        if (chromeSwipeArea != chromeBounds) {
                            chromeSwipeArea = chromeBounds
                            Log.d("Accessibility", "Chrome area changed: $chromeSwipeArea}")
                            gestureServiceHandler.onChromeSwipeArea.update { chromeSwipeArea }
                        }
                        if (isChromeVisibleToUser && isChromeFocused) {
                            if (swipeJob?.isActive != true) {
                                Log.d("Accessibility", "monitorChromeJob start")
                                startSwipes()
                            }
                        } else swipeJob?.run {
                            Log.d("Accessibility", "monitorChromeJob stop: chrome not visible")
                            stopSwipes()
                            monitorChromeJob?.cancel()
                            monitorChromeJob = null
                        }
                    }
                } else {
                    isChromeVisibleToUser = false
                    gestureServiceHandler.onChromeVisibleToUser.update { isChromeVisibleToUser }
                    isChromeFocused = false
                    gestureServiceHandler.onChromeFocused.update { isChromeFocused }
                    chromeSwipeArea = SwipeArea()
                    gestureServiceHandler.onChromeSwipeArea.update { chromeSwipeArea }
                    swipeJob?.run {
                        Log.d("Accessibility", "monitorChromeJob stop: chromeNode not found")
                        stopSwipes()
                        monitorChromeJob?.cancel()
                        monitorChromeJob = null
                    }
                }
                delay(255L)
            }
        }
    }

    private fun openChrome(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage("com.android.chrome")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            applicationContext.startActivity(intent)
            Log.d("ClientViewModel", "Open Chrome")
            return true
        } catch (e: Exception) {
            Log.e("ClientViewModel", "Chrome not found", e)
            return false
        }
    }

    private fun startSwipes() {
        swipeJob = coroutineScope.launch(Dispatchers.Main) {
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
                    return if (it.isVisibleToUser) {
                        if (it.isFocused) it
                        else null
                    } else null
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
