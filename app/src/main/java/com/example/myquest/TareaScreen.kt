package com.example.myquest

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ==========================================
// 1. MODELO DE DATOS
// ==========================================
data class TareaItem(
    val id: String = System.currentTimeMillis().toString(),
    val titulo: String,
    val categoria: String,          // "Otros", "Universidad", "Hogar", "Trabajo"
    val fechaLimite: String = "",   // Ej: "21/11/2026"
    val esDiaria: Boolean = false,
    val completada: Boolean = false,
    val fechaCompletada: String = "" // Ej: "2026-11-21" para control de medianoche
)

// ==========================================
// 2. PANTALLA PRINCIPAL DE TAREAS
// ==========================================
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TareaScreen() {
    val context = LocalContext.current

    // Lista reactiva cargada desde almacenamiento local y revisada con la regla de medianoche
    val listaTareas = remember {
        mutableStateListOf<TareaItem>().apply {
            addAll(cargarTareasLocal(context))
            aplicarReglaMedianoche(this)
        }
    }

    // Launcher para solicitar permiso de notificaciones en Android 13+ (API 33+)
    val launcherPermisoNotif = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* concedido o denegado */ }

    // Pide permiso si hace falta y guarda cambios de la regla de medianoche
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val tienePermiso = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!tienePermiso) {
                launcherPermisoNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        guardarTareasLocal(context, listaTareas)
    }

    // 4 secciones principales fijas
    val seccionesFijas = listOf("Otros", "Universidad", "Hogar", "Trabajo")

    // Estado de despliegue de cada sección fija
    val seccionesExpandidas = remember {
        mutableStateMapOf(
            "Otros" to true,
            "Universidad" to true,
            "Hogar" to false,
            "Trabajo" to false
        )
    }

    // Estados para crear una tarea nueva o editar una existente
    var mostrarDialogoCrear by remember { mutableStateOf(false) }
    var tareaEnEdicion by remember { mutableStateOf<TareaItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Tareas",
            style = MaterialTheme.typography.headlineMedium
        )

        // Botón fijo arriba a la izquierda para crear tareas
        Button(
            onClick = { mostrarDialogoCrear = true }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Nueva Misión")
        }

        Text(
            text = "Toca una sección para desplegar o cerrar sus tareas",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Contenedor desplazable con las 4 tarjetas fijas y sus subíndices
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            seccionesFijas.forEach { seccion ->
                val estaAbierta = seccionesExpandidas[seccion] ?: false

                // Filtramos por sección y colocamos las no completadas arriba y las hechas al final
                val tareasDeSeccion = listaTareas
                    .filter { it.categoria == seccion }
                    .sortedBy { it.completada }

                // Tarjeta fija de sección (peldaño)
                Card(
                    onClick = { seccionesExpandidas[seccion] = !estaAbierta },
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$seccion (${tareasDeSeccion.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (estaAbierta) "▲" else "▼",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Subíndices de tareas pertenecientes a la sección abierta
                if (estaAbierta) {
                    tareasDeSeccion.forEach { tarea ->
                        CardSubTarea(
                            tarea = tarea,
                            onEditar = {
                                tareaEnEdicion = tarea
                            },
                            onEliminar = {
                                // Punto 4.1: cancelar recordatorios al eliminar la tarea
                                cancelarRecordatoriosTarea(context, tarea.id)
                                listaTareas.removeAll { it.id == tarea.id }
                                guardarTareasLocal(context, listaTareas)
                            },
                            onToggleListo = {
                                val idx = listaTareas.indexOfFirst { it.id == tarea.id }
                                if (idx != -1) {
                                    val actual = listaTareas[idx]
                                    val ahoraCompletada = !actual.completada
                                    val tareaActualizada = actual.copy(
                                        completada = ahoraCompletada,
                                        fechaCompletada = if (ahoraCompletada) obtenerClaveDiaHoy() else ""
                                    )
                                    listaTareas[idx] = tareaActualizada
                                    guardarTareasLocal(context, listaTareas)

                                    // Punto 4.2: cancelar avisos si se marca como lista; reprogramar si se desmarca
                                    if (ahoraCompletada) {
                                        cancelarRecordatoriosTarea(context, tareaActualizada.id)
                                    } else {
                                        programarRecordatoriosTarea(context, tareaActualizada)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // ==========================================
    // 3. DIÁLOGO DE CREACIÓN / EDICIÓN DE TAREA
    // ==========================================
    val editando = tareaEnEdicion
    if (mostrarDialogoCrear || editando != null) {
        var textoTitulo by remember(editando) { mutableStateOf(editando?.titulo ?: "") }
        var categoriaSeleccionada by remember(editando) { mutableStateOf(editando?.categoria ?: "Otros") }
        var fechaSeleccionada by remember(editando) { mutableStateOf(editando?.fechaLimite ?: "") }
        var esDiaria by remember(editando) { mutableStateOf(editando?.esDiaria ?: false) }
        var mostrarDatePicker by remember { mutableStateOf(false) }

        val cerrarDialogo = {
            mostrarDialogoCrear = false
            tareaEnEdicion = null
        }

        AlertDialog(
            onDismissRequest = cerrarDialogo,
            title = {
                Text(text = if (editando != null) "Editar Tarea" else "Nueva Misión / Tarea")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Nombre de la tarea
                    OutlinedTextField(
                        value = textoTitulo,
                        onValueChange = { textoTitulo = it },
                        label = { Text("Nombre de la tarea") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Selección de sección fija
                    Text(
                        text = "Sección",
                        style = MaterialTheme.typography.labelMedium
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        seccionesFijas.forEach { seccion ->
                            FilterChip(
                                selected = categoriaSeleccionada == seccion,
                                onClick = { categoriaSeleccionada = seccion },
                                label = { Text(seccion) }
                            )
                        }
                    }

                    // Botón presionable para abrir el selector de fecha
                    Text(
                        text = "Fecha programada",
                        style = MaterialTheme.typography.labelMedium
                    )

                    OutlinedButton(
                        onClick = { mostrarDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (fechaSeleccionada.isBlank()) {
                                "📅 Elegir fecha"
                            } else {
                                "📅 $fechaSeleccionada"
                            }
                        )
                    }

                    // Interruptor para marcar si la tarea es diaria
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "¿Es diaria?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = esDiaria,
                            onCheckedChange = { esDiaria = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (textoTitulo.isNotBlank()) {
                            // Punto 4.3: guardar tarea nueva o editada y programar recordatorios (-7, -3, -1 días)
                            val tareaGuardada = if (editando != null) {
                                editando.copy(
                                    titulo = textoTitulo.trim(),
                                    categoria = categoriaSeleccionada,
                                    fechaLimite = fechaSeleccionada,
                                    esDiaria = esDiaria
                                ).also { actualizada ->
                                    val idx = listaTareas.indexOfFirst { it.id == editando.id }
                                    if (idx != -1) listaTareas[idx] = actualizada
                                }
                            } else {
                                TareaItem(
                                    titulo = textoTitulo.trim(),
                                    categoria = categoriaSeleccionada,
                                    fechaLimite = fechaSeleccionada,
                                    esDiaria = esDiaria
                                ).also { nueva ->
                                    listaTareas.add(nueva)
                                }
                            }

                            guardarTareasLocal(context, listaTareas)
                            programarRecordatoriosTarea(context, tareaGuardada)
                            seccionesExpandidas[categoriaSeleccionada] = true
                            cerrarDialogo()
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = cerrarDialogo) {
                    Text("Cancelar")
                }
            }
        )

        // Selector de fecha (calendario)
        if (mostrarDatePicker) {
            val datePickerState = rememberDatePickerState()

            DatePickerDialog(
                onDismissRequest = { mostrarDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
                                    timeZone = TimeZone.getTimeZone("UTC")
                                }
                                fechaSeleccionada = formato.format(Date(millis))
                            }
                            mostrarDatePicker = false
                        }
                    ) {
                        Text("Aceptar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarDatePicker = false }) {
                        Text("Cancelar")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

// ==========================================
// 4. TARJETA SUBÍNDICE DE TAREA
// ==========================================
@Composable
fun CardSubTarea(
    tarea: TareaItem,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onToggleListo: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp), // Indentación leve para efecto subíndice
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Información de la tarea (sin prioridad)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = tarea.titulo,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (tarea.completada) TextDecoration.LineThrough else TextDecoration.None
                )

                val partesMeta = buildList {
                    if (tarea.fechaLimite.isNotBlank()) add("📅 ${tarea.fechaLimite}")
                    if (tarea.esDiaria) add("🔁 Diaria")
                    if (tarea.completada) add("✅ Lista")
                }

                if (partesMeta.isNotEmpty()) {
                    Text(
                        text = partesMeta.joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 3 botoncitos compactos: Listo, Editar, Eliminar
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleListo,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Marcar como lista",
                        modifier = Modifier.size(18.dp),
                        tint = if (tarea.completada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onEditar,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar tarea",
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onEliminar,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar tarea",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// ==========================================
// 5. PERSISTENCIA Y REGLA DE MEDIANOCHE
// ==========================================
fun guardarTareasLocal(context: Context, lista: List<TareaItem>) {
    val prefs = context.getSharedPreferences("myquest_tareas_prefs", Context.MODE_PRIVATE)
    val jsonArray = JSONArray()

    lista.forEach { tarea ->
        val obj = JSONObject().apply {
            put("id", tarea.id)
            put("titulo", tarea.titulo)
            put("categoria", tarea.categoria)
            put("fechaLimite", tarea.fechaLimite)
            put("esDiaria", tarea.esDiaria)
            put("completada", tarea.completada)
            put("fechaCompletada", tarea.fechaCompletada)
        }
        jsonArray.put(obj)
    }

    prefs.edit().putString("lista_tareas_json", jsonArray.toString()).apply()
}

fun cargarTareasLocal(context: Context): List<TareaItem> {
    val prefs = context.getSharedPreferences("myquest_tareas_prefs", Context.MODE_PRIVATE)
    val jsonString = prefs.getString("lista_tareas_json", null) ?: return emptyList()

    val listaRecuperada = mutableListOf<TareaItem>()
    val jsonArray = JSONArray(jsonString)

    for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        listaRecuperada.add(
            TareaItem(
                id = obj.getString("id"),
                titulo = obj.getString("titulo"),
                categoria = obj.getString("categoria"),
                fechaLimite = obj.optString("fechaLimite", ""),
                esDiaria = obj.optBoolean("esDiaria", false),
                completada = obj.optBoolean("completada", false),
                fechaCompletada = obj.optString("fechaCompletada", "")
            )
        )
    }

    return listaRecuperada
}

// Devuelve la fecha actual como clave de día, ej: "2026-11-21"
fun obtenerClaveDiaHoy(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}

// Aplica la regla de las 12:00 AM al entrar si ya cambió el día respecto a fechaCompletada
fun aplicarReglaMedianoche(lista: MutableList<TareaItem>) {
    val hoy = obtenerClaveDiaHoy()
    val iterador = lista.iterator()

    while (iterador.hasNext()) {
        val tarea = iterador.next()
        if (tarea.completada && tarea.fechaCompletada.isNotBlank() && tarea.fechaCompletada != hoy) {
            if (tarea.esDiaria) {
                // Tarea diaria completada antes de hoy -> vuelve a pendiente
                val index = lista.indexOf(tarea)
                if (index != -1) {
                    lista[index] = tarea.copy(completada = false, fechaCompletada = "")
                }
            } else {
                // Tarea puntual completada antes de hoy -> se elimina sola
                iterador.remove()
            }
        }
    }
}