# SMCFT – App Android en Kotlin

Aplicación Android (Kotlin + Jetpack Compose) del **Sistema de Monitoreo y Control Fronterizo Terrestre (SMCFT)**,
construida a partir del informe *Taller de Base de Datos – Evaluación Parcial N°2*
(Jael Reyes, Benjamin Riquelme Pozo, Aileen Oyaneder).

La app lleva a Kotlin la capa de procesamiento PL/SQL del informe: el package `PKG_GESTION_FRONTERA`,
los procedimientos y funciones independientes y los tres triggers, sobre una base de datos local
con el mismo modelo de tablas del esquema `SMCFT`.

## Funcionalidades

| Pantalla | Qué hace | Objeto del informe |
|---|---|---|
| Inicio | Resumen del sistema y acceso rápido | `FN_TOTAL_PLAZOS_VENCIDOS`, `SP_ACTUALIZAR_PLAZOS` |
| Registrar cruce | Cruce peatonal o vehicular con sus viajeros y conductor; valida vehículo, RUT y autorización de menores | `sp_registrar_cruce` |
| Cruces y declaraciones | Detalle de cada cruce, ítems declarados (Aduana/SAG) y valor en USD | `FN_VALOR_DECLARADO` |
| Plazos de admisión | Plazos de 90 días (diplomáticos) o 180 días, días restantes y vencidos | `TRG_CRUCE_PLAZO_AI`, `SP_ACTUALIZAR_PLAZOS` |
| Reportes | Cruces, viajeros y USD declarados por paso en un rango de fechas | `SP_REPORTE_CRUCES` + `validar_rango` (privado) |
| Documentos | Alta, cambio de estado, actualización masiva y eliminación | `TRG_DOCUMENTO_AUDIT`, `TRG_DOCUMENTO_SENTENCIA` |
| Auditoría | Auditoría por documento (fila) y por instrucción (sentencia) | tablas `AUDITORIA` y `AUDITORIA_DETALLE` |
| Personas y vehículos | Registro de viajeros, vehículos y autorizaciones notariales de menores | `PERSONA`, `VEHICULO`, `AUTORIZACION_MENOR` |

Los datos iniciales son los de `03_datos_prueba.sql`: Ana, Carlos, Sofía (menor con autorización), Claudia
(usuario técnico 1), los pasos Los Libertadores y Cardenal Samoré y los vehículos `ABCD12` (particular) y
`XX999Z` (diplomático).

## Estructura

```
core/   Módulo Kotlin puro (sin Android): modelo, base de datos, package, triggers y pruebas
  model/Modelos.kt               Tablas del esquema SMCFT (01_tablas.sql)
  db/BaseDatos.kt                Secuencias, SAVEPOINT/ROLLBACK/COMMIT, PK/UNIQUE/FK/CHECK
  db/Triggers.kt                 TRG_CRUCE_PLAZO_AI, TRG_DOCUMENTO_AUDIT, TRG_DOCUMENTO_SENTENCIA
  pkg/GestionFrontera.kt         PKG_GESTION_FRONTERA (público + validarRango privado)
  ObjetosIndependientes.kt       SP_REPORTE_CRUCES, SP_ACTUALIZAR_PLAZOS, FN_VALOR_DECLARADO, FN_TOTAL_PLAZOS_VENCIDOS
  DatosPrueba.kt                 03_datos_prueba.sql
  test/PruebasInformeTest.kt     Pruebas 01 a 07 del informe + casos de error
app/    App Android con Jetpack Compose (Material 3)
```

### Cómo se reproduce el comportamiento de Oracle

- **Procedimientos y funciones**: métodos de `GestionFrontera`. Las versiones independientes solo delegan
  en el package, así la lógica existe una sola vez.
- **Elementos públicos y privados**: lo público del package son los miembros públicos de la clase;
  `validarRango` es `private` y lanza `-20020`.
- **Errores**: `SmcftException` tiene los mismos códigos que `RAISE_APPLICATION_ERROR`
  (`-20001`, `-20002`, `-20004`, `-20010`, `-20020`, …).
- **Triggers**: `BaseDatos` los ejecuta solos en cada `INSERT`/`UPDATE`/`DELETE`, aunque el dato no pase por
  el package. Los de fila corren una vez por fila; el de sentencia, una vez por instrucción.
- **Transacciones**: las tablas son inmutables, así que un `SAVEPOINT` guarda el estado actual y un
  `ROLLBACK TO` vuelve a él. Cada acción de la app hace `COMMIT` si termina bien, y lo confirmado se guarda
  en un JSON en el almacenamiento interno, o `ROLLBACK` si falla.

## Cómo ejecutar

### App Android
1. Abrir la carpeta del proyecto en **Android Studio** (Ladybug o más reciente).
2. Esperar la sincronización de Gradle (usa el wrapper incluido, Gradle 8.11.1, AGP 8.7.3 y Kotlin 2.0.21).
3. Ejecutar la configuración `app` en un emulador o dispositivo con Android 8.0 (API 26) o superior.

### Pruebas (sin emulador)
```bash
./gradlew :core:test
```

| Prueba | Qué verifica |
|---|---|
| 01 Trigger de plazo | Un INSERT directo de una SALIDA con vehículo crea un plazo de 180 días (90 si es diplomático) |
| 02 Auditoría | Un UPDATE de 2 documentos deja 2 detalles de fila y 1 auditoría de sentencia |
| 03 Package público | `sp_registrar_cruce` registra el cruce con su viajero y su rol |
| 04 Procedimiento con parámetros | `SP_REPORTE_CRUCES` genera un reporte con sus detalles |
| 05 Objetos sin parámetros | `FN_TOTAL_PLAZOS_VENCIDOS` cuenta el plazo vencido y `SP_ACTUALIZAR_PLAZOS` lo marca VENCIDO |
| 06 Función con parámetros | `FN_VALOR_DECLARADO` devuelve USD 450 |
| 07 Validación privada | Un rango invertido se rechaza con `-20020` |
| Casos de error | Menor sin autorización (-20002), vehículo inexistente (-20004), RUT no registrado (-20010) |

## Diferencias con la versión Oracle

- La app no se conecta a Oracle Cloud: trabaja con una base local que tiene las mismas tablas y reglas.
  Conectarla a Oracle requeriría un backend (por ejemplo, una API REST) entre la app y la base.
- `sp_reporte_cruces_por_paso` lanza `-20005` cuando no hay pasos activos. En el PL/SQL ese manejador
  existe, pero nunca se alcanza.
