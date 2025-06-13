package ar.edu.algo2

import java.time.LocalDate


//Punto 1

// Template Method
// Definimos el esqueleto de un algoritmo donde una parte se repite
// pero permitimos que algunas otras, específicas, sean personalizables en clases hijas.
//
// Template esDivertido()
// Primitiva criterioEspecifico()
// Primitiva esTranquilo
abstract class Lugar(val nombre: String, val codigoAfip: String){

    open fun esDivertido(): Boolean = nombre.length % 2 == 0 && criterioEspecifico()

    abstract fun criterioEspecifico(): Boolean

    abstract fun esTranquilo(): Boolean
}

class Ciudad(nombre: String,
             codigoAfip: String,
             var cantidadDeHabitantes : Int,
             var decibelesPromedio: Double): Lugar(nombre, codigoAfip)
{
    val atraccionesTuristicas= mutableListOf<String>()

    override fun criterioEspecifico() = atraccionesTuristicas.size > 3 && cantidadDeHabitantes > 100000

    override fun esTranquilo() = decibelesPromedio < 20

}


class Pueblo(nombre: String,
             codigoAfip: String,
             val extesionKm : Double,
             val fundacion: LocalDate,
             val provincia: String): Lugar(nombre, codigoAfip)
{
    val litorales = listOf<String>("Entre Ríos","Corrientes","Misiones")

    override fun criterioEspecifico() = fundacion.year < 1800 || litorales.contains(provincia)

    override fun esTranquilo() = provincia == "La Pampa"

}

class Balnearios(nombre: String,
                 codigoAfip: String,
                 val promedioDeMetrosPlaya : Double,
                 val marEsPeligroso: Boolean,
                 val tienePeatonal: Boolean) : Lugar(nombre,codigoAfip){

    override fun criterioEspecifico(): Boolean = promedioDeMetrosPlaya > 300 &&  marEsPeligroso

    override fun esTranquilo(): Boolean = !tienePeatonal
}


// Punto 2
class Persona{
    var presupuesto: Double = 0.0
    lateinit var mail: String
    lateinit var preferenciaVacaciones: Preferencia
    lateinit var dni: String


    fun lugarEsAdecuado(lugar: Lugar): Boolean =  preferenciaVacaciones.esAdecuado(lugar)

}

// Strategy ==> desacoplar el algoritmo de elección de una clase
// La lógica de cómo se hace algo se separa de la clase que usa esa lógica
//
// En este caso desacoplar el algoritmo de preferencia de un lugar
interface Preferencia{
    fun esAdecuado(lugar: Lugar): Boolean
}

object Tranquilo : Preferencia{
    override fun esAdecuado(lugar: Lugar) = lugar.esTranquilo()
}

object Divertido : Preferencia{
    override fun esAdecuado(lugar: Lugar) = lugar.esDivertido()
}


class Bipolar: Preferencia{
    val preferenciasARotar = mutableListOf<Preferencia>(Tranquilo,Divertido)

    override fun esAdecuado(lugar: Lugar): Boolean {
        val primero = preferenciasARotar.first()
        val resultado = primero.esAdecuado(lugar)

        if (resultado) {rotarPrefencias()}

        return resultado
    }

    fun rotarPrefencias(){
        val primero = preferenciasARotar.removeAt(0)
        preferenciasARotar.add(primero)
    }
}

// Composite ==> rama y hojas funcionan de manera polimórfica
// Implementan la misma interfaz o clase base
// Se pueden tratar de manera uniforme sin qué el código sepa si maneja una hoja o una rama
//
// En este caso Combinado es la rama y las anteriores las hojas
class Combinado : Preferencia{
    val preferencias = mutableListOf<Preferencia>()
    override fun esAdecuado(lugar: Lugar) = preferencias.any { it.esAdecuado(lugar) }
}

// Command ⇒ encapsula la lógica de asignar personas a tours según condiciones
// Ejecuta acciones diferentes (asignar o agregar a pendientes) como comandos implícitos
class Tour(val fechaDeSalida: LocalDate,
           val cantidadDePersonasRequeridas: Int,
           val montoApagarPorPersona: Double)
{
    val lugaresAvisitar: MutableList<Lugar> = mutableListOf()
    val personasAviajar = mutableListOf<Persona>()
    var confirmado: Boolean = false

    fun estaLleno(): Boolean = personasAviajar.size >= cantidadDePersonasRequeridas

    fun confirmarTour(){
        confirmado = true
    }

    fun agregarPersonaATour(persona:Persona){
        if (!confirmado){ personasAviajar.add(persona) }
        if (estaLleno()){ confirmarTour() }
    }

    fun quitarPersonaSinCondiciones(persona: Persona){
        personasAviajar.remove(persona)

    }

    fun mailsPersonasAnotadas(): List<String> {
        return personasAviajar.map { it.mail }
    }


}

class Administrador{
    val toursADesignar = mutableListOf<Tour>()
    val personasParaAsignarTour = mutableListOf<Persona>()
    val personasPendientesDeAsignarTour = mutableListOf<Persona>()
    val observers = mutableListOf<PostConfirmacionObservers>()

    fun otorgarTourAPersonas() {
        personasParaAsignarTour.forEach { persona ->
            toursADesignar
                .filter { !it.confirmado }
                .filter { it.montoApagarPorPersona <= persona.presupuesto }
                .find { it.lugaresAvisitar.all { lugar -> persona.lugarEsAdecuado(lugar) } }
                ?.also { agregarPersonaATour(it, persona) }
                ?: run { agregarPersonaAPendientesDeAsignar(persona) }
        }

    }

    fun agregarPersonaATour(tour:Tour, persona:Persona){
        tour.agregarPersonaATour(persona)
        personasParaAsignarTour.remove(persona)
    }

    fun agregarPersonaAPendientesDeAsignar(persona: Persona){
        personasPendientesDeAsignarTour.add(persona)
    }

    fun quitarPersonaDeTour(tour:Tour, persona:Persona){
        tour.quitarPersonaSinCondiciones(persona)
    }

    fun confirmarTour(tour: Tour){
        tour.confirmarTour()
        observers.forEach { it.ejecutar(tour)}
    }
}


interface ServicioMail {
    fun enviarMail(mail: Mail)
}

data class Mail(val from: String, val to: String, val subject: String, val content: String)


// Observer ⇒ ejecuta acciones cuando se produce un evento (al confirmar un tour)
interface PostConfirmacionObservers {
    fun ejecutar(tour: Tour){

    }
}

class EnviarMail: PostConfirmacionObservers {
    lateinit var servicioMail: ServicioMail

    override fun ejecutar(tour: Tour) {
        val fechaLimite = tour.fechaDeSalida.minusDays(30)
        tour.mailsPersonasAnotadas().forEach {
            servicioMail.enviarMail(
                Mail(
                    from = "admin@admin.com",
                    to = it,
                    subject = "El tour ha sido confirmado!",
                    content = "La fecha de salida será el día ${tour.fechaDeSalida}" +
                            "La fecha límite de pago será el día $fechaLimite" +
                            "Visitaremos los siguientes lugares:" +
                            lugaresAVisitarEnMail(tour)
                )
            )
        }
    }

    fun lugaresAVisitarEnMail(tour: Tour): String {
        return tour.lugaresAvisitar.joinToString(", ") { it.nombre }
    }
}

interface AfipEnviar {
    fun notificarAfip(data: InterfazAFIP)
}

data class InterfazAFIP(val from: String, val to: String, val dni: String)


class RotadorDePreferenciaBipolar : PostConfirmacionObservers {
    override fun ejecutar(tour: Tour) {
        val personas = tour.personasAviajar

        personas.forEach { persona ->
            val preferencia = persona.preferenciaVacaciones

            if (preferencia is Bipolar) {

                preferencia.rotarPrefencias()
            }
        }
    }
}