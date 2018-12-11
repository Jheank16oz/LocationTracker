package com.jheank16oz.locationtracker

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.util.Log


class MyForeGroundService : Service() {

    private val ID_SERVICE = 101
    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }



    override fun onCreate() {
        super.onCreate()

        // do stuff like register for BroadcastReceiver, etc.

        // Create the Foreground Service
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createNotificationChannel(notificationManager) else ""
        val notificationBuilder = NotificationCompat.Builder(this, channelId)


        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }



        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        val style = NotificationCompat.EXTRA_MEDIA_SESSION


        val notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_mtrl_chip_checked_circle)
                .setColor(resources.getColor(R.color.colorPrimary))
                .setContentTitle("Asistencia")
                .setContentText("Hay una asistencia e proceso")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentIntent(pendingIntent)
                .build()

        startForeground(ID_SERVICE, notification)
    }



    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager): String {
        val channelId = "my_service_channelid"
        val channelName = "My Foreground Service"
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        // omitted the LED color
        channel.importance = NotificationManager.IMPORTANCE_NONE
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        notificationManager.createNotificationChannel(channel)
        return channelId
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.action == STARTFOREGROUND_ACTION) {
            Log.i(TAG, "Received Start Foreground Intent ")
            // your start service code
        } else if (intent.action == STOPFOREGROUND_ACTION) {
            Log.i(TAG, "Received Stop Foreground Intent")
            //your end servce code
            stopForeground(true)
            stopSelf()
        }
        return Service.START_STICKY
    }

    companion object {
        const val STARTFOREGROUND_ACTION = "start"
        const val STOPFOREGROUND_ACTION = "stop"
        const val TAG  = "Service"

    }
}