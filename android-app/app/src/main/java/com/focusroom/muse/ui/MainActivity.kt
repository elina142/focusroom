package com.focusroom.muse.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.focusroom.muse.R
import com.focusroom.muse.service.MuseStreamService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val status = findViewById<TextView>(R.id.statusText)
        val start = findViewById<Button>(R.id.startButton)
        val stop = findViewById<Button>(R.id.stopButton)

        start.setOnClickListener {
            if (ensurePermissions()) {
                status.setText(R.string.status_running)
                startService()
            }
        }
        stop.setOnClickListener {
            status.setText(R.string.status_idle)
            stopService(Intent(this, MuseStreamService::class.java))
        }
    }

    private fun startService() {
        val intent = Intent(this, MuseStreamService::class.java).apply {
            putExtra(MuseStreamService.EXTRA_HOST, DEFAULT_HOST)
            putExtra(MuseStreamService.EXTRA_PORT, DEFAULT_PORT)
            putExtra(MuseStreamService.EXTRA_LEVEL_COUNT, DEFAULT_LEVELS)
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun ensurePermissions(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_CONNECT
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
            return false
        }
        return true
    }

    companion object {
        private const val DEFAULT_HOST = "192.168.0.10"
        private const val DEFAULT_PORT = 9010
        private const val DEFAULT_LEVELS = 3
    }
}
