# Untitled  Bomberman Game

Roguelike de escritorio en Java Swing. Derrota enemigos con bombas, encuentra la salida y elige una de tres cartas. Cada nivel genera un laberinto y la partida continúa hasta que pierdes toda la vida.

## Ejecutar

Requisitos: **JDK 21** y **Maven 3.9.x**. La primera compilación requiere Internet para descargar las dependencias. Verifica que `java -version` y `mvn -version` indiquen Java 21.

### NetBeans

1. Clona el repositorio o descarga **Code → Download ZIP** y descomprímelo.
2. Abre **File → Open Project** y selecciona la carpeta raíz que contiene el `pom.xml`. Configura JDK 21 en NetBeans.
3. Haz clic derecho sobre **mecha-viva** y selecciona **Clean and Build**.
4. Cuando aparezca `BUILD SUCCESS`, ejecuta el JAR desde una terminal abierta en la carpeta raíz.

```sh
java -jar launcher/target/launcher-0.1.0-SNAPSHOT.jar
```

### Terminal

```sh
git clone https://github.com/7CodeChiri/proyecto-final-poo.git
cd proyecto-final-poo
mvn clean verify
java -jar launcher/target/launcher-0.1.0-SNAPSHOT.jar
```

Si Windows tiene otra versión de Java en el PATH, ejecuta el binario del JDK 21 directamente desde PowerShell.

```powershell
& "C:\ruta\a\jdk-21\bin\java.exe" -jar ".\launcher\target\launcher-0.1.0-SNAPSHOT.jar"
```

## Controles y reglas

- **WASD o flechas** para moverte y **Espacio** para colocar una bomba.
- Las explosiones avanzan en cruz, destruyen cajas, alcanzan enemigos y pueden activar otras bombas. Las paredes detienen la explosión.
- La salida se abre al derrotar a todos los enemigos. Selecciona una carta con el mouse para avanzar. Las mejoras se conservan hasta que termina la partida.
- El patrullero elige una casilla libre al azar. El perseguidor prefiere la casilla que reduce la distancia al jugador.
- La simulación trabaja con una cuadrícula. La ventana interpola la posición del jugador y los enemigos para que se deslicen entre casillas.

## Dificultad adaptativa

El primer nivel utiliza el generador equilibrado. Al cruzar la salida, el juego registra el daño recibido y el tiempo del nivel. El selector evalúa las reglas en este orden.

| Condición del nivel anterior | Implementación |
| --- | --- |
| Perdiste al menos 2 puntos de vida | `RecoveryGenerator` · Java |
| Perdiste menos de 2 puntos y tardaste menos de 45 segundos | `ChallengeGenerator` · Scala |
| Los demás casos | `BalancedGenerator` · Java |

`LevelSelector` carga la clase con Reflection, valida que implemente `Strategy`, invoca el constructor y ejecuta el generador. La ventana muestra el modo y el motivo. El juego también registra la semilla del mapa en la terminal.

## Módulos

| Módulo | Responsabilidad |
| --- | --- |
| `plugin-api` | Contrato genérico `Strategy<I, O>`. |
| `domain` | Arena, solicitud y resultado de generación, enemigo abstracto y sus dos variantes. |
| `adaptive-core` | Reglas de selección y carga por Reflection. |
| `plugins-java` | Generadores equilibrado y de recuperación. |
| `plugins-scala` | Generador de desafío. |
| `launcher` | Ventana Swing, controles, reglas de partida y pruebas JUnit 5. |

`GameTest` comprueba los tres generadores, el corredor de salida, la selección de cartas y el daño de las bombas. GitHub Actions ejecuta `mvn clean verify` y adjunta el JAR si la compilación pasa.

## Pendientes de la entrega final

CRUD con JDBC, configuración JSON, Maven Wrapper, UML formal, Doxygen y reporte de cobertura. Los aportes individuales, revisiones y defensa deben documentarse durante el desarrollo.
