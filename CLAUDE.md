# Instrucciones de colaboración — LaLiga Stats (TFC DAM)

Proyecto individual de Yeray Morán Martín (CFGS DAM, IES Fray Diego Tadeo González, curso 2026-27).
Entrega: **4 de diciembre de 2026**. Defensa: **10 u 11 de diciembre de 2026**.

## Reglas de trabajo

- Responder siempre en español, explicando cada cambio para que el alumno pueda defenderlo sin ayuda.
- Rutina por funcionalidad: QUÉ VAMOS A HACER · POR QUÉ · ARCHIVOS · QUÉ NECESITO ENTENDER → cambios →
  QUÉ HAS CAMBIADO · CÓMO FUNCIONA · CÓMO LO PRUEBO EN INTELLIJ · CÓMO LO GUARDAMOS EN GIT · QUÉ HEMOS CONSEGUIDO · QUÉ QUEDA PENDIENTE.
- Reproducir un problema antes de corregirlo. No dar nada por terminado sin ejecutarlo.
- No inventar datos, pruebas, capturas ni resultados. Estadística ausente = `NULL` = "N/D", nunca 0.
- Predicciones: solo con datos anteriores a la fecha del partido (sin fuga de información futura).
- La documentación distingue terminado / parcial / pendiente / descartado.

## Estilo de código (importante para la rúbrica y la defensa)

El tutor ha advertido sobre el uso excesivo de IA, y la rúbrica pone un **0 automático** si el alumno no puede
explicar el código generado con IA. Por eso el código debe ser el que escribiría un estudiante de DAM que lo entiende:

- Comentarios en español: Javadoc en cada clase y método público (qué hace, qué recibe, qué devuelve) y
  comentarios de bloque explicando el *porqué* de las partes no evidentes. No comentar líneas triviales.
- Construcciones vistas en el ciclo: clases, `if`/`for`, colecciones, excepciones, JDBC con `PreparedStatement`.
  Se pueden usar `record`, lambdas y streams sencillos, pero explicándolos en un comentario la primera vez.
- Una instrucción por línea, llaves siempre, métodos cortos con nombres descriptivos en español.
- Nada de trucos "inteligentes" ni librerías extra si se puede hacer con lo visto en clase.
- Tras cada cambio, explicar el código al alumno y proponerle una pregunta de comprobación.
- El uso de IA se declara en la memoria (no ocultarlo): lo que se evalúa es que el alumno lo entienda.

## Git

- `main` debe estar siempre estable. Cada funcionalidad en una rama (`feat/...`, `fix/...`, `docs/...`, `chore/...`) y Pull Request.
- Commits convencionales en español: `feat:`, `fix:`, `test:`, `docs:`, `chore:`, `refactor:`.
- Nunca reescribir historial, forzar push ni subir credenciales, `.env` o los CSV de `data/`.
- Pedir aprobación antes de fusionar, borrar ramas o cualquier acción destructiva.

## Tecnología

Java 21 · JavaFX 21 · Maven (usar `mvnw.cmd`) · MySQL 26.10 · JDBC (Connector/J) · Commons CSV · JUnit 5 · GitHub Actions.

- Ejecutar: configuración **LaLiga App** (clase `Launcher`) o `.\mvnw.cmd javafx:run`.
- Pruebas: configuración **Tests (mvn clean test)** o `.\mvnw.cmd clean test`.
- Conexión: variables de entorno `LALIGA_DB_URL`, `LALIGA_DB_USER`, `LALIGA_DB_PASSWORD` (ver `.env.example`).

## Arquitectura

`ui` (JavaFX) → `service` (lógica, cálculos, modelos estadísticos) → `repository` (JDBC, SQL parametrizado) → MySQL.
`importer` lee CSV de Football-Data (`data/`, ignorado por Git). `model` contiene records inmutables.
Mantener las clases sencillas y explicables; no añadir patrones complejos sin justificarlo.

## Documentos clave

- `docs/ESTADO_Y_PLAN.md` — estado real y planificación.
- `docs/MATRIZ_RUBRICA.md` — cumplimiento de la rúbrica (pesos pendientes de confirmar).
- `docs/INCIDENCIAS.md` — registro de incidencias con evidencias.
- `docs/FUENTES_Y_PRUEBAS.md` — fuentes de datos y casos de prueba.
