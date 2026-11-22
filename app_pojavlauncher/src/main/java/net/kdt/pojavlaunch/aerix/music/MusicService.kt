package net.kdt.pojavlaunch.aerix.music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import net.kdt.pojavlaunch.LauncherActivity
import net.kdt.pojavlaunch.R

/** Keeps the launcher alive while a song plays and shows the player in the notification shade. */
class MusicService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        LuciferMusic.onStateChanged = { refresh() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        if (LuciferMusic.current == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        when (intent?.action) {
            ACTION_TOGGLE -> LuciferMusic.toggle()
            ACTION_STOP -> LuciferMusic.stop(this)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        LuciferMusic.onStateChanged = null
        super.onDestroy()
    }

    private fun refresh() {
        if (LuciferMusic.current == null) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, build())
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Music", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun action(name: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this, requestCode,
            Intent(this, MusicService::class.java).setAction(name),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun build(): Notification {
        val track = LuciferMusic.current
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, LauncherActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.notif_icon)
            .setContentTitle(track?.title ?: "Music")
            .setContentText(track?.artist ?: "")
            .setContentIntent(open)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .addAction(
                0,
                if (LuciferMusic.playing) "Pause" else "Play",
                action(ACTION_TOGGLE, 1)
            )
            .addAction(0, "Stop", action(ACTION_STOP, 2))
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "lucifer_music"
        private const val NOTIFICATION_ID = 7421
        private const val ACTION_TOGGLE = "net.kdt.pojavlaunch.aerix.music.TOGGLE"
        private const val ACTION_STOP = "net.kdt.pojavlaunch.aerix.music.STOP"

        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, MusicService::class.java))
            }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, MusicService::class.java)) }
        }
    }
}
