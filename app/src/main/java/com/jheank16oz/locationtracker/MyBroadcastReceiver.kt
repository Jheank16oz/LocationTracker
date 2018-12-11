package com.jheank16oz.locationtracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

/**
 *
 *  <p>MyBroadcastReceiver</p>
 */
class MyBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SNOOZE = "cancel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        StringBuilder().apply {
            append("Action: ${intent.action}\n")
            append("URI: ${intent.toUri(Intent.URI_INTENT_SCHEME)}\n")
            toString().also { log ->
                Log.e("Broadcast ", log)
                Toast.makeText(context, log, Toast.LENGTH_LONG).show()
            }
        }
    }
}