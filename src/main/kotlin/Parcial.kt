package ar.edu.algo2

import java.time.LocalDate



//Punto 1
abstract class Lugar(val nombre: String){

    // Template method
    open fun esDivertido(): Boolean = tieneCantidadParDeLetras() && criterioEspecifico()

    // Template method
    open fun tieneCantidadParDeLetras(): Boolean = nombre.length % 2 == 0

    // Primitiva
    abstract fun criterioEspecifico(): Boolean

    // Primitiva
    abstract fun esTranquilo(): Boolean
}

class Ciudad(nombre: String, var cantidadDeHabitantes : Int, var atraccionesTuristicas: List<String>, var decibelesPromedio: Double): Lugar(nombre){
    override fun criterioEspecifico() = atraccionesTuristicas.size > 3 && cantidadDeHabitantes > 100000

    override fun esTranquilo() = decibelesPromedio < 20

}


class Pueblo(nombre: String, var extesionKm : Double, var fundacion: LocalDate, var provincia: String): Lugar(nombre){
    val litorales = listOf<String>("Entre Ríos","Corrientes","Misiones")
    override fun criterioEspecifico() = fundacion.year < 1800 || litorales.contains(provincia)

    override fun esTranquilo() = provincia == "La Pampa"

}

class Balnearios(nombre: String, var promedioDeMetrosPlaya : Double, var marEsPeligroso: Boolean, var tienePeatonal: Boolean) : Lugar(nombre){
    override fun criterioEspecifico(): Boolean = promedioDeMetrosPlaya > 300 &&  marEsPeligroso

    override fun esTranquilo() = !tienePeatonal
}


// Punto 2
class Persona{
    val preferenciasVacaciones = mutableListOf<Preferencia>()

    fun agregarPreferencia(preferencia: Preferencia) {
        preferenciasVacaciones.add(preferencia)
    }

    fun eliminarPreferencias(preferencia: Preferencia) {
        preferenciasVacaciones.remove(preferencia)
    }


    fun lugarEsAdecuado(lugar: Lugar): Boolean {
        return preferenciasVacaciones.any { it.esAdecuado(lugar) }
    }
}

// Strategy
interface Preferencia{
    fun esAdecuado(lugar: Lugar): Boolean
}

class Tranquilidad : Preferencia{
    override fun esAdecuado(lugar: Lugar) = lugar.esTranquilo()
}

class Divertido : Preferencia{
    override fun esAdecuado(lugar: Lugar) = lugar.esDivertido()
}


class Alternador(var proxima: Preferencia, var otra: Preferencia): Preferencia {
    override fun esAdecuado(lugar: Lugar): Boolean {
        val resultado = proxima.esAdecuado(lugar)
        intercambiar()
        return resultado
    }

    private fun intercambiar() {
        val temp = proxima
        proxima = otra
        otra = temp
    }
}

class Combinado : Preferencia{
    val preferencias = mutableListOf<Preferencia>()
    override fun esAdecuado(lugar: Lugar) = preferencias.any { it.esAdecuado(lugar) }
}
