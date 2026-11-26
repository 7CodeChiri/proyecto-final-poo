# Untitled Bomberman Game

Roguelike de escritorio en Java Swing. Cruza laberintos, derrota enemigos con bombas y elige una de tres cartas al superar cada nivel. El siguiente mapa se adapta al desempeño del jugador.

## Compilar y jugar

Instala el **JDK 21** y confirma con `java -version`. El Maven Wrapper descarga **Maven 3.9.9**. La primera compilación requiere Internet.

Clona el repositorio o usa **Code → Download ZIP** y descomprime. Abre una terminal en la carpeta que contiene `pom.xml`.

**Windows (PowerShell)**

```powershell
.\mvnw.cmd -version
.\mvnw.cmd clean verify
java -jar .\launcher\target\launcher-0.1.0-SNAPSHOT.jar
```

**Linux y macOS**

```sh
./mvnw -version
./mvnw clean verify
java -jar launcher/target/launcher-0.1.0-SNAPSHOT.jar
```

En NetBeans, abre la misma carpeta mediante **File → Open Project**, selecciona el JDK 21 y ejecuta **Clean and Build** sobre el proyecto padre. Ejecuta el JAR con el comando anterior. Si `java -version` muestra otro JDK, configura `JAVA_HOME` y el PATH antes de compilar o usa el ejecutable del JDK 21 directamente para abrir el JAR.

## Jugar y guardar el historial

1. Crea un perfil, selecciónalo y pulsa **Jugar**.
2. Muévete con **WASD o las flechas** y coloca bombas con **Espacio**.
3. Derrota a todos los enemigos, cruza la salida y elige una carta con el mouse.
4. Al perder toda la vida, el juego guarda nivel, bajas y tiempo activo. Pulsa **Perfiles** para consultar la partida, editar el alias o eliminar un perfil de prueba.

SQLite crea `bomberman.db` en el directorio desde el que ejecutas el juego. Puedes definir `BOMBERMAN_DB` para elegir otra ruta. La base local y el archivo `.env` están excluidos del repositorio. [Esquema SQL](db/schema.sql)

## Dificultad adaptable

El primer nivel es equilibrado. Cuando cruzas la salida, la partida mide **daño recibido** y **segundos de juego activo**. Tras elegir una carta, el selector evalúa las reglas en este orden.

| Escenario con los parámetros iniciales | Generador |
| --- | --- |
| Perdió 2 puntos de vida o más | Recuperación · Java |
| Perdió menos de 2 puntos y tardó menos de 45 segundos | Desafío · Scala |
| Perdió menos de 2 puntos y tardó 45 segundos o más | Equilibrado · Java |

Edita [`config/difficulty.json`](config/difficulty.json) y reinicia para cambiar los umbrales o las clases configuradas. `BOMBERMAN_CONFIG` permite elegir otra ruta. Si el archivo externo está ausente, el JAR usa la configuración incorporada.

El selector carga la clase por nombre, verifica que implemente `Strategy`, invoca el constructor y ejecuta la estrategia. La ventana muestra el generador y su motivo. El registro de terminal añade la semilla y las métricas.

## Documentación y entrega

- [Arquitectura](docs/architecture.md) y [diagramas PlantUML](docs/diagrams/)
- [Pruebas y reporte de cobertura](docs/testing.md)
- [Presentación](docs/presentacion.md) y [guion para la defensa](docs/defensa.md)
- [Doxygen](Doxyfile) y [plantilla de PR](.github/pull_request_template.md)

GitHub Actions compila el JAR y genera informes de Surefire y JaCoCo, documentación de Doxygen y diagramas SVG. Los archivos aparecen en **Actions → Build → Artifacts** cuando termina la ejecución.
