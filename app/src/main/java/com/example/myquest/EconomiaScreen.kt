package com.example.myquest

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// TIPOS DE MONEDA DISPONIBLES
enum class TipoMoneda(val etiqueta: String, val simbolo: String) {
    LOCAL("Local (Electrónico)", "Bs."),
    DIVISA("Divisa (Física)", "$"),
    USDT("USDT (Cripto)", "USDT")
}

// MODELO DE DATOS: Saldos por tipo de dinero
data class SaldosMultiMoneda(
    val local: Double = 0.0,
    val divisa: Double = 0.0,
    val usdt: Double = 0.0
)

// MODELO DE DATOS: Ahorro con 3 acumulados independientes
data class AhorroItem(
    val id: String,
    val nombre: String,
    val porcentaje: Float,
    val acumuladoLocal: Double = 0.0,
    val acumuladoDivisa: Double = 0.0,
    val acumuladoUsdt: Double = 0.0
)

// MODELO DE DATOS: Gasto / Deuda
data class GastoItem(
    val id: String,
    val nombre: String,
    val montoTotal: Double,
    val montoPagado: Double = 0.0,
    val esFijo: Boolean = false,
    val fechaPago: String = "",
    val moneda: TipoMoneda = TipoMoneda.DIVISA,
    val ultimoMesCobrado: Int = -1
)

// MODELO DE DATOS: Consumo
data class ConsumoItem(
    val id: String,
    val nombre: String,
    val monto: Double,
    val fuenteOrigen: String,
    val moneda: TipoMoneda = TipoMoneda.DIVISA,
    val timestamp: Long = System.currentTimeMillis()
)

// --- UTILIDAD DE ALMACENAMIENTO PERSISTENTE ---
object StorageUtil {
    private const val PREF_NAME = "myquest_prefs"
    private const val KEY_SALDOS_MULTI = "key_saldos_multi"
    private const val KEY_AHORROS = "key_ahorros"
    private const val KEY_GASTOS = "key_gastos"
    private const val KEY_CONSUMOS = "key_consumos"

    fun guardarSaldos(context: Context, saldos: SaldosMultiMoneda) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val obj = JSONObject()
        obj.put("local", saldos.local)
        obj.put("divisa", saldos.divisa)
        obj.put("usdt", saldos.usdt)
        prefs.edit().putString(KEY_SALDOS_MULTI, obj.toString()).apply()
    }

    fun cargarSaldos(context: Context): SaldosMultiMoneda {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_SALDOS_MULTI, null) ?: return SaldosMultiMoneda()
        return try {
            val obj = JSONObject(jsonString)
            SaldosMultiMoneda(
                local = obj.optDouble("local", 0.0),
                divisa = obj.optDouble("divisa", 0.0),
                usdt = obj.optDouble("usdt", 0.0)
            )
        } catch (_: Exception) {
            SaldosMultiMoneda()
        }
    }

    fun guardarAhorros(context: Context, lista: List<AhorroItem>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        for (item in lista) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("nombre", item.nombre)
            obj.put("porcentaje", item.porcentaje.toDouble())
            obj.put("acumuladoLocal", item.acumuladoLocal)
            obj.put("acumuladoDivisa", item.acumuladoDivisa)
            obj.put("acumuladoUsdt", item.acumuladoUsdt)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_AHORROS, jsonArray.toString()).apply()
    }

    fun cargarAhorros(context: Context): List<AhorroItem> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_AHORROS, null) ?: return emptyList()
        val lista = mutableListOf<AhorroItem>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                lista.add(
                    AhorroItem(
                        id = obj.getString("id"),
                        nombre = obj.getString("nombre"),
                        porcentaje = obj.getDouble("porcentaje").toFloat(),
                        acumuladoLocal = obj.optDouble("acumuladoLocal", 0.0),
                        acumuladoDivisa = obj.optDouble("acumuladoDivisa", 0.0),
                        acumuladoUsdt = obj.optDouble("acumuladoUsdt", 0.0)
                    )
                )
            }
        } catch (_: Exception) {}
        return lista
    }

    fun guardarGastos(context: Context, lista: List<GastoItem>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        for (item in lista) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("nombre", item.nombre)
            obj.put("montoTotal", item.montoTotal)
            obj.put("montoPagado", item.montoPagado)
            obj.put("esFijo", item.esFijo)
            obj.put("fechaPago", item.fechaPago)
            obj.put("moneda", item.moneda.name)
            obj.put("ultimoMesCobrado", item.ultimoMesCobrado)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_GASTOS, jsonArray.toString()).apply()
    }

    fun cargarGastos(context: Context): List<GastoItem> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_GASTOS, null) ?: return emptyList()
        val lista = mutableListOf<GastoItem>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val mStr = obj.optString("moneda", TipoMoneda.DIVISA.name)
                lista.add(
                    GastoItem(
                        id = obj.getString("id"),
                        nombre = obj.getString("nombre"),
                        montoTotal = obj.getDouble("montoTotal"),
                        montoPagado = obj.getDouble("montoPagado"),
                        esFijo = obj.optBoolean("esFijo", false),
                        fechaPago = obj.optString("fechaPago", ""),
                        moneda = try { TipoMoneda.valueOf(mStr) } catch (_: Exception) { TipoMoneda.DIVISA },
                        ultimoMesCobrado = obj.optInt("ultimoMesCobrado", -1)
                    )
                )
            }
        } catch (_: Exception) {}
        return lista
    }

    fun guardarConsumos(context: Context, lista: List<ConsumoItem>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        for (item in lista) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("nombre", item.nombre)
            obj.put("monto", item.monto)
            obj.put("fuenteOrigen", item.fuenteOrigen)
            obj.put("moneda", item.moneda.name)
            obj.put("timestamp", item.timestamp)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_CONSUMOS, jsonArray.toString()).apply()
    }

    fun cargarConsumos(context: Context): List<ConsumoItem> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_CONSUMOS, null) ?: return emptyList()
        val lista = mutableListOf<ConsumoItem>()
        val unMesEnMs = 30L * 24 * 60 * 60 * 1000
        val ahora = System.currentTimeMillis()

        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val mStr = obj.optString("moneda", TipoMoneda.DIVISA.name)
                val ts = obj.optLong("timestamp", ahora)

                if (ahora - ts <= unMesEnMs) {
                    lista.add(
                        ConsumoItem(
                            id = obj.getString("id"),
                            nombre = obj.getString("nombre"),
                            monto = obj.getDouble("monto"),
                            fuenteOrigen = obj.getString("fuenteOrigen"),
                            moneda = try { TipoMoneda.valueOf(mStr) } catch (_: Exception) { TipoMoneda.DIVISA },
                            timestamp = ts
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return lista
    }
}

@Composable
fun PantallaEconomia() {
    val context = LocalContext.current

    var textoIngresado by remember { mutableStateOf("") }
    var monedaIngresoSeleccionada by remember { mutableStateOf(TipoMoneda.LOCAL) }
    var menuMonedaIngresoExpandido by remember { mutableStateOf(false) }

    var saldos by remember { mutableStateOf(StorageUtil.cargarSaldos(context)) }

    val listaAhorros = remember { mutableStateListOf<AhorroItem>().apply { addAll(StorageUtil.cargarAhorros(context)) } }
    val listaGastos = remember { mutableStateListOf<GastoItem>().apply { addAll(StorageUtil.cargarGastos(context)) } }
    val listaConsumos = remember { mutableStateListOf<ConsumoItem>().apply { addAll(StorageUtil.cargarConsumos(context)) } }

    var pantallaAhorroExpandida by remember { mutableStateOf(false) }
    var pantallaGastoExpandida by remember { mutableStateOf(false) }
    var pantallaConsumoExpandida by remember { mutableStateOf(false) }

    var mostrarDialogoConvertidorGeneral by remember { mutableStateOf(false) }

    var mostrarDialogoAgregarAhorro by remember { mutableStateOf(false) }
    var nombreAhorroInput by remember { mutableStateOf("") }
    var porcentajeAhorroInput by remember { mutableStateOf("") }

    var mostrarDialogoAgregarGasto by remember { mutableStateOf(false) }
    var nombreGastoInput by remember { mutableStateOf("") }
    var montoGastoInput by remember { mutableStateOf("") }
    var esGastoFijoInput by remember { mutableStateOf(false) }
    var fechaPagoGastoInput by remember { mutableStateOf("") }
    var diaFijoSeleccionado by remember { mutableStateOf("1") }
    var monedaGastoInput by remember { mutableStateOf(TipoMoneda.DIVISA) }

    var mostrarDialogoAgregarConsumo by remember { mutableStateOf(false) }
    var nombreConsumoInput by remember { mutableStateOf("") }
    var montoConsumoInput by remember { mutableStateOf("") }
    var fuenteConsumoSeleccionada by remember { mutableStateOf("SALDO_DIVISA") }

    val totalGastosPendientes = listaGastos.sumOf { it.montoTotal - it.montoPagado }
    val totalConsumido = listaConsumos.sumOf { it.monto }

    fun guardarTodo() {
        StorageUtil.guardarSaldos(context, saldos)
        StorageUtil.guardarAhorros(context, listaAhorros)
        StorageUtil.guardarGastos(context, listaGastos)
        StorageUtil.guardarConsumos(context, listaConsumos)
    }

    LaunchedEffect(Unit) {
        val calendarioHoy = Calendar.getInstance()
        val diaHoy = calendarioHoy.get(Calendar.DAY_OF_MONTH)
        val mesHoy = calendarioHoy.get(Calendar.MONTH)

        var huboCambios = false
        for (i in listaGastos.indices) {
            val gasto = listaGastos[i]
            if (gasto.esFijo) {
                val diaCobro = gasto.fechaPago.toIntOrNull() ?: 1
                if (diaHoy >= diaCobro && gasto.ultimoMesCobrado != mesHoy) {
                    listaGastos[i] = gasto.copy(
                        montoPagado = 0.0,
                        ultimoMesCobrado = mesHoy
                    )
                    huboCambios = true
                }
            }
        }

        val unMesEnMs = 30L * 24 * 60 * 60 * 1000
        val ahora = System.currentTimeMillis()
        val consumosAntes = listaConsumos.size
        listaConsumos.removeAll { ahora - it.timestamp > unMesEnMs }

        if (huboCambios || consumosAntes != listaConsumos.size) {
            guardarTodo()
        }
    }

    if (pantallaAhorroExpandida) {
        VistaDetalleAhorros(
            listaAhorros = listaAhorros,
            saldos = saldos,
            onActualizarSaldos = { nuevosSaldos -> saldos = nuevosSaldos; guardarTodo() },
            onCerrar = { pantallaAhorroExpandida = false },
            onEliminar = { ahorro -> listaAhorros.remove(ahorro); guardarTodo() },
            onGuardarCambios = { guardarTodo() }
        )
        return
    }

    if (pantallaGastoExpandida) {
        VistaDetalleGastos(
            listaGastos = listaGastos,
            listaAhorros = listaAhorros,
            listaConsumos = listaConsumos,
            saldos = saldos,
            onActualizarSaldos = { nuevosSaldos -> saldos = nuevosSaldos; guardarTodo() },
            onCerrar = { pantallaGastoExpandida = false },
            onEliminar = { gasto -> listaGastos.remove(gasto); guardarTodo() },
            onGuardarCambios = { guardarTodo() }
        )
        return
    }

    if (pantallaConsumoExpandida) {
        VistaDetalleConsumos(
            listaConsumos = listaConsumos,
            onCerrar = { pantallaConsumoExpandida = false },
            onEliminar = { consumo -> listaConsumos.remove(consumo); guardarTodo() }
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Saldos Disponibles", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Local: ${TipoMoneda.LOCAL.simbolo}${saldos.local}")
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Divisa: ${TipoMoneda.DIVISA.simbolo}${saldos.divisa}")
                    Text(text = "USDT: ${saldos.usdt} USDT")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { mostrarDialogoConvertidorGeneral = true },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("🔄 Editar / Convertir")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = textoIngresado,
                onValueChange = { nuevoTexto -> textoIngresado = nuevoTexto },
                label = { Text("Monto del ingreso") },
                placeholder = { Text("Ej. 1000") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                modifier = Modifier.weight(1f)
            )

            Box {
                Button(onClick = { menuMonedaIngresoExpandido = true }) {
                    Text(monedaIngresoSeleccionada.simbolo)
                }
                DropdownMenu(
                    expanded = menuMonedaIngresoExpandido,
                    onDismissRequest = { menuMonedaIngresoExpandido = false }
                ) {
                    TipoMoneda.values().forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m.etiqueta) },
                            onClick = {
                                monedaIngresoSeleccionada = m
                                menuMonedaIngresoExpandido = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val ingresoBruto = textoIngresado.toDoubleOrNull() ?: 0.0
                if (ingresoBruto > 0) {
                    var totalAhorradoIngreso = 0.0

                    for (i in listaAhorros.indices) {
                        val ahorro = listaAhorros[i]
                        val montoParaEsteAhorro = ingresoBruto * (ahorro.porcentaje / 100.0)
                        totalAhorradoIngreso += montoParaEsteAhorro

                        listaAhorros[i] = when (monedaIngresoSeleccionada) {
                            TipoMoneda.LOCAL -> ahorro.copy(acumuladoLocal = ahorro.acumuladoLocal + montoParaEsteAhorro)
                            TipoMoneda.DIVISA -> ahorro.copy(acumuladoDivisa = ahorro.acumuladoDivisa + montoParaEsteAhorro)
                            TipoMoneda.USDT -> ahorro.copy(acumuladoUsdt = ahorro.acumuladoUsdt + montoParaEsteAhorro)
                        }
                    }

                    val restante = ingresoBruto - totalAhorradoIngreso
                    saldos = when (monedaIngresoSeleccionada) {
                        TipoMoneda.LOCAL -> saldos.copy(local = saldos.local + restante)
                        TipoMoneda.DIVISA -> saldos.copy(divisa = saldos.divisa + restante)
                        TipoMoneda.USDT -> saldos.copy(usdt = saldos.usdt + restante)
                    }

                    textoIngresado = ""
                    guardarTodo()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar Monto (${monedaIngresoSeleccionada.etiqueta})")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Tus Tarjetas / Categorías", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                TarjetaAhorroResumen(
                    listaAhorros = listaAhorros,
                    onClicTitulo = { pantallaAhorroExpandida = true },
                    onClicAgregar = { mostrarDialogoAgregarAhorro = true }
                )
            }
            item {
                TarjetaGastoResumen(
                    totalGastos = totalGastosPendientes,
                    listaGastos = listaGastos,
                    onClicTitulo = { pantallaGastoExpandida = true },
                    onClicAgregar = {
                        nombreGastoInput = ""
                        montoGastoInput = ""
                        esGastoFijoInput = false
                        fechaPagoGastoInput = ""
                        diaFijoSeleccionado = "1"
                        monedaGastoInput = TipoMoneda.DIVISA
                        mostrarDialogoAgregarGasto = true
                    }
                )
            }
            item {
                TarjetaConsumoResumen(
                    totalConsumido = totalConsumido,
                    listaConsumos = listaConsumos,
                    onClicTitulo = { pantallaConsumoExpandida = true },
                    onClicAgregar = {
                        fuenteConsumoSeleccionada = "SALDO_DIVISA"
                        mostrarDialogoAgregarConsumo = true
                    }
                )
            }
        }
    }

    // DIÁLOGO GENERAL DE EDICIÓN / INTERCAMBIO DIRECTO (Saldos)
    if (mostrarDialogoConvertidorGeneral) {
        DialogoEdicionSaldos(
            saldosActuales = saldos,
            onDismiss = { mostrarDialogoConvertidorGeneral = false },
            onGuardarCambiosDirectos = { nuevosSaldos ->
                saldos = nuevosSaldos
                guardarTodo()
                mostrarDialogoConvertidorGeneral = false
            },
            onConfirmarCambio = { origen, destino, montoSalida, montoEntrada ->
                var exito = false

                val saldosTemp = when (origen) {
                    TipoMoneda.LOCAL -> if (saldos.local >= montoSalida) saldos.copy(local = saldos.local - montoSalida) else null
                    TipoMoneda.DIVISA -> if (saldos.divisa >= montoSalida) saldos.copy(divisa = saldos.divisa - montoSalida) else null
                    TipoMoneda.USDT -> if (saldos.usdt >= montoSalida) saldos.copy(usdt = saldos.usdt - montoSalida) else null
                }

                if (saldosTemp != null) {
                    saldos = when (destino) {
                        TipoMoneda.LOCAL -> saldosTemp.copy(local = saldosTemp.local + montoEntrada)
                        TipoMoneda.DIVISA -> saldosTemp.copy(divisa = saldosTemp.divisa + montoEntrada)
                        TipoMoneda.USDT -> saldosTemp.copy(usdt = saldosTemp.usdt + montoEntrada)
                    }
                    guardarTodo()
                    exito = true
                }

                if (!exito) {
                    Toast.makeText(context, "Saldo insuficiente en la moneda de origen", Toast.LENGTH_SHORT).show()
                }
                mostrarDialogoConvertidorGeneral = false
            }
        )
    }

    if (mostrarDialogoAgregarAhorro) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoAgregarAhorro = false },
            title = { Text("Crear Nuevo Ahorro") },
            text = {
                Column {
                    OutlinedTextField(value = nombreAhorroInput, onValueChange = { nombreAhorroInput = it }, label = { Text("Nombre") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = porcentajeAhorroInput, onValueChange = { porcentajeAhorroInput = it }, label = { Text("Porcentaje (%)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val p = porcentajeAhorroInput.toFloatOrNull() ?: 0f
                    if (nombreAhorroInput.isNotBlank()) {
                        listaAhorros.add(AhorroItem(System.currentTimeMillis().toString(), nombreAhorroInput, p))
                        nombreAhorroInput = ""
                        porcentajeAhorroInput = ""
                        mostrarDialogoAgregarAhorro = false
                        guardarTodo()
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogoAgregarAhorro = false }) { Text("Cancelar") } }
        )
    }

    if (mostrarDialogoAgregarGasto) {
        val calendario = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val mesFormateado = String.format("%02d", month + 1)
                val diaFormateado = String.format("%02d", dayOfMonth)
                fechaPagoGastoInput = "$year-$mesFormateado-$diaFormateado"
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        )

        AlertDialog(
            onDismissRequest = { mostrarDialogoAgregarGasto = false },
            title = { Text("Crear Nuevo Gasto / Deuda") },
            text = {
                Column {
                    OutlinedTextField(value = nombreGastoInput, onValueChange = { nombreGastoInput = it }, label = { Text("Nombre") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = montoGastoInput, onValueChange = { montoGastoInput = it }, label = { Text("Monto Total") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Tipo de gasto:", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !esGastoFijoInput, onClick = { esGastoFijoInput = false })
                        Text(text = "Deuda (Puntual)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = esGastoFijoInput, onClick = { esGastoFijoInput = true })
                        Text(text = "Gasto Fijo (Recurrente mensual)")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (esGastoFijoInput) {
                        Text(text = "Día del mes para pago recurrente:", style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value = diaFijoSeleccionado,
                            onValueChange = { nuevoDia ->
                                val num = nuevoDia.toIntOrNull()
                                if (nuevoDia.isEmpty() || (num != null && num in 1..31)) {
                                    diaFijoSeleccionado = nuevoDia
                                }
                            },
                            label = { Text("Día (1 - 31)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { datePickerDialog.show() }
                        ) {
                            OutlinedTextField(
                                value = fechaPagoGastoInput,
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                label = { Text("Fecha límite (Toca para elegir)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Moneda:", style = MaterialTheme.typography.bodyMedium)
                    TipoMoneda.values().forEach { m ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = monedaGastoInput == m, onClick = { monedaGastoInput = m })
                            Text(text = m.etiqueta)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val m = montoGastoInput.toDoubleOrNull() ?: 0.0
                    val fechaDefinitiva = if (esGastoFijoInput) diaFijoSeleccionado else fechaPagoGastoInput

                    if (nombreGastoInput.isNotBlank() && m > 0 && fechaDefinitiva.isNotBlank()) {
                        listaGastos.add(
                            GastoItem(
                                id = System.currentTimeMillis().toString(),
                                nombre = nombreGastoInput,
                                montoTotal = m,
                                montoPagado = 0.0,
                                esFijo = esGastoFijoInput,
                                fechaPago = fechaDefinitiva,
                                moneda = monedaGastoInput
                            )
                        )
                        mostrarDialogoAgregarGasto = false
                        guardarTodo()
                    } else {
                        Toast.makeText(context, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogoAgregarGasto = false }) { Text("Cancelar") } }
        )
    }

    if (mostrarDialogoAgregarConsumo) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoAgregarConsumo = false },
            title = { Text("Registrar Consumo Manual") },
            text = {
                Column {
                    OutlinedTextField(value = nombreConsumoInput, onValueChange = { nombreConsumoInput = it }, label = { Text("Nombre del consumo") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = montoConsumoInput, onValueChange = { montoConsumoInput = it }, label = { Text("Monto") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Sacar dinero de:", style = MaterialTheme.typography.bodyMedium)

                    TextButton(onClick = { fuenteConsumoSeleccionada = "SALDO_LOCAL" }) { Text(text = if (fuenteConsumoSeleccionada == "SALDO_LOCAL") "✔ Saldo Local (${TipoMoneda.LOCAL.simbolo}${saldos.local})" else "Saldo Local") }
                    TextButton(onClick = { fuenteConsumoSeleccionada = "SALDO_DIVISA" }) { Text(text = if (fuenteConsumoSeleccionada == "SALDO_DIVISA") "✔ Saldo Divisa (${TipoMoneda.DIVISA.simbolo}${saldos.divisa})" else "Saldo Divisa") }
                    TextButton(onClick = { fuenteConsumoSeleccionada = "SALDO_USDT" }) { Text(text = if (fuenteConsumoSeleccionada == "SALDO_USDT") "✔ Saldo USDT (${saldos.usdt} USDT)" else "Saldo USDT") }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val montoC = montoConsumoInput.toDoubleOrNull() ?: 0.0
                    if (nombreConsumoInput.isNotBlank() && montoC > 0) {
                        var transaccionValida = false
                        var monedaConsumo = TipoMoneda.DIVISA

                        when (fuenteConsumoSeleccionada) {
                            "SALDO_LOCAL" -> {
                                if (saldos.local >= montoC) {
                                    saldos = saldos.copy(local = saldos.local - montoC)
                                    transaccionValida = true
                                    monedaConsumo = TipoMoneda.LOCAL
                                } else Toast.makeText(context, "Saldo local insuficiente", Toast.LENGTH_SHORT).show()
                            }
                            "SALDO_DIVISA" -> {
                                if (saldos.divisa >= montoC) {
                                    saldos = saldos.copy(divisa = saldos.divisa - montoC)
                                    transaccionValida = true
                                    monedaConsumo = TipoMoneda.DIVISA
                                } else Toast.makeText(context, "Saldo divisa insuficiente", Toast.LENGTH_SHORT).show()
                            }
                            "SALDO_USDT" -> {
                                if (saldos.usdt >= montoC) {
                                    saldos = saldos.copy(usdt = saldos.usdt - montoC)
                                    transaccionValida = true
                                    monedaConsumo = TipoMoneda.USDT
                                } else Toast.makeText(context, "Saldo USDT insuficiente", Toast.LENGTH_SHORT).show()
                            }
                        }

                        if (transaccionValida) {
                            listaConsumos.add(
                                ConsumoItem(
                                    id = "${System.currentTimeMillis()}-${(0..99999).random()}",
                                    nombre = nombreConsumoInput,
                                    monto = montoC,
                                    fuenteOrigen = fuenteConsumoSeleccionada,
                                    moneda = monedaConsumo
                                )
                            )
                            nombreConsumoInput = ""
                            montoConsumoInput = ""
                            mostrarDialogoAgregarConsumo = false
                            guardarTodo()
                        }
                    }
                }) { Text("Guardar Consumo") }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogoAgregarConsumo = false }) { Text("Cancelar") } }
        )
    }
}

// --- DIÁLOGO DE EDICIÓN Y CONVERSIÓN DE SALDOS ---
@Composable
fun DialogoEdicionSaldos(
    saldosActuales: SaldosMultiMoneda,
    onDismiss: () -> Unit,
    onGuardarCambiosDirectos: (SaldosMultiMoneda) -> Unit,
    onConfirmarCambio: (origen: TipoMoneda, destino: TipoMoneda, montoSalida: Double, montoEntrada: Double) -> Unit
) {
    var modo by remember { mutableStateOf("INTERCAMBIO") } // "INTERCAMBIO" o "EDITAR_DIRECTO"

    var origen by remember { mutableStateOf(TipoMoneda.LOCAL) }
    var destino by remember { mutableStateOf(TipoMoneda.DIVISA) }
    var montoSalidaInput by remember { mutableStateOf("") }
    var montoEntradaInput by remember { mutableStateOf("") }

    var editLocal by remember { mutableStateOf(saldosActuales.local.toString()) }
    var editDivisa by remember { mutableStateOf(saldosActuales.divisa.toString()) }
    var editUsdt by remember { mutableStateOf(saldosActuales.usdt.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar / Convertir Saldos") },
        text = {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(onClick = { modo = "INTERCAMBIO" }, enabled = modo != "INTERCAMBIO") { Text("Cambio / Compra") }
                    Button(onClick = { modo = "EDITAR_DIRECTO" }, enabled = modo != "EDITAR_DIRECTO") { Text("Editar Montos") }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (modo == "INTERCAMBIO") {
                    Text(text = "1. Moneda de Origen (Lo que gastas/sacas):", style = MaterialTheme.typography.bodyMedium)
                    TipoMoneda.values().forEach { m ->
                        val saldoDisp = when(m) {
                            TipoMoneda.LOCAL -> saldosActuales.local
                            TipoMoneda.DIVISA -> saldosActuales.divisa
                            TipoMoneda.USDT -> saldosActuales.usdt
                        }
                        TextButton(onClick = { origen = m }) {
                            Text(if (origen == m) "✔ ${m.etiqueta} (Disp: ${m.simbolo}$saldoDisp)" else "${m.etiqueta} (Disp: ${m.simbolo}$saldoDisp)")
                        }
                    }

                    OutlinedTextField(
                        value = montoSalidaInput,
                        onValueChange = { montoSalidaInput = it },
                        label = { Text("Monto a retirar (${origen.simbolo})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "2. Moneda de Destino (Lo que recibes/compras):", style = MaterialTheme.typography.bodyMedium)
                    TipoMoneda.values().forEach { m ->
                        if (m != origen) {
                            TextButton(onClick = { destino = m }) {
                                Text(if (destino == m) "✔ ${m.etiqueta}" else m.etiqueta)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = montoEntradaInput,
                        onValueChange = { montoEntradaInput = it },
                        label = { Text("Monto a abonar (${destino.simbolo})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                    )
                } else {
                    Text(text = "Corrige directamente los saldos si te equivocaste:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = editLocal, onValueChange = { editLocal = it }, label = { Text("Saldo Local (Bs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(value = editDivisa, onValueChange = { editDivisa = it }, label = { Text("Saldo Divisa ($)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(value = editUsdt, onValueChange = { editUsdt = it }, label = { Text("Saldo USDT") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (modo == "INTERCAMBIO") {
                    val salida = montoSalidaInput.toDoubleOrNull() ?: 0.0
                    val entrada = montoEntradaInput.toDoubleOrNull() ?: 0.0
                    if (salida > 0 && entrada > 0 && origen != destino) {
                        onConfirmarCambio(origen, destino, salida, entrada)
                    }
                } else {
                    val l = editLocal.toDoubleOrNull() ?: saldosActuales.local
                    val d = editDivisa.toDoubleOrNull() ?: saldosActuales.divisa
                    val u = editUsdt.toDoubleOrNull() ?: saldosActuales.usdt
                    onGuardarCambiosDirectos(SaldosMultiMoneda(local = l, divisa = d, usdt = u))
                }
            }) { Text(if (modo == "INTERCAMBIO") "Confirmar Cambio" else "Guardar Cambios") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// --- VISTA DETALLADA DE AHORROS ---
@Composable
fun VistaDetalleAhorros(
    listaAhorros: MutableList<AhorroItem>,
    saldos: SaldosMultiMoneda,
    onActualizarSaldos: (SaldosMultiMoneda) -> Unit,
    onCerrar: () -> Unit,
    onEliminar: (AhorroItem) -> Unit,
    onGuardarCambios: () -> Unit
) {
    val context = LocalContext.current
    var mostrarDialogoCrear by remember { mutableStateOf(false) }
    var nombreInput by remember { mutableStateOf("") }
    var porcentajeInput by remember { mutableStateOf("") }

    // Estado para el botón "Editar" en Ahorros (Mantiene el nombre exacto "Editar")
    var ahorroEnEdicion by remember { mutableStateOf<AhorroItem?>(null) }

    var ahorroParaAbono by remember { mutableStateOf<AhorroItem?>(null) }
    var montoAbonoInput by remember { mutableStateOf("") }
    var monedaAbonoSeleccionada by remember { mutableStateOf(TipoMoneda.DIVISA) }

    var ahorroParaRetiro by remember { mutableStateOf<AhorroItem?>(null) }
    var montoRetiroInput by remember { mutableStateOf("") }
    var monedaRetiroSeleccionada by remember { mutableStateOf(TipoMoneda.DIVISA) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Detalle de Ahorros", style = MaterialTheme.typography.titleLarge)
                Button(onClick = onCerrar) { Text("Volver") }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { mostrarDialogoCrear = true }, modifier = Modifier.fillMaxWidth()) { Text("Agregar Nueva Meta de Ahorro") }
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listaAhorros) { ahorro ->
                    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(text = ahorro.nombre, style = MaterialTheme.typography.titleMedium)
                            Text(text = "Porcentaje: ${ahorro.porcentaje}%", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Acumulados:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(text = "• Local: Bs.${ahorro.acumuladoLocal}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "• Divisa: $${ahorro.acumuladoDivisa}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "• USDT: ${ahorro.acumuladoUsdt} USDT", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { ahorroParaRetiro = ahorro; montoRetiroInput = "" }) { Text("Retirar", color = Color(0xFFE53935)) }
                                TextButton(onClick = { ahorroParaAbono = ahorro; montoAbonoInput = "" }) { Text("Abonar") }
                                // Botón que mantiene el nombre de "Editar"
                                TextButton(onClick = { ahorroEnEdicion = ahorro }) { Text("Editar") }
                                TextButton(onClick = { onEliminar(ahorro) }) { Text("Eliminar", color = Color.Red) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogoCrear) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoCrear = false },
            title = { Text("Crear Nuevo Ahorro") },
            text = {
                Column {
                    OutlinedTextField(value = nombreInput, onValueChange = { nombreInput = it }, label = { Text("Nombre") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = porcentajeInput, onValueChange = { porcentajeInput = it }, label = { Text("Porcentaje (%)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val p = porcentajeInput.toFloatOrNull() ?: 0f
                    if (nombreInput.isNotBlank()) {
                        listaAhorros.add(AhorroItem(System.currentTimeMillis().toString(), nombreInput, p))
                        nombreInput = ""
                        porcentajeInput = ""
                        mostrarDialogoCrear = false
                        onGuardarCambios()
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogoCrear = false }) { Text("Cancelar") } }
        )
    }

    // DIÁLOGO DE EDICIÓN PARA EL AHORRO SELECCIONADO (Nombre, % y conversión interna o corrección de montos)
    if (ahorroEnEdicion != null) {
        val metaActual = ahorroEnEdicion!!
        var subModo by remember { mutableStateOf("GENERAL") } // "GENERAL" o "CONVERTIR_INTERNO" o "CORREGIR_MONTOS"

        var nuevoNombre by remember { mutableStateOf(metaActual.nombre) }
        var nuevoPorcentaje by remember { mutableStateOf(metaActual.porcentaje.toString()) }

        var origenAho by remember { mutableStateOf(TipoMoneda.LOCAL) }
        var destinoAho by remember { mutableStateOf(TipoMoneda.DIVISA) }
        var montoSalidaAho by remember { mutableStateOf("") }
        var montoEntradaAho by remember { mutableStateOf("") }

        var corrLocal by remember { mutableStateOf(metaActual.acumuladoLocal.toString()) }
        var corrDivisa by remember { mutableStateOf(metaActual.acumuladoDivisa.toString()) }
        var corrUsdt by remember { mutableStateOf(metaActual.acumuladoUsdt.toString()) }

        AlertDialog(
            onDismissRequest = { ahorroEnEdicion = null },
            title = { Text("Editar Meta: ${metaActual.nombre}") },
            text = {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { subModo = "GENERAL" }, enabled = subModo != "GENERAL") { Text("Datos") }
                        TextButton(onClick = { subModo = "CONVERTIR_INTERNO" }, enabled = subModo != "CONVERTIR_INTERNO") { Text("Intercambio") }
                        TextButton(onClick = { subModo = "CORREGIR_MONTOS" }, enabled = subModo != "CORREGIR_MONTOS") { Text("Montos") }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (subModo == "GENERAL") {
                        OutlinedTextField(value = nuevoNombre, onValueChange = { nuevoNombre = it }, label = { Text("Nombre") })
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = nuevoPorcentaje, onValueChange = { nuevoPorcentaje = it }, label = { Text("Porcentaje (%)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done))
                    } else if (subModo == "CONVERTIR_INTERNO") {
                        Text(text = "Intercambia fondos dentro de este ahorro:", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Origen (Saca):")
                        TipoMoneda.values().forEach { m ->
                            val disp = when(m) { TipoMoneda.LOCAL -> metaActual.acumuladoLocal; TipoMoneda.DIVISA -> metaActual.acumuladoDivisa; TipoMoneda.USDT -> metaActual.acumuladoUsdt }
                            TextButton(onClick = { origenAho = m }) { Text(if (origenAho == m) "✔ ${m.etiqueta} ($disp)" else "${m.etiqueta} ($disp)") }
                        }
                        OutlinedTextField(value = montoSalidaAho, onValueChange = { montoSalidaAho = it }, label = { Text("Monto a retirar") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done))

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Destino (Pone):")
                        TipoMoneda.values().forEach { m ->
                            if (m != origenAho) {
                                TextButton(onClick = { destinoAho = m }) { Text(if (destinoAho == m) "✔ ${m.etiqueta}" else m.etiqueta) }
                            }
                        }
                        OutlinedTextField(value = montoEntradaAho, onValueChange = { montoEntradaAho = it }, label = { Text("Monto a abonar") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done))
                    } else {
                        Text(text = "Corrige montos acumulados si hubo error:", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(value = corrLocal, onValueChange = { corrLocal = it }, label = { Text("Acumulado Local (Bs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done))
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(value = corrDivisa, onValueChange = { corrDivisa = it }, label = { Text("Acumulado Divisa ($)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done))
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(value = corrUsdt, onValueChange = { corrUsdt = it }, label = { Text("Acumulado USDT") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val index = listaAhorros.indexOfFirst { it.id == metaActual.id }
                    if (index != -1) {
                        if (subModo == "GENERAL") {
                            val p = nuevoPorcentaje.toFloatOrNull() ?: metaActual.porcentaje
                            listaAhorros[index] = metaActual.copy(nombre = nuevoNombre, porcentaje = p)
                        } else if (subModo == "CONVERTIR_INTERNO") {
                            val salida = montoSalidaAho.toDoubleOrNull() ?: 0.0
                            val entrada = montoEntradaAho.toDoubleOrNull() ?: 0.0
                            val dispOrigen = when(origenAho) { TipoMoneda.LOCAL -> metaActual.acumuladoLocal; TipoMoneda.DIVISA -> metaActual.acumuladoDivisa; TipoMoneda.USDT -> metaActual.acumuladoUsdt }

                            if (salida > 0 && entrada > 0 && salida <= dispOrigen && origenAho != destinoAho) {
                                var temp = when(origenAho) {
                                    TipoMoneda.LOCAL -> metaActual.copy(acumuladoLocal = metaActual.acumuladoLocal - salida)
                                    TipoMoneda.DIVISA -> metaActual.copy(acumuladoDivisa = metaActual.acumuladoDivisa - salida)
                                    TipoMoneda.USDT -> metaActual.copy(acumuladoUsdt = metaActual.acumuladoUsdt - salida)
                                }
                                temp = when(destinoAho) {
                                    TipoMoneda.LOCAL -> temp.copy(acumuladoLocal = temp.acumuladoLocal + entrada)
                                    TipoMoneda.DIVISA -> temp.copy(acumuladoDivisa = temp.acumuladoDivisa + entrada)
                                    TipoMoneda.USDT -> temp.copy(acumuladoUsdt = temp.acumuladoUsdt + entrada)
                                }
                                listaAhorros[index] = temp
                            } else {
                                Toast.makeText(context, "Verifica los montos de conversión", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val l = corrLocal.toDoubleOrNull() ?: metaActual.acumuladoLocal
                            val d = corrDivisa.toDoubleOrNull() ?: metaActual.acumuladoDivisa
                            val u = corrUsdt.toDoubleOrNull() ?: metaActual.acumuladoUsdt
                            listaAhorros[index] = metaActual.copy(acumuladoLocal = l, acumuladoDivisa = d, acumuladoUsdt = u)
                        }
                        onGuardarCambios()
                    }
                    ahorroEnEdicion = null
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { ahorroEnEdicion = null }) { Text("Cancelar") } }
        )
    }

    if (ahorroParaAbono != null) {
        AlertDialog(
            onDismissRequest = { ahorroParaAbono = null },
            title = { Text("Abonar Extra a ${ahorroParaAbono?.nombre}") },
            text = {
                Column {
                    Text(text = "Moneda del abono:", style = MaterialTheme.typography.bodyMedium)
                    TipoMoneda.values().forEach { m ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = monedaAbonoSeleccionada == m, onClick = { monedaAbonoSeleccionada = m })
                            Text(text = m.etiqueta)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = montoAbonoInput, onValueChange = { montoAbonoInput = it }, label = { Text("Monto extra") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val extra = montoAbonoInput.toDoubleOrNull() ?: 0.0
                    if (extra > 0) {
                        val index = listaAhorros.indexOfFirst { it.id == ahorroParaAbono?.id }
                        if (index != -1) {
                            val a = listaAhorros[index]
                            listaAhorros[index] = when (monedaAbonoSeleccionada) {
                                TipoMoneda.LOCAL -> a.copy(acumuladoLocal = a.acumuladoLocal + extra)
                                TipoMoneda.DIVISA -> a.copy(acumuladoDivisa = a.acumuladoDivisa + extra)
                                TipoMoneda.USDT -> a.copy(acumuladoUsdt = a.acumuladoUsdt + extra)
                            }
                            onGuardarCambios()
                        }
                    }
                    ahorroParaAbono = null
                }) { Text("Abonar") }
            },
            dismissButton = { TextButton(onClick = { ahorroParaAbono = null }) { Text("Cancelar") } }
        )
    }

    if (ahorroParaRetiro != null) {
        AlertDialog(
            onDismissRequest = { ahorroParaRetiro = null },
            title = { Text("Retirar de ${ahorroParaRetiro?.nombre}") },
            text = {
                Column {
                    Text(text = "Moneda a retirar:", style = MaterialTheme.typography.bodyMedium)
                    TipoMoneda.values().forEach { m ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = monedaRetiroSeleccionada == m, onClick = { monedaRetiroSeleccionada = m })
                            Text(text = m.etiqueta)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = montoRetiroInput, onValueChange = { montoRetiroInput = it }, label = { Text("Monto a retirar") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val retiro = montoRetiroInput.toDoubleOrNull() ?: 0.0
                    val meta = ahorroParaRetiro!!

                    val saldoActual = when (monedaRetiroSeleccionada) {
                        TipoMoneda.LOCAL -> meta.acumuladoLocal
                        TipoMoneda.DIVISA -> meta.acumuladoDivisa
                        TipoMoneda.USDT -> meta.acumuladoUsdt
                    }

                    if (retiro > saldoActual) {
                        Toast.makeText(context, "Monto mayor al disponible en esa moneda", Toast.LENGTH_SHORT).show()
                    } else if (retiro > 0) {
                        val index = listaAhorros.indexOfFirst { it.id == meta.id }
                        if (index != -1) {
                            listaAhorros[index] = when (monedaRetiroSeleccionada) {
                                TipoMoneda.LOCAL -> meta.copy(acumuladoLocal = meta.acumuladoLocal - retiro)
                                TipoMoneda.DIVISA -> meta.copy(acumuladoDivisa = meta.acumuladoDivisa - retiro)
                                TipoMoneda.USDT -> meta.copy(acumuladoUsdt = meta.acumuladoUsdt - retiro)
                            }

                            onActualizarSaldos(
                                when (monedaRetiroSeleccionada) {
                                    TipoMoneda.LOCAL -> saldos.copy(local = saldos.local + retiro)
                                    TipoMoneda.DIVISA -> saldos.copy(divisa = saldos.divisa + retiro)
                                    TipoMoneda.USDT -> saldos.copy(usdt = saldos.usdt + retiro)
                                }
                            )
                            onGuardarCambios()
                        }
                        ahorroParaRetiro = null
                    }
                }) { Text("Confirmar Retiro a Saldo") }
            },
            dismissButton = { TextButton(onClick = { ahorroParaRetiro = null }) { Text("Cancelar") } }
        )
    }
}

// --- VISTA DETALLADA DE GASTOS ---
@Composable
fun VistaDetalleGastos(
    listaGastos: MutableList<GastoItem>,
    listaAhorros: MutableList<AhorroItem>,
    listaConsumos: MutableList<ConsumoItem>,
    saldos: SaldosMultiMoneda,
    onActualizarSaldos: (SaldosMultiMoneda) -> Unit,
    onCerrar: () -> Unit,
    onEliminar: (GastoItem) -> Unit,
    onGuardarCambios: () -> Unit
) {
    val context = LocalContext.current
    var gastoParaPagar by remember { mutableStateOf<GastoItem?>(null) }
    var montoPagoInput by remember { mutableStateOf("") }
    var fuenteSeleccionada by remember { mutableStateOf("SALDO") }

    var mostrarDialogoCrearGasto by remember { mutableStateOf(false) }
    var nombreGastoInput by remember { mutableStateOf("") }
    var montoGastoInput by remember { mutableStateOf("") }
    var esGastoFijoInput by remember { mutableStateOf(false) }
    var fechaPagoGastoInput by remember { mutableStateOf("") }
    var diaFijoSeleccionado by remember { mutableStateOf("1") }
    var monedaGastoInput by remember { mutableStateOf(TipoMoneda.DIVISA) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Detalle de Gastos", style = MaterialTheme.typography.titleLarge)
                Button(onClick = onCerrar) { Text("Volver") }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    nombreGastoInput = ""
                    montoGastoInput = ""
                    esGastoFijoInput = false
                    fechaPagoGastoInput = ""
                    diaFijoSeleccionado = "1"
                    monedaGastoInput = TipoMoneda.DIVISA
                    mostrarDialogoCrearGasto = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Agregar Nuevo Gasto / Deuda")
            }
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listaGastos) { gasto ->
                    val pendiente = (gasto.montoTotal - gasto.montoPagado).coerceAtLeast(0.0)
                    val textoFecha = if (gasto.esFijo) "Día ${gasto.fechaPago} de cada mes" else "Límite: ${gasto.fechaPago}"

                    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = gasto.nombre, style = MaterialTheme.typography.titleMedium)
                                Text(text = gasto.moneda.etiqueta, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Text(text = "Tipo: ${if (gasto.esFijo) "Gasto Fijo" else "Deuda"} | $textoFecha", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Total: ${gasto.moneda.simbolo}${gasto.montoTotal} | Pendiente: ${gasto.moneda.simbolo}$pendiente", style = MaterialTheme.typography.bodyMedium, color = Color.Red)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { gastoParaPagar = gasto; montoPagoInput = ""; fuenteSeleccionada = "SALDO" }) { Text("Abonar / Pagar") }
                                TextButton(onClick = { onEliminar(gasto) }) { Text("Eliminar", color = Color.Red) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogoCrearGasto) {
        val calendario = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val mesFormateado = String.format("%02d", month + 1)
                val diaFormateado = String.format("%02d", dayOfMonth)
                fechaPagoGastoInput = "$year-$mesFormateado-$diaFormateado"
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        )

        AlertDialog(
            onDismissRequest = { mostrarDialogoCrearGasto = false },
            title = { Text("Crear Nuevo Gasto / Deuda") },
            text = {
                Column {
                    OutlinedTextField(value = nombreGastoInput, onValueChange = { nombreGastoInput = it }, label = { Text("Nombre") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = montoGastoInput, onValueChange = { montoGastoInput = it }, label = { Text("Monto Total") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Tipo de gasto:", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !esGastoFijoInput, onClick = { esGastoFijoInput = false })
                        Text(text = "Deuda (Puntual)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = esGastoFijoInput, onClick = { esGastoFijoInput = true })
                        Text(text = "Gasto Fijo (Recurrente mensual)")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (esGastoFijoInput) {
                        Text(text = "Día del mes para pago recurrente:", style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value = diaFijoSeleccionado,
                            onValueChange = { nuevoDia ->
                                val num = nuevoDia.toIntOrNull()
                                if (nuevoDia.isEmpty() || (num != null && num in 1..31)) {
                                    diaFijoSeleccionado = nuevoDia
                                }
                            },
                            label = { Text("Día (1 - 31)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { datePickerDialog.show() }
                        ) {
                            OutlinedTextField(
                                value = fechaPagoGastoInput,
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                label = { Text("Fecha límite (Toca para elegir)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Moneda:", style = MaterialTheme.typography.bodyMedium)
                    TipoMoneda.values().forEach { m ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = monedaGastoInput == m, onClick = { monedaGastoInput = m })
                            Text(text = m.etiqueta)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val m = montoGastoInput.toDoubleOrNull() ?: 0.0
                    val fechaDefinitiva = if (esGastoFijoInput) diaFijoSeleccionado else fechaPagoGastoInput

                    if (nombreGastoInput.isNotBlank() && m > 0 && fechaDefinitiva.isNotBlank()) {
                        listaGastos.add(
                            GastoItem(
                                id = System.currentTimeMillis().toString(),
                                nombre = nombreGastoInput,
                                montoTotal = m,
                                montoPagado = 0.0,
                                esFijo = esGastoFijoInput,
                                fechaPago = fechaDefinitiva,
                                moneda = monedaGastoInput
                            )
                        )
                        mostrarDialogoCrearGasto = false
                        onGuardarCambios()
                    } else {
                        Toast.makeText(context, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogoCrearGasto = false }) { Text("Cancelar") } }
        )
    }

    if (gastoParaPagar != null) {
        val gastoObj = gastoParaPagar!!
        val pendienteReal = (gastoObj.montoTotal - gastoObj.montoPagado).coerceAtLeast(0.0)

        AlertDialog(
            onDismissRequest = { gastoParaPagar = null },
            title = { Text("Pagar a ${gastoObj.nombre}") },
            text = {
                Column {
                    Text(text = "Pendiente: ${gastoObj.moneda.simbolo}$pendienteReal", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = montoPagoInput,
                        onValueChange = { montoPagoInput = it },
                        label = { Text("Monto a abonar") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Sacar dinero de:", style = MaterialTheme.typography.bodyMedium)

                    TextButton(onClick = { fuenteSeleccionada = "SALDO" }) {
                        val saldoDisp = when (gastoObj.moneda) {
                            TipoMoneda.LOCAL -> saldos.local
                            TipoMoneda.DIVISA -> saldos.divisa
                            TipoMoneda.USDT -> saldos.usdt
                        }
                        Text(text = if (fuenteSeleccionada == "SALDO") "✔ Saldo General (${gastoObj.moneda.simbolo}$saldoDisp)" else "Saldo General (${gastoObj.moneda.simbolo}$saldoDisp)")
                    }

                    TextButton(onClick = { fuenteSeleccionada = "EXTERNO" }) {
                        Text(text = if (fuenteSeleccionada == "EXTERNO") "✔ Monto Externo (Fuera de la app)" else "Monto Externo (Fuera de la app)")
                    }

                    for (ahorro in listaAhorros) {
                        val dispAhorro = when (gastoObj.moneda) {
                            TipoMoneda.LOCAL -> ahorro.acumuladoLocal
                            TipoMoneda.DIVISA -> ahorro.acumuladoDivisa
                            TipoMoneda.USDT -> ahorro.acumuladoUsdt
                        }
                        if (dispAhorro > 0.0) {
                            TextButton(onClick = { fuenteSeleccionada = ahorro.id }) {
                                Text(text = if (fuenteSeleccionada == ahorro.id) "✔ Ahorro: ${ahorro.nombre} (${gastoObj.moneda.simbolo}$dispAhorro)" else "Ahorro: ${ahorro.nombre} (${gastoObj.moneda.simbolo}$dispAhorro)")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val monto = montoPagoInput.toDoubleOrNull() ?: 0.0
                    val montoEfectivo = if (monto > pendienteReal) pendienteReal else monto

                    if (montoEfectivo > 0) {
                        var transaccionValida = false
                        var nombreFuente = ""

                        when (fuenteSeleccionada) {
                            "SALDO" -> {
                                val saldoDisp = when (gastoObj.moneda) {
                                    TipoMoneda.LOCAL -> saldos.local
                                    TipoMoneda.DIVISA -> saldos.divisa
                                    TipoMoneda.USDT -> saldos.usdt
                                }
                                if (saldoDisp >= montoEfectivo) {
                                    onActualizarSaldos(
                                        when (gastoObj.moneda) {
                                            TipoMoneda.LOCAL -> saldos.copy(local = saldos.local - montoEfectivo)
                                            TipoMoneda.DIVISA -> saldos.copy(divisa = saldos.divisa - montoEfectivo)
                                            TipoMoneda.USDT -> saldos.copy(usdt = saldos.usdt - montoEfectivo)
                                        }
                                    )
                                    transaccionValida = true
                                    nombreFuente = "Saldo General"
                                } else {
                                    Toast.makeText(context, "Saldo general insuficiente", Toast.LENGTH_SHORT).show()
                                }
                            }
                            "EXTERNO" -> {
                                transaccionValida = true
                                nombreFuente = "Externo"
                            }
                            else -> {
                                val indexAhorro = listaAhorros.indexOfFirst { it.id == fuenteSeleccionada }
                                if (indexAhorro != -1) {
                                    val metaAhorro = listaAhorros[indexAhorro]
                                    val dispAhorro = when (gastoObj.moneda) {
                                        TipoMoneda.LOCAL -> metaAhorro.acumuladoLocal
                                        TipoMoneda.DIVISA -> metaAhorro.acumuladoDivisa
                                        TipoMoneda.USDT -> metaAhorro.acumuladoUsdt
                                    }

                                    if (dispAhorro >= montoEfectivo) {
                                        listaAhorros[indexAhorro] = when (gastoObj.moneda) {
                                            TipoMoneda.LOCAL -> metaAhorro.copy(acumuladoLocal = metaAhorro.acumuladoLocal - montoEfectivo)
                                            TipoMoneda.DIVISA -> metaAhorro.copy(acumuladoDivisa = metaAhorro.acumuladoDivisa - montoEfectivo)
                                            TipoMoneda.USDT -> metaAhorro.copy(acumuladoUsdt = metaAhorro.acumuladoUsdt - montoEfectivo)
                                        }
                                        transaccionValida = true
                                        nombreFuente = "Ahorro: ${metaAhorro.nombre}"
                                    } else {
                                        Toast.makeText(context, "Fondos insuficientes en el ahorro seleccionado", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }

                        if (transaccionValida) {
                            val indexGasto = listaGastos.indexOfFirst { it.id == gastoObj.id }
                            if (indexGasto != -1) {
                                val nuevoPagado = gastoObj.montoPagado + montoEfectivo
                                if (nuevoPagado >= gastoObj.montoTotal && !gastoObj.esFijo) {
                                    listaGastos.removeAt(indexGasto)
                                } else {
                                    listaGastos[indexGasto] = gastoObj.copy(montoPagado = nuevoPagado)
                                }
                            }

                            if (fuenteSeleccionada != "EXTERNO") {
                                listaConsumos.add(
                                    ConsumoItem(
                                        id = "${System.currentTimeMillis()}-${(0..99999).random()}",
                                        nombre = "Pago Gasto: ${gastoObj.nombre}",
                                        monto = montoEfectivo,
                                        fuenteOrigen = nombreFuente,
                                        moneda = gastoObj.moneda,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                            }

                            onGuardarCambios()
                            gastoParaPagar = null
                        }
                    }
                }) { Text("Confirmar Pago") }
            },
            dismissButton = { TextButton(onClick = { gastoParaPagar = null }) { Text("Cancelar") } }
        )
    }
}

// --- VISTA DETALLADA DE CONSUMOS ---
@Composable
fun VistaDetalleConsumos(
    listaConsumos: MutableList<ConsumoItem>,
    onCerrar: () -> Unit,
    onEliminar: (ConsumoItem) -> Unit
) {
    LaunchedEffect(Unit) {
        val unMesEnMs = 30L * 24 * 60 * 60 * 1000
        val ahora = System.currentTimeMillis()
        listaConsumos.removeAll { ahora - it.timestamp > unMesEnMs }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Detalle de Consumos", style = MaterialTheme.typography.titleLarge)
                Button(onClick = onCerrar) { Text("Volver") }
            }
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listaConsumos.reversed(), key = { it.id }) { consumo ->                    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(text = consumo.nombre, style = MaterialTheme.typography.titleMedium)
                            Text(text = "Monto: ${consumo.moneda.simbolo}${consumo.monto}", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Origen: ${consumo.fuenteOrigen}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { onEliminar(consumo) }) { Text("Eliminar", color = Color.Red) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- TARJETAS DE RESUMEN ---
@Composable
fun TarjetaAhorroResumen(listaAhorros: List<AhorroItem>, onClicTitulo: () -> Unit, onClicAgregar: () -> Unit) {
    Card(modifier = Modifier.width(220.dp).height(320.dp), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1E88E5)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Ahorro ⤢", style = MaterialTheme.typography.titleLarge, color = Color.White, modifier = Modifier.clickable { onClicTitulo() })
                Text(text = "Metas: ${listaAhorros.size}", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
            }
            Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (ahorro in listaAhorros.take(3)) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "${ahorro.nombre} (${ahorro.porcentaje}%)", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        Text(text = "Bs.${ahorro.acumuladoLocal} | $${ahorro.acumuladoDivisa} | ${ahorro.acumuladoUsdt} U", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Button(onClick = onClicAgregar, modifier = Modifier.fillMaxWidth()) { Text("Agregar") }
        }
    }
}

@Composable
fun TarjetaGastoResumen(totalGastos: Double, listaGastos: List<GastoItem>, onClicTitulo: () -> Unit, onClicAgregar: () -> Unit) {
    Card(modifier = Modifier.width(220.dp).height(320.dp), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFE53935)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Gastos ⤢", style = MaterialTheme.typography.titleLarge, color = Color.White, modifier = Modifier.clickable { onClicTitulo() })
                Text(text = "Pendientes: ${listaGastos.size}", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
            }
            Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (gasto in listaGastos.take(3)) {
                    val p = (gasto.montoTotal - gasto.montoPagado).coerceAtLeast(0.0)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = gasto.nombre, color = Color.White, style = MaterialTheme.typography.bodySmall)
                        Text(text = "${gasto.moneda.simbolo}$p", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Button(onClick = onClicAgregar, modifier = Modifier.fillMaxWidth()) { Text("Agregar") }
        }
    }
}

@Composable
fun TarjetaConsumoResumen(totalConsumido: Double, listaConsumos: List<ConsumoItem>, onClicTitulo: () -> Unit, onClicAgregar: () -> Unit) {
    Card(modifier = Modifier.width(220.dp).height(320.dp), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFB8C00)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Consumos ⤢", style = MaterialTheme.typography.titleLarge, color = Color.White, modifier = Modifier.clickable { onClicTitulo() })
                Text(text = "Registros: ${listaConsumos.size}", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
            }
            Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Aquí aplicamos .reversed() para que lo último que registraste salga arriba
                for (consumo in listaConsumos.reversed().take(3)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = consumo.nombre, color = Color.White, style = MaterialTheme.typography.bodySmall)
                        Text(text = "${consumo.moneda.simbolo}${consumo.monto}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Button(onClick = onClicAgregar, modifier = Modifier.fillMaxWidth()) { Text("Agregar") }
        }
    }
}