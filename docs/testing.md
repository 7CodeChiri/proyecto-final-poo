# Pruebas y cobertura

Ejecuta `./mvnw clean verify` en Linux/macOS o `.\\mvnw.cmd clean verify` en Windows.

| Suite | Comprobación |
| --- | --- |
| GameTest | Selección de los tres generadores, acceso a la salida, elección única y daño de bomba. |
| IntegrationTest | CRUD en SQLite, persistencia al reabrir, eliminación en cascada, configuración válida e inválida. |

Surefire escribe los resultados en `launcher/target/surefire-reports`. JaCoCo genera un informe agregado en `launcher/target/site/jacoco-aggregate/index.html`. El informe HTML muestra cobertura de líneas y ramas por paquete. La ejecución de GitHub Actions adjunta ambos informes con sus resultados reales.
