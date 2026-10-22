-- ords/02_api_rest.sql: modulo REST "smcft.api" que usa la app Android.
-- Ejecutar conectado como SMCFT, despues de 01_habilitar_esquema.sql. Se puede volver a ejecutar:
-- ORDS.DEFINE_MODULE reemplaza el modulo completo.
--
-- Las consultas (GET) devuelven filas en JSON. Los procesos (POST/PUT/DELETE) llaman a los objetos
-- PL/SQL del informe y hacen COMMIT. Si hay un error hacen ROLLBACK y devuelven
-- {"error_code": SQLCODE, "error_msg": SQLERRM}, por ejemplo -20002 si un menor no tiene autorizacion.
--
-- URL base: https://<host>/ords/smcft/api/
SET DEFINE OFF

DECLARE
    c_modulo CONSTANT VARCHAR2(30) := 'smcft.api';

    PROCEDURE plantilla(p_patron VARCHAR2) IS
    BEGIN
        ORDS.DEFINE_TEMPLATE(p_module_name => c_modulo, p_pattern => p_patron);
    END;

    -- GET que devuelve una lista ({"items": [...]}) o, con p_una_fila, un solo objeto.
    PROCEDURE consulta(p_patron VARCHAR2, p_sql VARCHAR2, p_una_fila BOOLEAN DEFAULT FALSE) IS
    BEGIN
        plantilla(p_patron);
        ORDS.DEFINE_HANDLER(
            p_module_name    => c_modulo,
            p_pattern        => p_patron,
            p_method         => 'GET',
            p_source_type    => CASE WHEN p_una_fila THEN ORDS.source_type_query_one_row
                                     ELSE ORDS.source_type_collection_feed END,
            p_items_per_page => 0,
            p_source         => p_sql);
    END;

    PROCEDURE salida(p_patron VARCHAR2, p_metodo VARCHAR2, p_nombre VARCHAR2, p_tipo VARCHAR2) IS
    BEGIN
        ORDS.DEFINE_PARAMETER(
            p_module_name        => c_modulo,
            p_pattern            => p_patron,
            p_method             => p_metodo,
            p_name               => p_nombre,
            p_bind_variable_name => p_nombre,
            p_source_type        => 'RESPONSE',
            p_param_type         => p_tipo,
            p_access_method      => 'OUT');
    END;

    -- Bloque PL/SQL transaccional: COMMIT si termina bien; ROLLBACK y error_code/error_msg si falla.
    -- p_resultado es el parametro de salida con el ID o la cantidad de filas.
    PROCEDURE proceso(p_patron VARCHAR2, p_metodo VARCHAR2, p_declaraciones VARCHAR2, p_cuerpo VARCHAR2,
                      p_resultado VARCHAR2) IS
    BEGIN
        ORDS.DEFINE_HANDLER(
            p_module_name => c_modulo,
            p_pattern     => p_patron,
            p_method      => p_metodo,
            p_source_type => ORDS.source_type_plsql,
            p_source      =>
                'DECLARE ' || p_declaraciones || CHR(10) ||
                'BEGIN ' || CHR(10) || p_cuerpo || CHR(10) ||
                '    COMMIT;' || CHR(10) ||
                'EXCEPTION WHEN OTHERS THEN' || CHR(10) ||
                '    ROLLBACK;' || CHR(10) ||
                '    :error_code := SQLCODE;' || CHR(10) ||
                '    :error_msg := SQLERRM;' || CHR(10) ||
                'END;');
        salida(p_patron, p_metodo, p_resultado, 'INT');
        salida(p_patron, p_metodo, 'error_code', 'INT');
        salida(p_patron, p_metodo, 'error_msg', 'STRING');
    END;
BEGIN
    ORDS.DEFINE_MODULE(
        p_module_name    => c_modulo,
        p_base_path      => '/api/',
        p_items_per_page => 0,
        p_status         => 'PUBLISHED',
        p_comments       => 'API de la app Android SMCFT');

    -- ======================================================================= consultas

    -- POST auth/login: SP_INICIAR_SESION valida la contrasena y registra LOGIN_ATTEMPT.
    -- El intento se confirma (COMMIT) aunque las credenciales sean incorrectas.
    plantilla('auth/login');
    ORDS.DEFINE_HANDLER(
        p_module_name => c_modulo,
        p_pattern     => 'auth/login',
        p_method      => 'POST',
        p_source_type => ORDS.source_type_plsql,
        p_source      => q'[
DECLARE
    v_id        NUMBER;
    v_nombre    VARCHAR2(201);
    v_rol       VARCHAR2(50);
    v_resultado VARCHAR2(20);
BEGIN
    SMCFT.SP_INICIAR_SESION(:email, :clave, v_id, v_nombre, v_rol, v_resultado);
    COMMIT;
    IF v_resultado = 'OK' THEN
        :id_usuario := v_id;
        :nombre := v_nombre;
        :rol := v_rol;
    ELSIF v_resultado = 'INACTIVO' THEN
        :error_code := -20032;
        :error_msg := 'ORA-20032: El usuario esta desactivado.';
    ELSE
        :error_code := -20031;
        :error_msg := 'ORA-20031: Correo o contrasena incorrectos.';
    END IF;
EXCEPTION WHEN OTHERS THEN
    ROLLBACK;
    :error_code := SQLCODE;
    :error_msg := SQLERRM;
END;]');
    salida('auth/login', 'POST', 'id_usuario', 'INT');
    salida('auth/login', 'POST', 'nombre', 'STRING');
    salida('auth/login', 'POST', 'rol', 'STRING');
    salida('auth/login', 'POST', 'error_code', 'INT');
    salida('auth/login', 'POST', 'error_msg', 'STRING');

    consulta('usuarios', q'[
        SELECT u.ID_USUARIO AS id_usuario, p.NOMBRES || ' ' || p.APELLIDOS AS nombre, u.EMAIL AS email,
               (SELECT MIN(r.NOMBRE_ROL) FROM SMCFT.USUARIO_ROL ur JOIN SMCFT.ROL r ON r.ID_ROL = ur.ID_ROL
                 WHERE ur.ID_USUARIO = u.ID_USUARIO AND ur.ACTIVO = 'S') AS rol,
               u.ACTIVO AS activo,
               (SELECT TO_CHAR(MAX(l.FECHA_INTENTO), 'YYYY-MM-DD"T"HH24:MI:SS') FROM SMCFT.LOGIN_ATTEMPT l
                 WHERE l.ID_USUARIO = u.ID_USUARIO AND l.EXITOSO = 'S') AS ultimo_acceso
          FROM SMCFT.USUARIO u
          JOIN SMCFT.PERSONA p ON p.ID_PERSONA = u.ID_PERSONA
         ORDER BY p.NOMBRES]');

    consulta('accesos', q'[
        SELECT l.ID_INTENTO AS id_intento, u.EMAIL AS email,
               CASE WHEN p.ID_PERSONA IS NOT NULL THEN p.NOMBRES || ' ' || p.APELLIDOS END AS nombre,
               l.EXITOSO AS exitoso, TO_CHAR(l.FECHA_INTENTO, 'YYYY-MM-DD"T"HH24:MI:SS') AS fecha_intento
          FROM SMCFT.LOGIN_ATTEMPT l
          LEFT JOIN SMCFT.USUARIO u ON u.ID_USUARIO = l.ID_USUARIO
          LEFT JOIN SMCFT.PERSONA p ON p.ID_PERSONA = u.ID_PERSONA
         ORDER BY l.ID_INTENTO DESC
         FETCH FIRST 50 ROWS ONLY]');

    consulta('resumen', q'[
        SELECT (SELECT COUNT(*) FROM SMCFT.PERSONA) AS personas,
               (SELECT COUNT(*) FROM SMCFT.VEHICULO) AS vehiculos,
               (SELECT COUNT(*) FROM SMCFT.CRUCE_FRONTERIZO) AS cruces,
               (SELECT COUNT(*) FROM SMCFT.PLAZO_ADMISION WHERE ESTADO = 'ACTIVO') AS plazos_activos,
               SMCFT.FN_TOTAL_PLAZOS_VENCIDOS() AS plazos_vencidos,
               (SELECT COUNT(*) FROM SMCFT.AUDITORIA) + (SELECT COUNT(*) FROM SMCFT.AUDITORIA_DETALLE)
                   AS registros_auditoria
          FROM DUAL]', p_una_fila => TRUE);

    consulta('pasos', q'[
        SELECT ID_PASO AS id_paso, NOMBRE AS nombre, REGION AS region, ESTADO AS estado
          FROM SMCFT.PASO_FRONTERIZO
         ORDER BY NOMBRE]');

    consulta('categorias', q'[
        SELECT ID_CATEGORIA AS id_categoria, NOMBRE AS nombre
          FROM SMCFT.CATEGORIA_ITEM
         WHERE ACTIVO = 'S'
         ORDER BY NOMBRE]');

    consulta('personas', q'[
        SELECT p.ID_PERSONA AS id_persona, p.RUT AS rut, p.NOMBRES AS nombres, p.APELLIDOS AS apellidos,
               p.NACIONALIDAD AS nacionalidad,
               TO_CHAR(p.FECHA_NACIMIENTO, 'YYYY-MM-DD') AS fecha_nacimiento,
               TRUNC(MONTHS_BETWEEN(SYSDATE, p.FECHA_NACIMIENTO) / 12) AS edad,
               CASE WHEN EXISTS (SELECT 1 FROM SMCFT.AUTORIZACION_MENOR a
                                  WHERE a.ID_MENOR = p.ID_PERSONA AND a.ESTADO = 'VIGENTE')
                    THEN 1 ELSE 0 END AS autorizacion_vigente
          FROM SMCFT.PERSONA p
         ORDER BY p.APELLIDOS, p.NOMBRES]');

    consulta('vehiculos', q'[
        SELECT v.ID_VEHICULO AS id_vehiculo, v.PATENTE AS patente, v.MARCA AS marca, v.MODELO AS modelo,
               v.ANIO AS anio, v.TIPO_VEHICULO AS tipo_vehiculo,
               p.NOMBRES || ' ' || p.APELLIDOS AS propietario
          FROM SMCFT.VEHICULO v
          JOIN SMCFT.PERSONA p ON p.ID_PERSONA = v.ID_PERSONA_PROPIETARIO
         ORDER BY v.PATENTE]');

    -- FN_VALOR_DECLARADO usada dentro de un SELECT, como en el informe.
    consulta('cruces', q'[
        SELECT c.ID_CRUCE AS id_cruce, p.NOMBRE AS nombre_paso, c.TIPO_MOVIMIENTO AS tipo_movimiento,
               TO_CHAR(c.FECHA_HORA, 'YYYY-MM-DD"T"HH24:MI:SS') AS fecha_hora, c.ESTADO AS estado,
               v.PATENTE AS patente,
               (SELECT COUNT(*) FROM SMCFT.DETALLE_CRUCE_PERSONA d WHERE d.ID_CRUCE = c.ID_CRUCE) AS cant_viajeros,
               SMCFT.FN_VALOR_DECLARADO(c.ID_CRUCE) AS valor_declarado
          FROM SMCFT.CRUCE_FRONTERIZO c
          JOIN SMCFT.PASO_FRONTERIZO p ON p.ID_PASO = c.ID_PASO
          LEFT JOIN SMCFT.VEHICULO v ON v.ID_VEHICULO = c.ID_VEHICULO
         ORDER BY c.FECHA_HORA DESC, c.ID_CRUCE DESC]');

    consulta('cruces/:id/viajeros', q'[
        SELECT p.RUT AS rut, p.NOMBRES || ' ' || p.APELLIDOS AS nombre, d.ROL_VIAJERO AS rol
          FROM SMCFT.DETALLE_CRUCE_PERSONA d
          JOIN SMCFT.PERSONA p ON p.ID_PERSONA = d.ID_PERSONA
         WHERE d.ID_CRUCE = :id
         ORDER BY d.ROL_VIAJERO DESC, p.APELLIDOS]');

    consulta('cruces/:id/items', q'[
        SELECT i.DESCRIPCION AS descripcion, i.CANTIDAD AS cantidad, i.VALOR_USD AS valor_usd
          FROM SMCFT.ITEM_DECLARADO i
          JOIN SMCFT.DECLARACION d ON d.ID_DECLARACION = i.ID_DECLARACION
         WHERE d.ID_CRUCE = :id
         ORDER BY i.ID_ITEM]');

    consulta('plazos', q'[
        SELECT pl.ID_PLAZO AS id_plazo, v.PATENTE AS patente, pl.ID_CRUCE_INGRESO AS id_cruce,
               TO_CHAR(pl.FECHA_INGRESO, 'YYYY-MM-DD"T"HH24:MI:SS') AS fecha_ingreso,
               TO_CHAR(pl.FECHA_LIMITE, 'YYYY-MM-DD"T"HH24:MI:SS') AS fecha_limite,
               pl.TIPO AS tipo, pl.ESTADO AS estado,
               TRUNC(CAST(pl.FECHA_LIMITE AS DATE)) - TRUNC(SYSDATE) AS dias_restantes
          FROM SMCFT.PLAZO_ADMISION pl
          JOIN SMCFT.VEHICULO v ON v.ID_VEHICULO = pl.ID_VEHICULO
         ORDER BY pl.FECHA_LIMITE]');

    consulta('reportes', q'[
        SELECT ID_REPORTE AS id_reporte, TITULO AS titulo, ESTADO AS estado
          FROM SMCFT.REPORTE
         ORDER BY ID_REPORTE DESC]');

    consulta('reportes/detalles', q'[
        SELECT ID_REPORTE AS id_reporte, DESCRIPCION AS descripcion, VALOR AS valor, UNIDAD AS unidad,
               CATEGORIA AS categoria
          FROM SMCFT.REPORTE_DETALLE
         ORDER BY ID_REPORTE, ID_DETALLE]');

    consulta('auditorias', q'[
        SELECT ID_AUDITORIA AS id_auditoria, TIPO_AUDITORIA AS tipo_auditoria, DESCRIPCION AS descripcion,
               ESTADO AS estado,
               TO_CHAR(FECHA_INICIO, 'YYYY-MM-DD"T"HH24:MI:SS') AS fecha_inicio,
               TO_CHAR(FECHA_CIERRE, 'YYYY-MM-DD"T"HH24:MI:SS') AS fecha_cierre
          FROM SMCFT.AUDITORIA
         ORDER BY ID_AUDITORIA DESC]');

    consulta('auditorias/detalles', q'[
        SELECT ID_DETALLE AS id_detalle, ID_AUDITORIA AS id_auditoria, ACCION AS accion,
               DESCRIPCION AS descripcion, RESULTADO AS resultado,
               TO_CHAR(FECHA_ACCION, 'YYYY-MM-DD"T"HH24:MI:SS') AS fecha_accion
          FROM SMCFT.AUDITORIA_DETALLE
         ORDER BY ID_DETALLE DESC]');

    -- ======================================================================= procesos del informe

    -- POST cruces: PKG_GESTION_FRONTERA.sp_registrar_cruce. "ruts" llega separado por comas.
    proceso('cruces', 'POST',
        q'[v_ruts SMCFT.PKG_GESTION_FRONTERA.t_ruts_varray := SMCFT.PKG_GESTION_FRONTERA.t_ruts_varray();
           v_id   NUMBER;]',
        q'[
    FOR r IN (SELECT TRIM(REGEXP_SUBSTR(:ruts, '[^,]+', 1, LEVEL)) AS rut
                FROM DUAL
             CONNECT BY REGEXP_SUBSTR(:ruts, '[^,]+', 1, LEVEL) IS NOT NULL) LOOP
        v_ruts.EXTEND;
        v_ruts(v_ruts.COUNT) := r.rut;
    END LOOP;
    SMCFT.PKG_GESTION_FRONTERA.sp_registrar_cruce(
        p_id_paso         => :id_paso,
        p_patente         => :patente,
        p_tipo_movimiento => :tipo_movimiento,
        p_ruts_viajeros   => v_ruts,
        p_rut_conductor   => :rut_conductor,
        p_id_cruce_out    => v_id);
    :id_cruce := v_id;]',
        'id_cruce');

    -- POST procesos/actualizar-plazos: SP_ACTUALIZAR_PLAZOS (procedimiento sin parametros).
    -- Se cuentan antes los plazos que cumplen la misma condicion del UPDATE para informar cuantos cambiaron.
    plantilla('procesos/actualizar-plazos');
    proceso('procesos/actualizar-plazos', 'POST', 'v_n NUMBER;', q'[
    SELECT COUNT(*) INTO v_n
      FROM SMCFT.PLAZO_ADMISION
     WHERE ESTADO = 'ACTIVO' AND FECHA_LIMITE < CAST(TRUNC(SYSDATE) AS TIMESTAMP);
    SMCFT.SP_ACTUALIZAR_PLAZOS;
    :filas := v_n;]',
        'filas');

    -- POST reportes: SP_REPORTE_CRUCES (procedimiento con parametros). Fechas 'YYYY-MM-DD'; "hasta" incluye el dia completo.
    proceso('reportes', 'POST', 'v_id NUMBER;', q'[
    SMCFT.SP_REPORTE_CRUCES(
        TO_DATE(:desde, 'YYYY-MM-DD'),
        TO_DATE(:hasta, 'YYYY-MM-DD') + 86399 / 86400,
        :id_usuario,
        v_id);
    :id_reporte := v_id;]',
        'id_reporte');

    -- ======================================================================= mantenimiento

    plantilla('declaraciones');
    proceso('declaraciones', 'POST', 'v_decl NUMBER;', q'[
    INSERT INTO SMCFT.DECLARACION (ID_DECLARACION, ID_CRUCE, TIPO, FECHA, ESTADO)
    VALUES (SMCFT.SEQ_DECLARACION.NEXTVAL, :id_cruce, :tipo, SYSDATE, 'REGISTRADA')
    RETURNING ID_DECLARACION INTO v_decl;
    INSERT INTO SMCFT.ITEM_DECLARADO (ID_ITEM, ID_DECLARACION, ID_CATEGORIA, DESCRIPCION, CANTIDAD, VALOR_USD, APROBADO)
    VALUES (SMCFT.SEQ_ITEM.NEXTVAL, v_decl, :id_categoria, :descripcion, :cantidad,
            TO_NUMBER(:valor_usd, '9999999990D99', 'NLS_NUMERIC_CHARACTERS=''.,'''), 'N');
    :id_declaracion := v_decl;]',
        'id_declaracion');

    -- DOCUMENTO: cada sentencia dispara TRG_DOCUMENTO_AUDIT (fila) y TRG_DOCUMENTO_SENTENCIA (sentencia).
    consulta('documentos', q'[
        SELECT d.ID_DOCUMENTO AS id_documento, d.TIPO_DOCUMENTO AS tipo_documento, d.NUMERO AS numero,
               TO_CHAR(d.FECHA_EMISION, 'YYYY-MM-DD') AS fecha_emision,
               TO_CHAR(d.FECHA_VENCIMIENTO, 'YYYY-MM-DD') AS fecha_vencimiento,
               CASE WHEN d.ID_PERSONA IS NOT NULL THEN p.NOMBRES || ' ' || p.APELLIDOS
                    ELSE 'Vehiculo ' || v.PATENTE END AS titular,
               d.ESTADO AS estado
          FROM SMCFT.DOCUMENTO d
          LEFT JOIN SMCFT.PERSONA p ON p.ID_PERSONA = d.ID_PERSONA
          LEFT JOIN SMCFT.VEHICULO v ON v.ID_VEHICULO = d.ID_VEHICULO
         ORDER BY d.ID_DOCUMENTO]');

    proceso('documentos', 'POST', 'v_id NUMBER;', q'[
    INSERT INTO SMCFT.DOCUMENTO (ID_DOCUMENTO, TIPO_DOCUMENTO, NUMERO, FECHA_EMISION, FECHA_VENCIMIENTO,
                                 ID_PERSONA, ID_VEHICULO, ESTADO)
    VALUES (SMCFT.SEQ_DOCUMENTO.NEXTVAL, :tipo_documento, :numero,
            TO_DATE(:fecha_emision, 'YYYY-MM-DD'), TO_DATE(:fecha_vencimiento, 'YYYY-MM-DD'),
            :id_persona, :id_vehiculo, 'VIGENTE')
    RETURNING ID_DOCUMENTO INTO v_id;
    :id_documento := v_id;]',
        'id_documento');

    plantilla('documentos/:id');
    proceso('documentos/:id', 'PUT', 'v_n NUMBER;', q'[
    UPDATE SMCFT.DOCUMENTO SET ESTADO = :estado WHERE ID_DOCUMENTO = :id;
    v_n := SQL%ROWCOUNT;
    :filas := v_n;]',
        'filas');

    proceso('documentos/:id', 'DELETE', 'v_n NUMBER;', q'[
    DELETE FROM SMCFT.DOCUMENTO WHERE ID_DOCUMENTO = :id;
    v_n := SQL%ROWCOUNT;
    :filas := v_n;]',
        'filas');

    -- Un solo UPDATE sobre varios documentos: varios detalles de fila y una auditoria de sentencia.
    plantilla('procesos/documentos-vencidos');
    proceso('procesos/documentos-vencidos', 'POST', 'v_n NUMBER;', q'[
    UPDATE SMCFT.DOCUMENTO
       SET ESTADO = 'VENCIDO'
     WHERE ESTADO = 'VIGENTE' AND FECHA_VENCIMIENTO < TRUNC(SYSDATE);
    v_n := SQL%ROWCOUNT;
    :filas := v_n;]',
        'filas');

    proceso('personas', 'POST', 'v_id NUMBER;', q'[
    INSERT INTO SMCFT.PERSONA (ID_PERSONA, RUT, NOMBRES, APELLIDOS, NACIONALIDAD, FECHA_NACIMIENTO)
    VALUES (SMCFT.SEQ_PERSONA.NEXTVAL, :rut, :nombres, :apellidos, UPPER(:nacionalidad),
            TO_DATE(:fecha_nacimiento, 'YYYY-MM-DD'))
    RETURNING ID_PERSONA INTO v_id;
    :id_persona := v_id;]',
        'id_persona');

    proceso('vehiculos', 'POST', 'v_id NUMBER;', q'[
    INSERT INTO SMCFT.VEHICULO (ID_VEHICULO, PATENTE, MARCA, MODELO, ANIO, TIPO_VEHICULO,
                                ID_PERSONA_PROPIETARIO, ESTADO)
    VALUES (SMCFT.SEQ_VEHICULO.NEXTVAL, UPPER(:patente), :marca, :modelo, :anio, :tipo_vehiculo,
            :id_propietario, 'ACTIVO')
    RETURNING ID_VEHICULO INTO v_id;
    :id_vehiculo := v_id;]',
        'id_vehiculo');

    -- Documento notarial del menor + AUTORIZACION_MENOR vigente (la exige sp_registrar_cruce).
    plantilla('autorizaciones');
    proceso('autorizaciones', 'POST', 'v_doc NUMBER; v_id NUMBER;', q'[
    INSERT INTO SMCFT.DOCUMENTO (ID_DOCUMENTO, TIPO_DOCUMENTO, NUMERO, FECHA_EMISION, FECHA_VENCIMIENTO,
                                 ID_PERSONA, ID_VEHICULO, ESTADO)
    VALUES (SMCFT.SEQ_DOCUMENTO.NEXTVAL, 'AUTORIZACION_NOTARIAL', :numero_documento, TRUNC(SYSDATE),
            ADD_MONTHS(TRUNC(SYSDATE), 12), :id_menor, NULL, 'VIGENTE')
    RETURNING ID_DOCUMENTO INTO v_doc;
    INSERT INTO SMCFT.AUTORIZACION_MENOR (ID_AUTORIZACION, ID_MENOR, ID_REPRESENTANTE, ID_DOCUMENTO,
                                          TIPO_PERMISO, FECHA_EMISION, ESTADO)
    VALUES (SMCFT.SEQ_AUTORIZACION.NEXTVAL, :id_menor, :id_representante, v_doc, 'NOTARIAL',
            TRUNC(SYSDATE), 'VIGENTE')
    RETURNING ID_AUTORIZACION INTO v_id;
    :id_autorizacion := v_id;]',
        'id_autorizacion');

    COMMIT;
END;
/
