package com.rogbooster.app.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Choreographer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.ServiceCompat
import com.rogbooster.app.R
import com.rogbooster.app.core.Prefs
import com.rogbooster.app.core.SystemMonitor
import com.rogbooster.app.ui.CrosshairView

/** Overlay permanente (SYSTEM_ALERT_WINDOW): FPS, temperatura, RAM y mira opcional. */
class FloatingWidgetService : Service() {

    private lateinit var wm: WindowManager
    private var widget: View? = null
    private var crosshair: CrosshairView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var frames = 0
    private var running = false

    private val frameCb = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            frames++
            if (running) Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private val ui = object : Runnable {
        override fun run() {
            val fps = frames; frames = 0
            val temp = SystemMonitor.batteryTempC(this@FloatingWidgetService)
            val ram = SystemMonitor.ram(this@FloatingWidgetService)
            widget?.let { w ->
                w.findViewById<TextView>(R.id.ovFps).text = "$fps FPS"
                w.findViewById<TextView>(R.id.ovRam).text = "RAM ${ram.freeMb} MB"
                val t = w.findViewById<TextView>(R.id.ovTemp)
                t.text = "%.1f °C".format(temp)
                t.setTextColor(
                    when (SystemMonitor.thermalLevel(temp)) {
                        SystemMonitor.Thermal.OK -> Color.parseColor("#2CE59A")
                        SystemMonitor.Thermal.WARM -> Color.parseColor("#FFB020")
                        else -> Color.parseColor("#FF2A4B")
                    }
                )
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        ServiceCompat.startForeground(
            this, 1002, Notifier.foreground(this, "Overlay de rendimiento activo"),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        if (widget == null) addWidget()
        syncCrosshair()
        if (!running) {
            running = true
            Choreographer.getInstance().postFrameCallback(frameCb)
            handler.post(ui)
        }
        return START_STICKY
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun addWidget() {
        val v = LayoutInflater.from(this).inflate(R.layout.overlay_widget, null)
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 24; y = 120 }

        var startX = 0; var startY = 0; var touchX = 0f; var touchY = 0f
        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { startX = lp.x; startY = lp.y; touchX = e.rawX; touchY = e.rawY }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = startX + (e.rawX - touchX).toInt()
                    lp.y = startY + (e.rawY - touchY).toInt()
                    wm.updateViewLayout(v, lp)
                }
            }
            true
        }
        wm.addView(v, lp)
        widget = v
    }

    private fun syncCrosshair() {
        val want = Prefs.bool(this, Prefs.CROSSHAIR, false)
        if (want && crosshair == null) {
            val size = (56 * resources.displayMetrics.density).toInt()
            val lp = WindowManager.LayoutParams(
                size, size, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.CENTER }
            crosshair = CrosshairView(this).also { wm.addView(it, lp) }
        } else if (!want && crosshair != null) {
            wm.removeView(crosshair); crosshair = null
        }
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacksAndMessages(null)
        Choreographer.getInstance().removeFrameCallback(frameCb)
        widget?.let { wm.removeView(it) }; widget = null
        crosshair?.let { wm.removeView(it) }; crosshair = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
