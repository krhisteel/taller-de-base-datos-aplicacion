package cl.duoc.smcft.core

import cl.duoc.smcft.core.db.BaseDatos
import cl.duoc.smcft.core.db.Secuencias
import cl.duoc.smcft.core.model.*
import java.time.LocalDate

/**
 * Datos base de 03_datos_prueba.sql (Anexo 12): personas, usuario técnico 1, pasos, vehículos,
 * documentos, autorización del menor y categorías. Solo se cargan si la base está vacía.
 */
object DatosPrueba {

    fun cargar(db: BaseDatos) {
        if (db.tablas.personas.isNotEmpty()) return
        val hoy = db.hoy()
        val triggers = db.triggersHabilitados
        db.triggersHabilitados = false // la carga inicial no debe quedar en la auditoría
        try {
            db.atomico {
                db.insertarPersona(Persona(sig(db, Secuencias.PERSONA), "12345678-9", "Ana", "Perez Soto", "CHILENA", LocalDate.of(1985, 4, 12)))
                db.insertarPersona(Persona(sig(db, Secuencias.PERSONA), "9876543-2", "Carlos", "Diaz Munoz", "ARGENTINA", LocalDate.of(1990, 7, 1)))
                db.insertarPersona(Persona(sig(db, Secuencias.PERSONA), "11222333-4", "Sofia", "Perez Rivas", "CHILENA", LocalDate.of(2014, 2, 20)))
                db.insertarPersona(Persona(sig(db, Secuencias.PERSONA), "5555555-5", "Claudia", "Salazar Vera", "CHILENA", LocalDate.of(1980, 1, 1)))

                db.insertarUsuario(Usuario(sig(db, Secuencias.USUARIO), 4, "claudia.salazar@example.com"))

                db.insertarPaso(PasoFronterizo(sig(db, Secuencias.PASO), "Los Libertadores", "Valparaiso", "Chile"))
                db.insertarPaso(PasoFronterizo(sig(db, Secuencias.PASO), "Cardenal Samore", "Los Lagos", "Chile"))

                db.insertarVehiculo(Vehiculo(sig(db, Secuencias.VEHICULO), "ABCD12", "Toyota", "Yaris", 2020, "PARTICULAR", 1))
                db.insertarVehiculo(Vehiculo(sig(db, Secuencias.VEHICULO), "XX999Z", "Ford", "Ranger", 2022, "DIPLOMATICO", 2))

                db.insertarDocumento(Documento(sig(db, Secuencias.DOCUMENTO), "CEDULA", "C-12345678", LocalDate.of(2020, 1, 1), LocalDate.of(2030, 1, 1), 1, null, "VIGENTE"))
                db.insertarDocumento(Documento(sig(db, Secuencias.DOCUMENTO), "PASAPORTE", "P-9876543", LocalDate.of(2019, 5, 1), LocalDate.of(2029, 5, 1), 2, null, "VIGENTE"))
                db.insertarDocumento(Documento(sig(db, Secuencias.DOCUMENTO), "CEDULA", "C-11222333", LocalDate.of(2021, 1, 1), LocalDate.of(2031, 1, 1), 3, null, "VIGENTE"))
                db.insertarDocumento(Documento(sig(db, Secuencias.DOCUMENTO), "AUTORIZACION_NOTARIAL", "AUT-000123", hoy.minusDays(30), hoy.plusMonths(12), 3, null, "VIGENTE"))

                db.insertarAutorizacion(AutorizacionMenor(sig(db, Secuencias.AUTORIZACION), 3, 1, 4, "NOTARIAL", hoy.minusDays(30), "VIGENTE"))

                db.insertarCategoria(CategoriaItem(sig(db, Secuencias.CATEGORIA), "ELECTRONICA", "Equipos electronicos personales", true, 500.0))
                db.insertarCategoria(CategoriaItem(sig(db, Secuencias.CATEGORIA), "ALIMENTOS", "Alimentos de origen animal o vegetal", true, 0.0))
            }
        } finally {
            db.triggersHabilitados = triggers
        }
        db.commit()
    }

    private fun sig(db: BaseDatos, secuencia: String) = db.nextval(secuencia)
}
