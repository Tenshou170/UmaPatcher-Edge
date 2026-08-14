package dev.LeadRDRK.UmaPatcherEdge.patcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.content.IntentCompat
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PackageInstallerStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999)
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmationIntent = IntentCompat.getParcelableExtra(
                    intent,
                    Intent.EXTRA_INTENT,
                    Intent::class.java
                )
                if (confirmationIntent != null) {
                    context.startActivity(confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                val pending = contList.toList()
                contList.clear()
                pending.forEach { it.resume(true) }
            }
            else -> {
                val pending = contList.toList()
                contList.clear()
                pending.forEach { it.resume(false) }
            }
        }
    }

    companion object {
        val contList: MutableList<Continuation<Boolean>> = CopyOnWriteArrayList()

        suspend fun waitForInstallFinish(): Boolean {
            contList.clear()
            return suspendCoroutine { cont ->
                contList.add(cont)
            }
        }
    }
}
