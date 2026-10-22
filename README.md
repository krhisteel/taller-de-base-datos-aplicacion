# SMCFT – App Android en Kotlin

Aplicación Android (Kotlin + Jetpack Compose) del **Sistema de Monitoreo y Control Fronterizo Terrestre (SMCFT)**,
construida a partir del informe *Taller de Base de Datos – Evaluación Parcial N°2*
(Jael Reyes, Benjamin Riquelme Pozo, Aileen Oyaneder).

La app lleva a Kotlin la capa de procesamiento PL/SQL del informe: el package `PKG_GESTION_FRONTERA`,
los procedimientos y funciones independientes y los tres triggers, sobre una base de datos local
con el mismo modelo de tablas del esquema `SMCFT`.

## Inicio de sesión y tipos de usuario

La app pide iniciar sesión. Cada tipo de usuario ve solo las acciones de su perfil (los permisos están en
`core/.../seguridad/Seguridad.kt`). Todos pueden consultar cruces, plazos y documentos.

| Perfil | Correo | Contraseña | Puede |
|---|---|---|---|
| Administrador | admin@smcft.cl | Admin2026 | Todo, incluidos usuarios y conexión |
| Oficial PDI | pdi@smcft.cl | Pdi2026 | Registrar cruces, viajeros, autorizaciones y documentos |
| Funcionario de Aduana | aduana@smcft.cl | Aduana2026 | Declaraciones de Aduana, vehículos, plazos y reportes |
| Inspector SAG | sag@smcft.cl | Sag2026 | Declaraciones SAG |
| Supervisor | supervisor@smcft.cl | Super2026 | Reportes y auditoría |

Las contraseñas se guardan como hash SHA-256 en `USUARIO.PASSWORD_HASH` y cada intento queda en `LOGIN_ATTEMPT`
(procedimiento `SP_INICIAR_SESION` en `sql/04_usuarios_roles.sql`).

## Funcionalidades

La app tiene una barra inferior con cinco secciones y pantallas adicionales en **Más**:

| Pantalla | Qué hace | Objeto del informe |
|---|---|---|
| Inicio | Panel con indicadores, alertas de plazos, gráfico de cruces por paso y últimos cruces | `FN_TOTAL_PLAZOS_VENCIDOS`, `SP_ACTUALIZAR_PLAZOS` |
| Cruces | Búsqueda, filtros y detalle de cada cruce (viajeros, ítems declarados) con formulario de declaración | `FN_VALOR_DECLARADO` |
| Registrar cruce | Formulario guiado en 4 pasos con vista previa de lo que harán el package y el trigger | `sp_registrar_cruce`, `TRG_CRUCE_PLAZO_AI` |
| Plazos | Plazos de 90/180 días con barra de avance, filtros y actualización de estados | `TRG_CRUCE_PLAZO_AI`, `SP_ACTUALIZAR_PLAZOS` |
| Documentos | Alta, cambio de estado, eliminación y actualización masiva | `TRG_DOCUMENTO_AUDIT`, `TRG_DOCUMENTO_SENTENCIA` |
| Reportes | Rangos rápidos o calendario y gráficos por paso | `SP_REPORTE_CRUCES` + `validar_rango` |
| Auditoría | Línea de tiempo de los cambios, por documento (fila) y por instrucción (sentencia) | `AUDITORIA`, `AUDITORIA_DETALLE` |
| Viajeros / Vehículos | Registro con validación de RUT y patente, y autorizaciones de menores | `PERSONA`, `VEHICULO`, `AUTORIZACION_MENOR` |
| Usuarios y accesos | Cuentas con su perfil y registro de inicios de sesión (solo administrador) | `USUARIO`, `ROL`, `USUARIO_ROL`, `LOGIN_ATTEMPT` |
| Mi cuenta | Perfil del usuario y lista de lo que puede hacer | — |
| Conexión | Elegir entre modo demostración y Oracle Cloud (ORDS) (solo administrador) | — |

Diseño Material 3 con paleta institucional (claro y oscuro), ícono propio, botones consistentes, animaciones y
"deslizar para actualizar". Cada pantalla tiene una descripción y un botón **?** que explica cómo se usa.

### Modos de datos

- **Modo demostración** (por defecto): los datos se guardan en el teléfono con las mismas reglas del informe. Funciona sin internet.
- **Oracle Cloud**: la app usa el esquema SMCFT real mediante ORDS. El package, los procedimientos, las funciones y los triggers se ejecutan en Oracle. Ver [`sql/README.md`](sql/README.md).

## Estructura

```
sql/    Scripts Oracle del informe (tablas, package, procedimientos, funciones, triggers, pruebas) y API ORDS
core/   Módulo Kotlin puro (sin Android): modelo, base de datos, package, triggers y pruebas
  repo/SmcftRepositorio.kt       Operaciones de la app; RepositorioLocal (demo) y RepositorioOracle (ORDS)
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

## Notas

- `sp_reporte_cruces_por_paso` lanza `-20005` cuando no hay pasos activos. En el PL/SQL ese manejador
  existe, pero nunca se alcanza.
