package com.rogbooster.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

/** Mira personalizada (punto + cruz + anillo) para el overlay. */
class CrosshairView(ctx: Context) : View(ctx) {
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF2A4B"); strokeWidth = 3f; style = Paint.Style.STROKE
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF"); style = Paint.Style.FILL
    }

    override fun onDraw(c: Canvas) {
        val cx = width / 2f; val cy = height / 2f
        val r = width / 2f - 4f
        val gap = r * 0.25f
        c.drawCircle(cx, cy, r, line)
        c.drawLine(cx - r, cy, cx - gap, cy, line)
        c.drawLine(cx + gap, cy, cx + r, cy, line)
        c.drawLine(cx, cy - r, cx, cy - gap, line)
        c.drawLine(cx, cy + gap, cx, cy + r, line)
        c.drawCircle(cx, cy, 4f, dot)
    }
}
