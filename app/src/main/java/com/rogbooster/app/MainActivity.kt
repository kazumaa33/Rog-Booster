package com.rogbooster.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.rogbooster.app.core.*
import com.rogbooster.app.databinding.ActivityMainBinding
import com.rogbooster.app.model.GameApp
import com.rogbooster.app.service.FloatingWidgetService
import com.rogbooster.app.ui.GameAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var adapter: GameAdapter
    private val handler = Handler(Looper.getMainLooper())

    private val refresh = object : Runnable {
        override fun run() { updateStats(); handler.postDelayed(this, 1500) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        adapter = GameAdapter { launchGame(it) }
        b.rvGames.layoutManager = LinearLayoutManager(this)
        b.rvGames.adapter = adapter
        loadGames()
        setupPermissionButtons()
        setupControls()

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
    }

    override fun onResume() { super.onResume(); handler.post(refresh); updatePermissionLabels() }
    override fun onPause() { handler.removeCallbacks(refresh); super.onPause() }

    private fun loadGames() = Thread {
        val games = GameScanner.scan(this)
        runOnUiThread {
            adapter.submit(games)
            b.tvEmpty.visibility = if (games.isEmpty()) View.VISIBLE else View.GONE
        }
    }.start()

    private fun launchGame(g: GameApp) {
        if (!PermissionHelper.dnd(this) || !PermissionHelper.notifListener(this))
            Toast.makeText(this, "Faltan permisos de notificaciones: el bloqueo será parcial", Toast.LENGTH_LONG).show()
        Thread {
            val ok = OptimizationManager.startGame(this, g.packageName)
            if (!ok) runOnUiThread { Toast.makeText(this, "No se pudo abrir ${g.name}", Toast.LENGTH_SHORT).show() }
        }.start()
    }

    // ---- Estado en tiempo real ----
    private fun updateStats() {
        val ram = SystemMonitor.ram(this)
        b.tvRam.text = "RAM libre: ${ram.freeMb} MB de ${ram.totalMb} MB"
        b.progressRam.setProgressCompat(ram.usedPercent, true)

        val t = SystemMonitor.batteryTempC(this)
        b.tvTemp.text = if (t < 0) "Temperatura: --" else "Temperatura de batería: %.1f °C".format(t)
        val (label, color) = when (SystemMonitor.thermalLevel(t)) {
            SystemMonitor.Thermal.OK -> "● Estable" to R.color.rog_ok
            SystemMonitor.Thermal.WARM -> "● Tibio" to R.color.rog_warn
            SystemMonitor.Thermal.HOT -> "● Caliente: riesgo de throttling" to R.color.rog_red
            SystemMonitor.Thermal.CRITICAL -> "● Crítico: baja los gráficos" to R.color.rog_red
        }
        b.tvThermal.text = label
        b.tvThermal.setTextColor(ContextCompat.getColor(this, color))

        b.tvStatus.text = if (GameState.isGameRunning) "Modo juego ACTIVO · ${GameState.currentPackage}" else "Modo juego inactivo"
        b.btnStop.visibility = if (GameState.isGameRunning) View.VISIBLE else View.GONE
    }

    // ---- Permisos ----
    private fun setupPermissionButtons() {
        b.btnPermNotif.setOnClickListener { startActivity(PermissionHelper.intentNotifListener()) }
        b.btnPermDnd.setOnClickListener { startActivity(PermissionHelper.intentDnd()) }
        b.btnPermOverlay.setOnClickListener { startActivity(PermissionHelper.intentOverlay(this)) }
        b.btnPermWrite.setOnClickListener { startActivity(PermissionHelper.intentWriteSettings(this)) }
        b.btnPermUsage.setOnClickListener { startActivity(PermissionHelper.intentUsage()) }
    }

    private fun mark(ok: Boolean, text: String) = (if (ok) "✓ " else "✗ ") + text

    private fun updatePermissionLabels() {
        b.btnPermNotif.text = mark(PermissionHelper.notifListener(this), "Bloqueo de notificaciones (listener)")
        b.btnPermDnd.text = mark(PermissionHelper.dnd(this), "Modo No Molestar")
        b.btnPermOverlay.text = mark(PermissionHelper.overlay(this), "Mostrar sobre otras apps (overlay)")
        b.btnPermWrite.text = mark(PermissionHelper.writeSettings(this), "Modificar ajustes (brillo / puntero)")
        b.btnPermUsage.text = mark(PermissionHelper.usageAccess(this), "Acceso de uso (detectar cierre del juego)")
    }

    // ---- Controles ----
    private fun setupControls() {
        b.switchOverlay.isChecked = Prefs.bool(this, Prefs.OVERLAY, true)
        b.switchCrosshair.isChecked = Prefs.bool(this, Prefs.CROSSHAIR, false)
        b.switchBrightness.isChecked = Prefs.bool(this, Prefs.BRIGHT_LOCK, false)
        b.sliderBrightness.value = Prefs.int(this, Prefs.BRIGHT_LEVEL, 200).toFloat()
        b.switchPointer.isChecked = Prefs.bool(this, Prefs.POINTER_ON, false)
        b.sliderPointer.value = Prefs.int(this, Prefs.POINTER_LEVEL, 0).toFloat()
        b.tvPointerVal.text = "Nivel: ${b.sliderPointer.value.toInt()} (rango -7 a 7)"

        b.switchOverlay.setOnCheckedChangeListener { _, on ->
            Prefs.putBool(this, Prefs.OVERLAY, on)
            if (on && !PermissionHelper.overlay(this)) startActivity(PermissionHelper.intentOverlay(this))
        }
        b.switchCrosshair.setOnCheckedChangeListener { _, on ->
            Prefs.putBool(this, Prefs.CROSSHAIR, on)
            if (GameState.isGameRunning && PermissionHelper.overlay(this))
                ContextCompat.startForegroundService(this, Intent(this, FloatingWidgetService::class.java))
        }
        b.switchBrightness.setOnCheckedChangeListener { _, on ->
            Prefs.putBool(this, Prefs.BRIGHT_LOCK, on)
            if (on && !PermissionHelper.writeSettings(this)) startActivity(PermissionHelper.intentWriteSettings(this))
        }
        b.sliderBrightness.addOnChangeListener { _, v, _ -> Prefs.putInt(this, Prefs.BRIGHT_LEVEL, v.toInt()) }
        b.switchPointer.setOnCheckedChangeListener { _, on ->
            Prefs.putBool(this, Prefs.POINTER_ON, on)
            if (on && !PermissionHelper.writeSettings(this)) startActivity(PermissionHelper.intentWriteSettings(this))
        }
        b.sliderPointer.addOnChangeListener { _, v, _ ->
            Prefs.putInt(this, Prefs.POINTER_LEVEL, v.toInt())
            b.tvPointerVal.text = "Nivel: ${v.toInt()} (rango -7 a 7)"
        }

        b.btnClean.setOnClickListener {
            Thread {
                val mb = OptimizationManager.clearMemory(this, GameState.currentPackage)
                runOnUiThread { Toast.makeText(this, "RAM liberada: ~$mb MB", Toast.LENGTH_SHORT).show() }
            }.start()
        }
        b.btnStop.setOnClickListener { OptimizationManager.stopGame(this); updateStats() }
        b.btnDpi.setOnClickListener { showDpiDialog() }
    }

    /**
     * Android no permite a una app normal cambiar el DPI. Calculamos el valor y abrimos
     * Opciones de desarrollador ("Ancho más pequeño"); con ADB también se puede aplicar.
     */
    private fun showDpiDialog() {
        val current = DeveloperTools.currentSmallestWidthDp(this)
        val input = EditText(this).apply {
            hint = "Ancho más pequeño en dp (actual: $current)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY)
        }
        AlertDialog.Builder(this)
            .setTitle("Sensibilidad / DPI")
            .setMessage("Un valor menor = interfaz y controles más grandes. Escribe el ancho objetivo (p. ej. 360).")
            .setView(input)
            .setPositiveButton("Calcular y abrir") { _, _ ->
                val target = input.text.toString().toIntOrNull()?.coerceIn(240, 1000) ?: current
                val dpi = DeveloperTools.densityFor(this, target)
                AlertDialog.Builder(this)
                    .setTitle("Ancho más pequeño: $target dp")
                    .setMessage("Escríbelo en Opciones de desarrollador → «Ancho más pequeño».\n\nO por ADB:\nadb shell wm density $dpi\n\nPara revertir:\nadb shell wm density reset")
                    .setPositiveButton("Abrir opciones") { _, _ -> DeveloperTools.openDeveloperOptions(this) }
                    .setNegativeButton("Cerrar", null).show()
            }
            .setNegativeButton("Cancelar", null).show()
    }
}
