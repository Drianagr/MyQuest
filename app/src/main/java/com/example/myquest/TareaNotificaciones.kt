package com.example.myquest

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.jar.Manifest


private const val CANAL_TAREAS_ID = "myquest_tareas_recordatorios"
private const val CANAL_TAREAS_NOMBRE = "Recordatorios de Tareas"

fun crearCanalNotificacionesTareas(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val canal = NotificationChannel(
            CANAL_TAREAS_ID,
            CANAL_TAREAS_NOMBRE,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos cuando faltan 7, 3 y 1 día para tareas de Universidad o Trabajo"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(canal)
    }
}

fun programarRecordatoriosTarea(context: Context, tarea: TareaItem) {
    crearCanalNotificacionesTareas(context)

    // Solo aplica para Universidad o Trabajo, sin completar y con fecha límite
    val categoriaValida = tarea.categoria == "Universidad" || tarea.categoria == "Trabajo"
    if (!categoriaValida || tarea.completada || tarea.fechaLimite.isBlank()) {
        cancelarRecordatoriosTarea(context, tarea.id)
        return
    }

    val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val fechaVencimiento = try {
        formatoFecha.parse(tarea.fechaLimite)
    } catch (_: Exception) {
        null
    } ?: return

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val diasAntes = listOf(7, 3, 1)

    diasAntes.forEach { dias ->
        val cal = Calendar.getInstance().apply {
            time = fechaVencimiento
            add(Calendar.DAY_OF_YEAR, -dias)
            set(Calendar.HOUR_OF_DAY, 9) // Hora del recordatorio: 9:00 AM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Si la fecha/hora calculada ya pasó, no programamos esa alarma
        if (cal.timeInMillis > System.currentTimeMillis()) {
            val textoDias = when (dias) {
                7 -> "Falta 1 semana (7 días)"
                3 -> "Faltan 3 días"
                else -> "Falta 1 día"
            }

            val intent = Intent(context, TareaNotificacionReceiver::class.java).apply {
                putExtra("notif_id", "${tarea.id}_$dias".hashCode())
                putExtra("titulo_tarea", tarea.titulo)
                putExtra("categoria_tarea", tarea.categoria)
                putExtra("mensaje_aviso", "$textoDias para: ${tarea.titulo} (${tarea.categoria})")
            }

            val requestCode = "${tarea.id}_$dias".hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                pendingIntent
            )
        }
    }
}

fun cancelarRecordatoriosTarea(context: Context, tareaId: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    listOf(7, 3, 1).forEach { dias ->
        val intent = Intent(context, TareaNotificacionReceiver::class.java)
        val requestCode = "${tareaId}_$dias".hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}

class TareaNotificacionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        crearCanalNotificacionesTareas(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permisoOk = ContextCompat.checkSelfPermission(
                context,
                "android.permission.POST_NOTIFICATIONS"
            ) == PackageManager.PERMISSION_GRANTED
            if (!permisoOk) return
        }
        val notifId = intent.getIntExtra("notif_id", System.currentTimeMillis().toInt())
        val categoria = intent.getStringExtra("categoria_tarea") ?: "Tarea"
        val mensaje = intent.getStringExtra("mensaje_aviso") ?: "Tienes una tarea próxima"

        val notificacion = NotificationCompat.Builder(context, CANAL_TAREAS_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Cámbialo por tu ícono de app si deseas
            .setContentTitle("MyQuest • $categoria")
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notifId, notificacion)
    }
}