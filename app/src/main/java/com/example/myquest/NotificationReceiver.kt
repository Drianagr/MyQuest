package com.example.myquest

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val titulo = intent.getStringExtra("titulo") ?: "Recordatorio de Pago"
        val mensaje = intent.getStringExtra("mensaje") ?: "Tienes un gasto o deuda próxima a vencer."
        val notifId = intent.getIntExtra("notif_id", System.currentTimeMillis().toInt())

        val channelId = "myquest_deudas_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal de notificación para Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorios de Deudas y Gastos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para pagos y gastos fijos próximos a vencer"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Puedes cambiarlo por un ícono propio si lo deseas
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(notifId, builder.build())
    }
}