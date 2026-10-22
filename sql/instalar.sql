-- instalar.sql: instala el esquema SMCFT completo en orden.
-- Ejecutar conectado como SMCFT desde la carpeta sql/:
--   SQL Developer: abrir este archivo y ejecutar como script (F5)
--   SQLcl / SQL*Plus: @instalar.sql
SET SERVEROUTPUT ON SIZE UNLIMITED
SET DEFINE OFF
WHENEVER SQLERROR EXIT FAILURE ROLLBACK

PROMPT === Tablas y secuencias ===
@@01_tablas.sql
@@02_secuencias.sql

PROMPT === Package PKG_GESTION_FRONTERA ===
@@package/01_spec.sql
@@package/02_body.sql

PROMPT === Procedimientos y funciones independientes ===
@@procedimientos/01_con_parametros.sql
@@procedimientos/02_sin_parametros.sql
@@funciones/01_con_parametros.sql
@@funciones/02_sin_parametros.sql

PROMPT === Triggers ===
@@triggers/01_plazo_fila.sql
@@triggers/02_auditoria_fila.sql
@@triggers/03_auditoria_sentencia.sql

PROMPT === Datos de prueba ===
@@03_datos_prueba.sql

PROMPT === Verificacion ===
@@pruebas/08_verificar_compilacion.sql
