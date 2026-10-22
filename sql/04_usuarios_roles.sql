-- 04_usuarios_roles.sql: tipos de usuario, cuentas de la app e inicio de sesion.
-- Ejecutar conectado como SMCFT despues de 03_datos_prueba.sql. Se puede volver a ejecutar.
--
-- Cuentas de prueba (correo / contrasena):
--   admin@smcft.cl      / Admin2026   ADMINISTRADOR       (usuario tecnico 1, Claudia Salazar)
--   pdi@smcft.cl        / Pdi2026     OFICIAL_PDI
--   aduana@smcft.cl     / Aduana2026  FUNCIONARIO_ADUANA
--   sag@smcft.cl        / Sag2026     INSPECTOR_SAG
--   supervisor@smcft.cl / Super2026   SUPERVISOR
--
-- PASSWORD_HASH = RAWTOHEX(STANDARD_HASH('smcft:' || LOWER(email) || ':' || clave, 'SHA256'))
SET SERVEROUTPUT ON SIZE UNLIMITED
SET DEFINE OFF

-- Secuencias que faltaban en 02_secuencias.sql (se ignoran si ya existen)
DECLARE
    PROCEDURE crear(p_nombre VARCHAR2) IS
    BEGIN
        EXECUTE IMMEDIATE 'CREATE SEQUENCE SMCFT.' || p_nombre || ' START WITH 1 INCREMENT BY 1';
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE <> -955 THEN RAISE; END IF; -- ORA-00955: el nombre ya existe
    END;
BEGIN
    crear('SEQ_ROL');
    crear('SEQ_USUARIO_ROL');
    crear('SEQ_LOGIN_ATTEMPT');
END;
/

-- Valida las credenciales y registra el intento en LOGIN_ATTEMPT.
-- No lanza errores de credenciales: devuelve p_id_usuario NULL y p_resultado
-- 'CREDENCIALES' o 'INACTIVO', para que quien llama pueda confirmar (COMMIT) el intento fallido.
CREATE OR REPLACE PROCEDURE SMCFT.SP_INICIAR_SESION(
    p_email      IN  VARCHAR2,
    p_clave      IN  VARCHAR2,
    p_id_usuario OUT NUMBER,
    p_nombre     OUT VARCHAR2,
    p_rol        OUT VARCHAR2,
    p_resultado  OUT VARCHAR2
) AS
    v_id     SMCFT.USUARIO.ID_USUARIO%TYPE;
    v_hash   SMCFT.USUARIO.PASSWORD_HASH%TYPE;
    v_activo SMCFT.USUARIO.ACTIVO%TYPE;
    v_nombre VARCHAR2(201);
    v_calc   VARCHAR2(64);
    v_ok     CHAR(1) := 'N';
BEGIN
    BEGIN
        SELECT u.ID_USUARIO, u.PASSWORD_HASH, u.ACTIVO, p.NOMBRES || ' ' || p.APELLIDOS
          INTO v_id, v_hash, v_activo, v_nombre
          FROM SMCFT.USUARIO u
          JOIN SMCFT.PERSONA p ON p.ID_PERSONA = u.ID_PERSONA
         WHERE LOWER(u.EMAIL) = LOWER(TRIM(p_email));
    EXCEPTION
        WHEN NO_DATA_FOUND THEN v_id := NULL;
    END;

    SELECT RAWTOHEX(STANDARD_HASH('smcft:' || LOWER(TRIM(p_email)) || ':' || p_clave, 'SHA256'))
      INTO v_calc FROM DUAL;

    IF v_id IS NULL OR v_hash <> v_calc THEN
        p_resultado := 'CREDENCIALES';
    ELSIF v_activo <> 'S' THEN
        p_resultado := 'INACTIVO';
    ELSE
        p_resultado  := 'OK';
        v_ok         := 'S';
        p_id_usuario := v_id;
        p_nombre     := v_nombre;
        SELECT MIN(r.NOMBRE_ROL) INTO p_rol
          FROM SMCFT.USUARIO_ROL ur
          JOIN SMCFT.ROL r ON r.ID_ROL = ur.ID_ROL
         WHERE ur.ID_USUARIO = v_id AND ur.ACTIVO = 'S';
    END IF;

    INSERT INTO SMCFT.LOGIN_ATTEMPT (ID_INTENTO, ID_USUARIO, EXITOSO, IP_ADDRESS, FECHA_INTENTO)
    VALUES (SMCFT.SEQ_LOGIN_ATTEMPT.NEXTVAL, v_id, v_ok, OWA_UTIL.GET_CGI_ENV('REMOTE_ADDR'), SYSTIMESTAMP);
EXCEPTION
    WHEN VALUE_ERROR THEN
        -- OWA_UTIL sin contexto web (por ejemplo, desde SQL Developer): se registra sin IP.
        INSERT INTO SMCFT.LOGIN_ATTEMPT (ID_INTENTO, ID_USUARIO, EXITOSO, IP_ADDRESS, FECHA_INTENTO)
        VALUES (SMCFT.SEQ_LOGIN_ATTEMPT.NEXTVAL, v_id, v_ok, NULL, SYSTIMESTAMP);
END;
/

DECLARE
    TYPE t_cuenta IS RECORD (rut VARCHAR2(12), nombres VARCHAR2(100), apellidos VARCHAR2(100),
                             email VARCHAR2(150), clave VARCHAR2(50), rol VARCHAR2(50));
    TYPE t_cuentas IS TABLE OF t_cuenta;
    v_cuentas t_cuentas := t_cuentas();
    v_persona NUMBER;
    v_usuario NUMBER;
    v_rol     NUMBER;
    v_n       NUMBER;
    v_hash    VARCHAR2(64);

    FUNCTION hash(p_email VARCHAR2, p_clave VARCHAR2) RETURN VARCHAR2 IS
        v VARCHAR2(64);
    BEGIN
        SELECT RAWTOHEX(STANDARD_HASH('smcft:' || LOWER(p_email) || ':' || p_clave, 'SHA256')) INTO v FROM DUAL;
        RETURN v;
    END;

    PROCEDURE agregar(p_rut VARCHAR2, p_nombres VARCHAR2, p_apellidos VARCHAR2,
                      p_email VARCHAR2, p_clave VARCHAR2, p_rol VARCHAR2) IS
    BEGIN
        v_cuentas.EXTEND;
        v_cuentas(v_cuentas.COUNT).rut := p_rut;
        v_cuentas(v_cuentas.COUNT).nombres := p_nombres;
        v_cuentas(v_cuentas.COUNT).apellidos := p_apellidos;
        v_cuentas(v_cuentas.COUNT).email := p_email;
        v_cuentas(v_cuentas.COUNT).clave := p_clave;
        v_cuentas(v_cuentas.COUNT).rol := p_rol;
    END;
BEGIN
    -- Roles
    FOR r IN (SELECT 'ADMINISTRADOR' AS nombre FROM DUAL UNION ALL SELECT 'OFICIAL_PDI' FROM DUAL
              UNION ALL SELECT 'FUNCIONARIO_ADUANA' FROM DUAL UNION ALL SELECT 'INSPECTOR_SAG' FROM DUAL
              UNION ALL SELECT 'SUPERVISOR' FROM DUAL) LOOP
        SELECT COUNT(*) INTO v_n FROM SMCFT.ROL WHERE NOMBRE_ROL = r.nombre;
        IF v_n = 0 THEN
            INSERT INTO SMCFT.ROL (ID_ROL, NOMBRE_ROL)
            VALUES (SMCFT.SEQ_ROL.NEXTVAL, r.nombre);
        END IF;
    END LOOP;

    -- Administrador: usuario tecnico 1
    v_hash := hash('admin@smcft.cl', 'Admin2026');
    UPDATE SMCFT.USUARIO
       SET EMAIL = 'admin@smcft.cl', PASSWORD_HASH = v_hash, ACTIVO = 'S'
     WHERE ID_USUARIO = 1;

    agregar('15678234-3', 'Marcela', 'Rojas Diaz', 'pdi@smcft.cl', 'Pdi2026', 'OFICIAL_PDI');
    agregar('16789345-7', 'Jorge', 'Fuentes Vera', 'aduana@smcft.cl', 'Aduana2026', 'FUNCIONARIO_ADUANA');
    agregar('17890456-6', 'Paula', 'Contreras Mella', 'sag@smcft.cl', 'Sag2026', 'INSPECTOR_SAG');
    agregar('18901567-4', 'Rodrigo', 'Silva Araya', 'supervisor@smcft.cl', 'Super2026', 'SUPERVISOR');

    FOR i IN 1 .. v_cuentas.COUNT LOOP
        BEGIN
            SELECT ID_PERSONA INTO v_persona FROM SMCFT.PERSONA WHERE RUT = v_cuentas(i).rut;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                INSERT INTO SMCFT.PERSONA (ID_PERSONA, RUT, NOMBRES, APELLIDOS, NACIONALIDAD, FECHA_NACIMIENTO)
                VALUES (SMCFT.SEQ_PERSONA.NEXTVAL, v_cuentas(i).rut, v_cuentas(i).nombres, v_cuentas(i).apellidos,
                        'CHILENA', DATE '1988-06-15')
                RETURNING ID_PERSONA INTO v_persona;
        END;

        v_hash := hash(v_cuentas(i).email, v_cuentas(i).clave);
        BEGIN
            SELECT ID_USUARIO INTO v_usuario FROM SMCFT.USUARIO WHERE EMAIL = v_cuentas(i).email;
            UPDATE SMCFT.USUARIO SET PASSWORD_HASH = v_hash, ACTIVO = 'S'
             WHERE ID_USUARIO = v_usuario;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                INSERT INTO SMCFT.USUARIO (ID_USUARIO, ID_PERSONA, EMAIL, PASSWORD_HASH, ACTIVO, FECHA_CREACION)
                VALUES (SMCFT.SEQ_USUARIO.NEXTVAL, v_persona, v_cuentas(i).email, v_hash, 'S', SYSTIMESTAMP)
                RETURNING ID_USUARIO INTO v_usuario;
        END;
    END LOOP;

    -- Asignacion de roles (una por usuario)
    FOR a IN (SELECT u.ID_USUARIO, CASE u.EMAIL
                         WHEN 'admin@smcft.cl' THEN 'ADMINISTRADOR'
                         WHEN 'pdi@smcft.cl' THEN 'OFICIAL_PDI'
                         WHEN 'aduana@smcft.cl' THEN 'FUNCIONARIO_ADUANA'
                         WHEN 'sag@smcft.cl' THEN 'INSPECTOR_SAG'
                         WHEN 'supervisor@smcft.cl' THEN 'SUPERVISOR' END AS rol
                FROM SMCFT.USUARIO u
               WHERE u.EMAIL IN ('admin@smcft.cl', 'pdi@smcft.cl', 'aduana@smcft.cl', 'sag@smcft.cl', 'supervisor@smcft.cl')) LOOP
        SELECT ID_ROL INTO v_rol FROM SMCFT.ROL WHERE NOMBRE_ROL = a.rol;
        SELECT COUNT(*) INTO v_n FROM SMCFT.USUARIO_ROL WHERE ID_USUARIO = a.ID_USUARIO AND ID_ROL = v_rol;
        IF v_n = 0 THEN
            INSERT INTO SMCFT.USUARIO_ROL (ID_USUARIO_ROL, ID_USUARIO, ID_ROL, FECHA_ASIGNACION, ACTIVO)
            VALUES (SMCFT.SEQ_USUARIO_ROL.NEXTVAL, a.ID_USUARIO, v_rol, SYSTIMESTAMP, 'S');
        END IF;
    END LOOP;

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Usuarios y roles listos.');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END;
/
