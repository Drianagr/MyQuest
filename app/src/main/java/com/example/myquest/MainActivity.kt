package com.example.myquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Scaffold
import kotlinx.coroutines.launch

// CLASE PRINCIPAL: Punto de entrada de la aplicación en Android
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PantallaPrincipal()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal() {
    // Estado para controlar si el menú lateral está abierto o cerrado
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Estado para recordar qué pestaña está activa ("INICIO" o "ECONOMIA")
    var pantallaActual by remember { mutableStateOf("INICIO") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Menú Principal",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                HorizontalDivider()

                // Opción del menú lateral para ir a Economía
                NavigationDrawerItem(
                    label = { Text("Economía") },
                    selected = pantallaActual == "ECONOMIA",
                    onClick = {
                        pantallaActual = "ECONOMIA" // Cambiamos la pantalla activa
                        scope.launch { drawerState.close() } // Cerramos el menú
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    // Cambia el título de la barra superior según la sección
                    title = {
                        Text(if (pantallaActual == "ECONOMIA") "myQuest - Economía" else "myQuest")
                    },
                    // Botón de tres rayas (hamburguesa) para abrir el menú
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
                if (pantallaActual == "ECONOMIA") {
                    // Llamamos a la pantalla de economía que está en su propio archivo
                    PantallaEconomia()
                } else {
                    Text(
                        text = "Toca el botón ☰ o desliza desde la izquierda",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
} // <--- Única llave de cierre para PantallaPrincipal
