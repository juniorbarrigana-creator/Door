package com.example.door.presentation

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If the Intent comes from WearMessageListenerService, try to unlock
        if (intent.getBooleanExtra("dismiss_keyguard", false)) {
            dismissKeyguard()
        } else {
            // If opened manually, just close it to maintain the "no interface" behavior
            finish()
        }
    }

    private fun sendConfirmationToWatch() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val nodeClient = Wearable.getNodeClient(this@MainActivity)
                    val messageClient = Wearable.getMessageClient(this@MainActivity)
                    val nodes = nodeClient.connectedNodes.await()
                    for (node in nodes) {
                        messageClient.sendMessage(node.id, "/unlock_done", "done".toByteArray()).await()
                    }
                } catch (_: Exception) {
                    // Fail silently
                }
            }
        }
    }

    private fun dismissKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    super.onDismissSucceeded()
                    sendConfirmationToWatch()
                    finish()
                }

                override fun onDismissCancelled() {
                    super.onDismissCancelled()
                    finish()
                }

                override fun onDismissError() {
                    super.onDismissError()
                    finish()
                }
            })
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
            // For older versions, add a small delay before closing to ensure flags work
            window.decorView.postDelayed({ finish() }, 500)
        }
    }
}
