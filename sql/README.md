# Scripts SQL del esquema SMCFT

Scripts del informe EP2 corregidos y ordenados. Se probaron en Oracle Database 23ai Free con ORDS 26.3: las 7 pruebas pasan y no quedan objetos inválidos.

## 1. Esquema (conectado como SMCFT)

| Orden | Script | Contenido |
|---|---|---|
| 1 | `01_tablas.sql` | 27 tablas y sus llaves foráneas |
| 2 | `02_secuencias.sql` | Secuencias |
| 3 | `package/01_spec.sql`, `package/02_body.sql` | `PKG_GESTION_FRONTERA` |
| 4 | `procedimientos/*.sql` | `SP_REPORTE_CRUCES`, `SP_ACTUALIZAR_PLAZOS` |
| 5 | `funciones/*.sql` | `FN_VALOR_DECLARADO`, `FN_TOTAL_PLAZOS_VENCIDOS` |
| 6 | `triggers/*.sql` | `TRG_CRUCE_PLAZO_AI`, `TRG_DOCUMENTO_AUDIT`, `TRG_DOCUMENTO_SENTENCIA` |
| 7 | `03_datos_prueba.sql` | Datos base (usuario técnico 1, pasos, vehículos…) y sincronización de secuencias |

`instalar.sql` ejecuta todo en ese orden. En SQL Developer, abra el archivo desde esta carpeta y ejecútelo como script (F5).

Pruebas: `pruebas/ejecutar_pruebas.sql` corre las pruebas 01 a 07 del informe y la verificación de compilación. Cada prueba revierte sus cambios.

## 2. API REST para la app (opcional, conectado como SMCFT)

La app Android se conecta a Oracle mediante ORDS, que viene incluido en Oracle Autonomous Database (Always Free).

| Orden | Script | Qué hace |
|---|---|---|
| 1 | `ords/01_habilitar_esquema.sql` | Publica el esquema en `https://<host>/ords/smcft/` |
| 2 | `ords/02_api_rest.sql` | Crea el módulo `smcft.api`: consultas y procesos que llaman al package, a los procedimientos y a las funciones |
| 3 | `ords/03_seguridad_oauth.sql` | Protege la API con OAuth y muestra el **client_id** y el **client_secret** (solo una vez) |

Después, en la app: **Más → Conexión → Oracle Cloud (ORDS)**. Ingrese la URL `https://<host>/ords/smcft/`, el client_id y el client_secret, toque **Probar conexión** y luego **Guardar y usar**.

La URL del host aparece en Oracle Cloud: Autonomous Database → *Tool configuration* → *Oracle REST Data Services* (o *Database Actions*), y se reemplaza el final `/ords/` por `/ords/smcft/`.
