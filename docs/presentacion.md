---
marp: true
theme: gaia
paginate: true
---

# Untitled Bomberman Game

Proyecto final de Programación Orientada a Objetos

Laberintos generados, mejoras por cartas y dificultad adaptativa en Java y Scala.

---

## Una partida

1. El jugador elige un perfil y entra al laberinto.
2. Usa bombas para destruir cajas y derrotar a los enemigos.
3. Cruza la salida y elige una de tres mejoras.
4. El siguiente nivel adapta el mapa. Al morir, la partida se guarda.

---

## Qué decide el siguiente nivel

| Vida perdida | Tiempo activo | Generador |
| --- | --- | --- |
| 2 o más | Cualquiera | Recuperación · Java |
| Menos de 2 | Menos de 45 s | Desafío · Scala |
| Menos de 2 | 45 s o más | Equilibrado · Java |

Los umbrales se leen del JSON. Reflection carga la clase y verifica el contrato Java.

---

## Dónde se aplica POO

- **Composición.** `Run` contiene la arena, los enemigos y el estado de la partida.
- **Herencia.** `Patrol` y `Chaser` implementan decisiones distintas desde la clase abstracta `Enemy`.
- **Polimorfismo.** Los tres generadores se ejecutan con `Strategy<LevelRequest, LevelLayout>`.
- **Encapsulamiento.** `Arena` administra sus celdas y el repositorio controla sus consultas.

---

## Demostración

Crea un perfil, juega y pierde para consultar el historial. Edita su alias y elimina un perfil de prueba.

Ejecuta los tres escenarios de generación. Cambia un umbral en el JSON, reinicia y repite el escenario para mostrar el efecto.

---

## Verificación

- Maven Wrapper fija la versión de Maven.
- JUnit comprueba la partida, los generadores, el JSON y el CRUD.
- JaCoCo genera el informe agregado de cobertura.
- Doxygen y PlantUML documentan el código y sus relaciones.
