-- ords/03_seguridad_oauth.sql: protege el modulo smcft.api con OAuth 2.0 (client credentials)
-- y registra el cliente "smcft_app" que usa la app Android.
-- Ejecutar conectado como SMCFT despues de 02_api_rest.sql.
--
-- IMPORTANTE: el script muestra el client_id y el client_secret UNA sola vez. Copielos en la
-- pantalla "Conexion" de la app. Si lo vuelve a ejecutar, el cliente se crea de nuevo con
-- otro secreto.
SET SERVEROUTPUT ON SIZE UNLIMITED
SET DEFINE OFF

DECLARE
    l_roles    OWA.VC_ARR;
    l_patrones OWA.VC_ARR;
    l_modulos  OWA.VC_ARR;
    l_cred     ORDS_TYPES.t_client_credentials;
BEGIN
    BEGIN
        ORDS.CREATE_ROLE(p_role_name => 'smcft_app');
    EXCEPTION
        WHEN OTHERS THEN NULL; -- el rol ya existia
    END;

    l_roles(1)   := 'smcft_app';
    l_modulos(1) := 'smcft.api';
    ORDS.DEFINE_PRIVILEGE(
        p_privilege_name => 'smcft.api',
        p_roles          => l_roles,
        p_patterns       => l_patrones,
        p_modules        => l_modulos,
        p_label          => 'API SMCFT',
        p_description    => 'Acceso de la app Android al modulo smcft.api');

    BEGIN
        ORDS_SECURITY.DELETE_CLIENT(p_name => 'smcft_app');
    EXCEPTION
        WHEN OTHERS THEN NULL; -- todavia no existia
    END;

    l_cred := ORDS_SECURITY.REGISTER_CLIENT(
        p_name            => 'smcft_app',
        p_grant_type      => 'client_credentials',
        p_support_email   => 'soporte@smcft.cl',
        p_description     => 'App Android SMCFT',
        p_client_secret   => ORDS_CONSTANTS.oauth_client_secret_default,
        p_privilege_names => 'smcft.api');

    ORDS_SECURITY.GRANT_CLIENT_ROLE(p_client_name => 'smcft_app', p_role_name => 'smcft_app');
    COMMIT;

    DBMS_OUTPUT.PUT_LINE('client_id     : ' || l_cred.client_key.client_id);
    DBMS_OUTPUT.PUT_LINE('client_secret : ' || l_cred.client_secret.secret);
END;
/
