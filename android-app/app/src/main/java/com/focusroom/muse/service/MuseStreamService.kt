package com.focusroom.muse.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.focusroom.muse.R
import com.focusroom.muse.attention.AttentionLevelCalculator
import com.focusroom.muse.model.AttentionResult
import com.focusroom.muse.muse.LibMuseDataSource
import com.focusroom.muse.network.HoloLensBroadcaster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MuseStreamService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var streamJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val host = intent?.getStringExtra(EXTRA_HOST) ?: DEFAULT_HOST
        val port = intent?.getIntExtra(EXTRA_PORT, DEFAULT_PORT) ?: DEFAULT_PORT
        val levelCount = intent?.getIntExtra(EXTRA_LEVEL_COUNT, DEFAULT_LEVEL_COUNT) ?: DEFAULT_LEVEL_COUNT

        startForeground(NOTIFICATION_ID, buildNotification())
        startStreaming(host, port, levelCount)

        return START_STICKY
    }

    override fun onDestroy() {
        streamJob?.cancel()
        super.onDestroy()
    }

    private fun startStreaming(host: String, port: Int, levelCount: Int) {
        streamJob?.cancel()

        val dataSource = LibMuseDataSource(this)
        val calculator = AttentionLevelCalculator(levelCount = levelCount)
        val broadcaster = HoloLensBroadcaster(host, port)

        streamJob = scope.launch {
            dataSource
                .openStream()
                .catch { it.printStackTrace() }
                .collect { sample ->
                    val result = calculator.process(sample)
                    sendToHoloLens(broadcaster, result)
                }
        }
    }

    private fun sendToHoloLens(broadcaster: HoloLensBroadcaster, result: AttentionResult) {
        scope.launch { broadcaster.send(result) }
    }

    private fun buildNotification(): Notification {
        val channelId = "muse_stream_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Muse streaming",
                NotificationManager.IMPORTANCE_LOW,
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.status_running))
            .setSmallIcon(R.drawable.ic_notification_overlay)
            .build()
    }

    companion object {
        const val EXTRA_HOST = "host"
        const val EXTRA_PORT = "port"
        const val EXTRA_LEVEL_COUNT = "levelCount"

        private const val DEFAULT_HOST = "192.168.0.10"
        private const val DEFAULT_PORT = 9010
        private const val DEFAULT_LEVEL_COUNT = 3
        private const val NOTIFICATION_ID = 42
    }
}
