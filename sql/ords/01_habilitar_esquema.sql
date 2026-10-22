-- ords/01_habilitar_esquema.sql: habilita el esquema SMCFT en ORDS (Oracle REST Data Services).
-- Ejecutar conectado como SMCFT. Las URLs quedan como https://<host>/ords/smcft/...
--
-- Si aparece un error de privilegios, ejecutar como ADMIN:
--   BEGIN
--       ORDS_ADMIN.ENABLE_SCHEMA(p_enabled => TRUE, p_schema => 'SMCFT',
--           p_url_mapping_type => 'BASE_PATH', p_url_mapping_pattern => 'smcft', p_auto_rest_auth => TRUE);
--       COMMIT;
--   END;
--   /
BEGIN
    ORDS.ENABLE_SCHEMA(
        p_enabled             => TRUE,
        p_url_mapping_type    => 'BASE_PATH',
        p_url_mapping_pattern => 'smcft',
        p_auto_rest_auth      => TRUE   -- las tablas NO quedan publicadas automaticamente
    );
    COMMIT;
END;
/
