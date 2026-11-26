package com.example.myquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

// CLASE PRINCIPAL: Punto de entrada de la aplicación en Android
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current

            // Estados globales de apariencia persistidos en SharedPreferences
            var temaApp by remember { mutableStateOf(leerTemaGuardado(context)) }
            var escalaFuente by remember { mutableFloatStateOf(leerEscalaFuenteGuardada(context)) }

            val usarOscuro = when (temaApp) {
                "OSCURO" -> true
                "CLARO" -> false
                else -> isSystemInDarkTheme()
            }

            val colorScheme = if (usarOscuro) darkColorScheme() else lightColorScheme()
            val densidadBase = LocalDensity.current

            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = densidadBase.density,
                    fontScale = densidadBase.fontScale * escalaFuente
                )
            ) {
                MaterialTheme(colorScheme = colorScheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        PantallaPrincipal(
                            temaActual = temaApp,
                            onCambiarTema = { nuevoTema ->
                                temaApp = nuevoTema
                                guardarTema(context, nuevoTema)
                            },
                            escalaActual = escalaFuente,
                            onCambiarEscala = { nuevaEscala ->
                                escalaFuente = nuevaEscala
                                guardarEscalaFuente(context, nuevaEscala)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal(
    temaActual: String,
    onCambiarTema: (String) -> Unit,
    escalaActual: Float,
    onCambiarEscala: (Float) -> Unit
) {
    // Estado para controlar si el menú lateral está abierto o cerrado
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Estado unificado para controlar qué pestaña está activa
    var pantallaActual by remember { mutableStateOf("INICIO") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Menú Principal",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // Opción para ir al Panel Principal
                NavigationDrawerItem(
                    label = { Text(text = "Inicio / Panel") },
                    selected = pantallaActual == "INICIO",
                    onClick = {
                        pantallaActual = "INICIO"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // Opción de Tareas
                NavigationDrawerItem(
                    label = { Text(text = "Tareas") },
                    selected = pantallaActual == "TAREAS",
                    onClick = {
                        pantallaActual = "TAREAS"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // Opción de Economía
                NavigationDrawerItem(
                    label = { Text(text = "Economía") },
                    selected = pantallaActual == "ECONOMIA",
                    onClick = {
                        pantallaActual = "ECONOMIA"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // Opción de Ajustes
                NavigationDrawerItem(
                    label = { Text(text = "Ajustes") },
                    selected = pantallaActual == "AJUSTES",
                    onClick = {
                        pantallaActual = "AJUSTES"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    // Cambia el título de la barra superior según la sección activa
                    title = {
                        val titulo = when (pantallaActual) {
                            "ECONOMIA" -> "myQuest - Economía"
                            "TAREAS" -> "myQuest - Tareas"
                            "AJUSTES" -> "myQuest - Ajustes"
                            else -> "myQuest"
                        }
                        Text(titulo)
                    },
                    // Botón de tres rayas (hamburguesa) para abrir o cerrar el menú
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        }) {
                            Text("☰", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                )
            }
        ) { paddingInterno ->
            // Contenedor que cambia de vista según la opción seleccionada
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingInterno)
            ) {
                when (pantallaActual) {
                    "ECONOMIA" -> PantallaEconomia()
                    "TAREAS" -> TareaScreen()
                    "AJUSTES" -> AjustesScreen(
                        temaActual = temaActual,
                        onCambiarTema = onCambiarTema,
                        escalaActual = escalaActual,
                        onCambiarEscala = onCambiarEscala
                    )
                    else -> {
                        Text(
                            text = "Toca el botón ☰ o desliza desde la izquierda",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}