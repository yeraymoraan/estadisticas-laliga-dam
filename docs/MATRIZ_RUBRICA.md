# Matriz de cumplimiento de la rúbrica

Fuente: `Rubrica Definitiva Proyecto Fin de Ciclo DAM-5.xlsx` (facilitada por el centro, revisada el 13/11/2026).
Estado: ✅ cumplido y verificado · 🟡 parcial · ❌ pendiente · ❓ por confirmar.

> **Aviso:** el Excel numera los criterios desde el 3 y los pesos de los criterios suman **87 %**, aunque los bloques
> indican 10 % + 60 % + 30 % = 100 %. Faltan los criterios 1 y 2 (13 %), que probablemente pertenecen a la memoria
> (bloque del 10 % con un solo criterio del 7 %) y a la defensa (bloque del 30 % con criterios que suman 20 %).
> **Hay que preguntar al tutor qué evalúan los criterios 1 y 2.**

## Criterios y estado

| Código | Criterio | RA | Peso | Estado | Evidencia actual | Trabajo pendiente | Prioridad |
|---|---|---|---:|---|---|---|---|
| 1.1 | Manual de despliegue/entorno y manual de usuario, con capturas | RA3 | 7 % | ❌ | `docs/INSTRUCCIONES_WINDOWS.md` (borrador sin capturas) | Manual de instalación y manual de usuario completos, con capturas paso a paso | Alta |
| 2.1 | Equilibrio entre estabilidad y reto técnico / requisitos | RA1 | 15 % | 🟡 | La interfaz arranca; importación de CSV reales | Predicción + simulación Monte Carlo (reto técnico), sin fallos en las funciones clave | Muy alta |
| 2.2 | Capas/MVC, modularidad, legibilidad y **comentarios técnicos** | RA2 | 15 % | 🟡 | Paquetes por capas; código comentado (13/11) | Separar `App.java` en pantallas y controladores; mantener los comentarios | Alta |
| 2.3 | BD normalizada, consultas seguras y **gestión de APIs REST** | RA2 | 10 % | 🟡 | Esquema verificado en MySQL 26.10 (PK, 5 FK, 3 CHECK, UNIQUE); `PreparedStatement`; usuario con permisos mínimos; reimportación sin duplicados | Diagrama E-R; **consumir una API REST** (el nivel 10 la menciona expresamente) | Alta |
| 2.4 | Git con historial limpio y ramas + ejecutable/instalador | RA3 | 10 % | 🟡 | Ramas, PR #1, commits semánticos, GitHub Actions | Seguir con ramas/PR; instalador o ejecutable para Windows (jpackage) | Media |
| 2.5 | Interfaz responsiva, usabilidad y accesibilidad (contrastes WCAG) | RA2 | 5 % | ❌ | Ventana básica | Diseño que se adapte al tamaño de ventana, contraste suficiente, navegación por teclado | Media |
| 2.6 | Pruebas automatizadas/unitarias/sistema, matriz de casos y evidencias | RA4 | 5 % | 🟡 | 3 pruebas JUnit en verde | Matriz de casos de prueba, pruebas de sistema con capturas, pruebas del modelo | Alta |
| 3.1 | Estructura, tiempo y material audiovisual de la exposición | RA4 | 5 % | ❌ | — | Presentación y ensayo cronometrado | Final |
| 3.2 | Ejecución en vivo y solvencia ante imprevistos | RA4 | 7 % | ❌ | — | Guion de demostración, datos preparados, plan B si falla MySQL | Final |
| 3.3 | Defensa razonada de decisiones técnicas, arquitectura y código | RA4 | 8 % | ❌ | — | Preguntas de ensayo; el alumno explica cada clase sin ayuda | Continua |
| ? | Criterios 1 y 2 (no aparecen en el Excel) | ? | 13 % ❓ | ❓ | — | Preguntar al tutor | Alta |

## Condiciones que suponen un cero automático

1. Plagio total o parcial **no referenciado** → citar fuentes de datos y herramientas, incluida la IA.
2. Falta de correspondencia con el anteproyecto aprobado → documentar la ampliación (mensaje del tutor que pide
   "alguna ventana donde se pueda simular cómo terminará la liga" y documento de cambios del 16/10/2026).
3. **No poder demostrar la autoría o no saber explicar fragmentos generados con IA** → el alumno debe entender
   y poder explicar cada clase. Por eso el código se escribe sencillo y comentado, y se repasa tras cada cambio.

## Penalizaciones

- Formato de la memoria (no usar la plantilla, cabeceras, paginación, índice general o de figuras/tablas): hasta −1 / −1,5 puntos.
- Ortografía y redacción: **−0,5 por cada falta**, hasta −3 puntos. Revisar tildes en la memoria y en la interfaz.

## Plantilla de la memoria (`Plantilla Proyectos desarrollo DAM.docx`)

Apartados: licencia (CC BY-SA 3.0 ES) · resumen (máx. 1 página) · índice de contenido · índice de figuras ·
objetivos · estudios (presentación, conceptos básicos) · necesidades del sector productivo · diseño del proyecto
(requisitos, diseño del sistema: datos, arquitectura, interfaz, componentes y despliegue; implementación; pruebas
de unidad, integración, alfa y beta) · planificación · procedimientos de control y evaluación · referencias · anexos.

Estilo: formas impersonales ("se instala", no "instalamos"), sin voz pasiva, párrafos cortos, viñetas, título al pie
en figuras y tablas, PDF. Entrega en una carpeta comprimida con el nombre `CICLO-CURSO-TITULO DEL PROYECTO`.

## Requisitos funcionales de la primera entrega (16/10/2026)

| RF | Descripción | Estado | Observación |
|---|---|---|---|
| RF-01 | CRUD de equipos | ❌ | Solo se crean equipos al importar. El tutor pide datos "que se introducen a través de la aplicación" |
| RF-02 | Registrar, editar y eliminar partidos con validaciones | 🟡 | Validaciones en `PartidoHistorico` y en SQL; falta el formulario |
| RF-03 | Filtrar por equipo, fecha y local/visitante | 🟡 | Solo filtro por texto de equipo |
| RF-04 | Estadísticas calculadas | 🟡 | Clasificación básica; faltan medias por equipo con tamaño de muestra |
| RF-05 | Liga con partidos finalizados y pendientes | ❌ | La temporada 2026/27 real (119 partidos jugados) cumple este papel |
| RF-06 | Simular pendientes y calcular clasificación | ❌ | Monte Carlo previsto. **Lo pide expresamente el tutor** |

## Preguntas para el tutor

1. ¿Qué evalúan los criterios 1 y 2 de la rúbrica (13 % que no aparece en el Excel)?
2. Criterio 2.3: en una aplicación de escritorio, ¿basta con consumir una API REST externa (p. ej. el calendario de LaLiga)?
3. ¿Qué formato de distribución se espera (JAR, instalador, carpeta comprimida)?
4. ¿Cuánto dura la exposición?
5. ¿Las tareas del canal "General" incluyen entregas parciales antes del 4 de diciembre?
6. Cambio de IDE de Eclipse a IntelliJ IDEA (no afecta al lenguaje ni a Maven).
7. Su mensaje sugiere "utilizar por ejemplo IA" en la simulación: ¿le parece válido un modelo estadístico explicable (Poisson + Monte Carlo)?
