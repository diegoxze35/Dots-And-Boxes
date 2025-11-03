# Dots and Boxes para Android

Este es un juego clásico de "Dots and Boxes" (Timbiriche o Cuadritos) desarrollado de forma nativa para Android utilizando Kotlin y Jetpack Compose.

La aplicación permite a los usuarios jugar en varios modos: multijugador local, contra una IA simple, o contra otro jugador a través de una conexión Bluetooth. Además, incluye persistencia de datos para guardar partidas en curso (en formato JSON) y un historial de partidas finalizadas (usando una base de datos Room).

## 🚀 Características Principales

* **Modo 1 vs. 1 Local:** Juega contra un amigo en el mismo dispositivo.
* **Modo 1 vs. IA:** Juega contra un oponente de IA (un algoritmo "greedy" simple que prioriza completar cajas).
* **Multijugador por Bluetooth:**
    * Escanear dispositivos cercanos y conectarse a ellos.
    * Hacer que el dispositivo sea "descubrible" para actuar como anfitrión (servidor).
    * Jugar una partida completa en tiempo real, con bloqueo de turno y envío de movimientos a través de sockets Bluetooth.
* **Guardar y Cargar Partidas:**
    * Guarda el estado de cualquier partida local (1v1 o 1vIA) en cualquier momento.
    * Las partidas se guardan como archivos JSON en el almacenamiento interno de la app.
    * Una pantalla de "Partidas Guardadas" permite al usuario ver, cargar o eliminar partidas anteriores.
* **Historial de Partidas:**
    * Todas las partidas completadas (locales, IA y anfitrión de BT) se guardan automáticamente en una base de datos **Room**.
    * Una pantalla de "Historial" muestra un resumen de los resultados anteriores, incluyendo oponentes, ganador y duración.

## 🛠️ Stack Tecnológico y Arquitectura

Este proyecto está construido 100% en **Kotlin** y utiliza una arquitectura **MVVM (Model-View-ViewModel)**.

* **UI:** **Jetpack Compose** se utiliza para construir toda la interfaz de usuario de forma declarativa.
* **Gestión de Estado:** El estado se gestiona en `GameViewModel` y `BluetoothViewModel` y se expone a la UI mediante `StateFlow`.
* **Asincronía:** **Kotlin Coroutines** se utiliza para todas las operaciones asíncronas, incluyendo movimientos de la IA, E/S de archivos y la gestión de conexiones Bluetooth.
* **Navegación:** **Navigation for Compose** se utiliza para gestionar la navegación entre las diferentes pantallas (Menú, Juego, Lobby, Historial, etc.).
* **Persistencia de Historial:** **Room Database** almacena la entidad `GameResult` para el historial de partidas.
* **Persistencia de Partidas Guardadas:** **Kotlinx Serialization** se utiliza para serializar/deserializar el `GameUiState` a y desde archivos JSON.
* **Conectividad:** Las **API de Bluetooth** nativas de Android (Sockets RFCOMM) se utilizan para la funcionalidad multijugador.

### Flujo de Datos (Bluetooth)

Para desacoplar la lógica del juego de la lógica de conexión, se utiliza un objeto singleton `BluetoothGameManager`:

1.  **`GameViewModel`** (cuando el jugador local hace un movimiento) -> `BluetoothGameManager.emitOutgoingMove(line)`
2.  **`BluetoothViewModel`** (recoge el `outgoingMove`) -> Envía el `line` serializado por el socket Bluetooth.
3.  (El dispositivo oponente recibe los datos)
4.  **`BluetoothViewModel`** (del oponente) -> `BluetoothGameManager.emitIncomingMove(line)`.
5.  **`GameViewModel`** (del oponente) (recoge el `incomingMove`) -> Procesa el movimiento como si fuera un oponente.

## 📦 Estructura del Proyecto
