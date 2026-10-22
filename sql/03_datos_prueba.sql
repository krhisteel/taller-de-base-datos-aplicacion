-- 03_datos_prueba.sql: datos base del sistema (version resumida del Anexo de codigo 12).
-- Carga las personas, el usuario tecnico 1 (requerido por los triggers), los pasos, los vehiculos,
-- los documentos, la autorizacion del menor y las categorias. Solo inserta si PERSONA esta vacia.
-- Despues adelanta cada secuencia por sobre el mayor ID existente.
-- Ejecutar conectado como SMCFT, con los triggers ya creados.
SET SERVEROUTPUT ON SIZE UNLIMITED
SET DEFINE OFF

DECLARE
    v_n NUMBER;

    PROCEDURE sincronizar(p_secuencia VARCHAR2, p_tabla VARCHAR2, p_columna VARCHAR2) IS
        v_max       NUMBER;
        v_siguiente NUMBER;
    BEGIN
        EXECUTE IMMEDIATE 'SELECT NVL(MAX(' || p_columna || '), 0) FROM SMCFT.' || p_tabla INTO v_max;
        LOOP
            EXECUTE IMMEDIATE 'SELECT SMCFT.' || p_secuencia || '.NEXTVAL FROM DUAL' INTO v_siguiente;
            EXIT WHEN v_siguiente > v_max;
        END LOOP;
    END sincronizar;
BEGIN
    SELECT COUNT(*) INTO v_n FROM SMCFT.PERSONA;

    IF v_n = 0 THEN
        INSERT INTO SMCFT.PERSONA VALUES (1, '12345678-9', 'Ana', 'Perez Soto', 'CHILENA', DATE '1985-04-12');
        INSERT INTO SMCFT.PERSONA VALUES (2, '9876543-2', 'Carlos', 'Diaz Munoz', 'ARGENTINA', DATE '1990-07-01');
        INSERT INTO SMCFT.PERSONA VALUES (3, '11222333-4', 'Sofia', 'Perez Rivas', 'CHILENA', DATE '2014-02-20');
        INSERT INTO SMCFT.PERSONA VALUES (4, '5555555-5', 'Claudia', 'Salazar Vera', 'CHILENA', DATE '1980-01-01');

        INSERT INTO SMCFT.USUARIO
        VALUES (1, 4, 'claudia.salazar@example.com', 'SIN_CREDENCIAL_DE_ACCESO', 'S', SYSTIMESTAMP);

        INSERT INTO SMCFT.PASO_FRONTERIZO VALUES (1, 'Los Libertadores', 'Valparaiso', 'Chile', 'ACTIVO');
        INSERT INTO SMCFT.PASO_FRONTERIZO VALUES (2, 'Cardenal Samore', 'Los Lagos', 'Chile', 'ACTIVO');

        INSERT INTO SMCFT.VEHICULO VALUES (1, 'ABCD12', 'Toyota', 'Yaris', 2020, 'PARTICULAR', 1, 'ACTIVO');
        INSERT INTO SMCFT.VEHICULO VALUES (2, 'XX999Z', 'Ford', 'Ranger', 2022, 'DIPLOMATICO', 2, 'ACTIVO');

        INSERT INTO SMCFT.DOCUMENTO
        VALUES (1, 'CEDULA', 'C-12345678', DATE '2020-01-01', DATE '2030-01-01', 1, NULL, 'VIGENTE');
        INSERT INTO SMCFT.DOCUMENTO
        VALUES (2, 'PASAPORTE', 'P-9876543', DATE '2019-05-01', DATE '2029-05-01', 2, NULL, 'VIGENTE');
        INSERT INTO SMCFT.DOCUMENTO
        VALUES (3, 'CEDULA', 'C-11222333', DATE '2021-01-01', DATE '2031-01-01', 3, NULL, 'VIGENTE');
        INSERT INTO SMCFT.DOCUMENTO
        VALUES (4, 'AUTORIZACION_NOTARIAL', 'AUT-000123', TRUNC(SYSDATE) - 30, ADD_MONTHS(TRUNC(SYSDATE), 12),
                3, NULL, 'VIGENTE');

        INSERT INTO SMCFT.AUTORIZACION_MENOR VALUES (1, 3, 1, 4, 'NOTARIAL', TRUNC(SYSDATE) - 30, 'VIGENTE');

        INSERT INTO SMCFT.CATEGORIA_ITEM
        VALUES (1, 'ELECTRONICA', 'Equipos electronicos personales', 'S', 500, 'S');
        INSERT INTO SMCFT.CATEGORIA_ITEM
        VALUES (2, 'ALIMENTOS', 'Alimentos de origen animal o vegetal', 'S', 0, 'S');

        DBMS_OUTPUT.PUT_LINE('Datos base insertados.');
    ELSE
        DBMS_OUTPUT.PUT_LINE('PERSONA ya tiene datos: no se insertan los datos base.');
    END IF;

    SELECT COUNT(*) INTO v_n FROM SMCFT.USUARIO WHERE ID_USUARIO = 1;
    IF v_n <> 1 THEN
        RAISE_APPLICATION_ERROR(-20044, 'Falta el usuario tecnico ID 1 requerido por los triggers.');
    END IF;

    sincronizar('SEQ_PERSONA', 'PERSONA', 'ID_PERSONA');
    sincronizar('SEQ_USUARIO', 'USUARIO', 'ID_USUARIO');
    sincronizar('SEQ_VEHICULO', 'VEHICULO', 'ID_VEHICULO');
    sincronizar('SEQ_DOCUMENTO', 'DOCUMENTO', 'ID_DOCUMENTO');
    sincronizar('SEQ_PASO', 'PASO_FRONTERIZO', 'ID_PASO');
    sincronizar('SEQ_CRUCE', 'CRUCE_FRONTERIZO', 'ID_CRUCE');
    sincronizar('SEQ_CATEGORIA', 'CATEGORIA_ITEM', 'ID_CATEGORIA');
    sincronizar('SEQ_DECLARACION', 'DECLARACION', 'ID_DECLARACION');
    sincronizar('SEQ_ITEM', 'ITEM_DECLARADO', 'ID_ITEM');
    sincronizar('SEQ_PLAZO', 'PLAZO_ADMISION', 'ID_PLAZO');
    sincronizar('SEQ_AUTORIZACION', 'AUTORIZACION_MENOR', 'ID_AUTORIZACION');
    sincronizar('SEQ_REPORTE', 'REPORTE', 'ID_REPORTE');
    sincronizar('SEQ_REPORTE_DET', 'REPORTE_DETALLE', 'ID_DETALLE');
    sincronizar('SEQ_AUDITORIA', 'AUDITORIA', 'ID_AUDITORIA');
    sincronizar('SEQ_AUDITORIA_DET', 'AUDITORIA_DETALLE', 'ID_DETALLE');

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Secuencias sincronizadas.');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END;
/
