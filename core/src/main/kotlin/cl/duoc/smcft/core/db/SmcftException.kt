package cl.duoc.smcft.core.db

/**
 * Equivalente a RAISE_APPLICATION_ERROR: lleva el mismo código de error que el PL/SQL
 * (-20001, -20002, -20004, -20020, ...) para que las pruebas y la interfaz lo puedan distinguir.
 */
class SmcftException(val codigo: Int, val detalle: String) : RuntimeException("ORA$codigo: $detalle")

object Errores {
    const val PLAZO_VENCIDO = -20001
    const val MENOR_SIN_AUTORIZACION = -20002
    const val DOCUMENTO_VENCIDO = -20003
    const val VEHICULO_NO_ENCONTRADO = -20004
    const val SIN_PASOS_ACTIVOS = -20005
    const val PLAZO_NO_EXISTE = -20006
    const val PERSONA_NO_EXISTE = -20010
    const val RANGO_INVALIDO = -20020
    const val CREDENCIALES_INVALIDAS = -20031
    const val USUARIO_INACTIVO = -20032
    const val MOVIMIENTO_INVALIDO = -20021
    const val SIN_VIAJEROS = -20022

    /** ORA-02291: clave padre no encontrada. */
    const val FK_VIOLADA = -2291

    /** ORA-00001: restricción única violada. */
    const val UNICO_VIOLADO = -1

    /** ORA-02290: restricción CHECK violada. */
    const val CHECK_VIOLADO = -2290
}
