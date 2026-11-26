# Arquitectura

`launcher` inicia Swing, carga la configuración y abre el almacén JDBC. `ProfilesPanel` muestra la lista de perfiles y su historial. Al jugar, `GameWindow` mantiene activo un perfil y `Run` controla el tablero, enemigos, bombas, cartas y estadísticas. El historial se escribe una vez al entrar en la pantalla de derrota.

`domain` define las entidades y los tipos de intercambio. `plugin-api` define el contrato Java. `adaptive-core` aplica los umbrales del JSON y carga por Reflection una clase que cumple el contrato. Los módulos `plugins-java` y `plugins-scala` aportan las tres implementaciones. `persistence` usa SQLite y sentencias preparadas. El esquema define la eliminación en cascada del historial al borrar un perfil.

## Diagramas PlantUML

- [Diagrama de clases](diagrams/classes.puml)
- [Secuencia de avance y guardado](diagrams/progression.puml)

Para generar SVG con PlantUML: `plantuml -tsvg docs/diagrams/*.puml`. Para generar la documentación de código con Doxygen y Graphviz: `doxygen Doxyfile`.
