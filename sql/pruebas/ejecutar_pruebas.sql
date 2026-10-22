-- pruebas/ejecutar_pruebas.sql: ejecuta las pruebas 01 a 07 del informe y la verificacion final.
-- Cada prueba deja sus datos con ROLLBACK TO SAVEPOINT. Ejecutar como SMCFT desde la carpeta sql/pruebas/.
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT FAILURE ROLLBACK
@@01_trigger_plazo.sql
@@02_triggers_auditoria.sql
@@03_package.sql
@@04_procedimiento_con_parametros.sql
@@05_procedimiento_y_funcion_sin_parametros.sql
@@06_funcion_con_parametros.sql
@@07_validacion_privada.sql
@@08_verificar_compilacion.sql
