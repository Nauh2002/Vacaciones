package ar.edu.algo2

import java.time.LocalDate


//Punto 1

// Template Method
// Definimos el esqueleto de un algoritmo donde una parte se repite
// pero permitimos que algunas otras, específicas, sean personalizables en clases hijas.
//
// Template esDivertido()
// template tieneCantidadParDeLetras()
// Primitiva criterioEspecifico()
// Primitiva esTranquilo
abstract class Lugar(val nombre: String, val codigoAfip: String){

    open fun esDivertido(): Boolean = tieneCantidadParDeLetras() && criterioEspecifico()

    open fun tieneCantidadParDeLetras(): Boolean = nombre.length % 2 == 0

    abstract fun criterioEspecifico(): Boolean

    abstract fun esTranquilo(): Boolean
}

class Ciudad(nombre: String,
             codigoAfip: String,
             var cantidadDeHabitantes : Int,
             var atraccionesTuristicas: List<String>,
             var decibelesPromedio: Double): Lugar(nombre, codigoAfip){

                 override fun criterioEspecifico() = atraccionesTuristicas.size > 3 && cantidadDeHabitantes > 100000

    override fun esTranquilo() = decibelesPromedio < 20

}


class Pueblo(nombre: String,
             codigoAfip: String,
             var extesionKm : Double,
             var fundacion: LocalDate,
             var provincia: String): Lugar(nombre, codigoAfip){
    val litorales = listOf<String>("Entre Ríos","Corrientes","Misiones")

    override fun criterioEspecifico() = fundacion.year < 1800 || litorales.contains(provincia)

    override fun esTranquilo() = provincia == "La Pampa"

}

class Balnearios(nombre: String,
                 codigoAfip: String,
                 var promedioDeMetrosPlaya : Double,
                 var marEsPeligroso: Boolean,
                 var tienePeatonal: Boolean) : Lugar(nombre,codigoAfip){

    override fun criterioEspecifico(): Boolean = promedioDeMetrosPlaya > 300 &&  marEsPeligroso

    override fun esTranquilo() = !tienePeatonal
}


// Punto 2
class Persona{
    var presupuesto: Double = 0.0
    lateinit var email: String
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

object Tranquilidad : Preferencia{
    override fun esAdecuado(lugar: Lugar) = lugar.esTranquilo()
}

object Divertido : Preferencia{
    override fun esAdecuado(lugar: Lugar) = lugar.esDivertido()
}


class Alternador(var proxima: Preferencia, var otra: Preferencia): Preferencia {
    override fun esAdecuado(lugar: Lugar): Boolean {
        val resultado = proxima.esAdecuado(lugar)
        intercambiar()
        return resultado
    }

    fun intercambiar() {
        val temp = proxima
        proxima = otra
        otra = temp
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


class Tour(var fechaDeSalida: LocalDate,
           var cantidadDePersonasRequeridas: Int,
           val lugares: MutableList<Lugar> = mutableListOf(),
           val montoApagarPorPersona: Double)
{
    val personasAviajar = mutableListOf<Persona>()
    val pendientes = mutableListOf<Persona>()
    val tourConfirmadoObserver = mutableListOf<TourConfirmadoObserver>()
    val observerAfip = mutableListOf<ObserverAfip>()


    fun puedeIr(persona: Persona): Boolean = persona.presupuesto >= montoApagarPorPersona && lugares.all{ lugar -> persona.lugarEsAdecuado(lugar) }

    fun agregarPersonaAlTour(persona: Persona) {
        if (personasAviajar.size < cantidadDePersonasRequeridas && puedeIr(persona)) {
            personasAviajar.add(persona)
        } else {
            pendientes.add(persona)
        }
    }

    fun agregarObserver(observer: TourConfirmadoObserver) {
        tourConfirmadoObserver.add(observer)
    }

    fun confirmarTour(administrador: Administrador) {
        if (administrador.estaConfirmado(this)) {
            personasAviajar.forEach { persona ->
                tourConfirmadoObserver.forEach { it.notificarParticipante(persona) }
                observerAfip.forEach { it.informarAfip(persona, this) }
            }
        }
    }
}

class Administrador{

    fun adminAgregarPersona(persona: Persona, tour: Tour) {
        tour.personasAviajar.add(persona)
    }

    fun adminEliminarPersona(persona: Persona,  tour: Tour) {
        tour.personasAviajar.remove(persona)
    }

    fun estaConfirmado(tour: Tour): Boolean = tour.personasAviajar.size == tour.cantidadDePersonasRequeridas
}


interface ServicioMail {
    fun enviarMail(mail: Mail)
}

data class Mail(val from: String, val fechaDeSalida: LocalDate, var fechaLimite: LocalDate, val lugaresAvisitar: List<Lugar>)

interface TourConfirmadoObserver {
    fun notificarParticipante(persona: Persona){
    }
}

class NotificarPersonas : TourConfirmadoObserver {
    lateinit var servicioMail : ServicioMail
    lateinit var fechaDeSalida: LocalDate
    lateinit var lugaresAvisitar: List<Lugar>

    override fun notificarParticipante(persona: Persona) {
        val fechaLimite = minOf(fechaDeSalida.minusDays(30), LocalDate.now())
        servicioMail.enviarMail(Mail(
            from = persona.email,
            fechaDeSalida = fechaDeSalida,
            fechaLimite = fechaLimite,
            lugaresAvisitar = lugaresAvisitar
        ))
    }
}


interface AfipEnviar{
    fun notificarAfip(data: Data)
}

data class Data(val from: String, val to: String, val dni: String)


interface ObserverAfip{
    fun informarAfip(persona: Persona, tour: Tour)
}


class InformarAfip : ObserverAfip{
    lateinit var afipEnviar : AfipEnviar
    override fun informarAfip(persona: Persona, tour: Tour) {
        if(tour.montoApagarPorPersona > 10_000_000){
            afipEnviar.notificarAfip(Data(
                from = persona.email,
                to = "AFIP",
                dni = persona.dni
            ))
        }

    }
}

class AlternadorCambioPreferencia : TourConfirmadoObserver {
    override fun notificarParticipante(persona: Persona) {
        if (persona.preferenciaVacaciones is Alternador) {
            val alternador = persona.preferenciaVacaciones as Alternador
            alternador.intercambiar()
        }
    }
}


/*Observaciones
*
* ---------------------------------------------------
* Tipos de ideas posibles para resolver los preferencia para irse de vacaciones:
*
* 1-Strategy (Lo implementado)
* Buscamos dinamismo, generando cambios durante la ejecución
* Desacopla comportamientos a una clase (que es la beneficiada)
* Es más simple agregar otro elemento a la estructura
*
* 2-Crear subclases
* Separa comportamientos en clases hijas (Solo una herencia por clase)
* Útil cuando las variantes son pocas, estables y bien definidas (Regalos)
* Poca flexibilidad, implica nueva clase o subclases
*
* 3-Tener variables y condicionales
* También otorga dinamismo: tener una variable con un string y condicionales me da la misma posibilidad
* que el strategy
* El problema es que acopla la lógica a una calse concreta con if y when
* Útil cuando se necesitan soluciones rápidas y/o con pocos casos
*
* ---------------------------------------------------
* En cuanto al criterio de Strategy (dependiendo de si quiero asignar algún valor)
* -Atenti que usa object y clases dependiendo del tipo de State que aplica-
*
* 1- Stateless
* Reutilizable como object
*
* 2- State full
* Se utilizan clases ya que cada asignación necesitara de un object independiente
*
*-----------------------------------------------------
* Ventajas en cuanto al armar un Strategy con Interface sobre Abstract Class
* No pierdo la posibilidad de subclasificar la clase (no gasto la bala de la clase)
*
* -----------------------------------------------------
* Cuando va Strategy?
* Cuando necesito separar de mi objeto ese algoritmo que encapsulo en objetos polimórficos
*
* Cuando va Template Method?
* Necesito tener objetos polimórficos en una jerarquía que no va a cambiar
* por lo que no me interesa modificar la identidad del objeto a futuro
*
* -----------------------------------------------------
*
* Abstract Class
*
* 1-Uso de super
* Problema si al hacer una nueva clase y no se carga, puede generar problemas en la lógica
*
* 2-Metodo abstracto
* Me obliga a cargar el metodo necesario al crear nuevas clases
* Se usa porque así lo dice el Dodain, sino, pal lobby
*
* -----------------------------------------------------
* Inmutabilidad
*
* 1-val
* Al generar objeto con atributos con "val" se generan objetos del tipo inmutables
* Es una decisión de diseño que implica que no queremos que ese atributo sea modificado
* Se puede leer, pero no se puede volver a asignar
* En una colección, podra ser mutable su contenido, pero no la referencia (el tipo de objetos que guardo).
*
* 2-var
* El atributo puede ser reemplazado con otra instancia o valor en cualquier momento
* Se puede leer y escribir
* Menos seguro cuando se busca inmutabilidad
*
* -----------------------------------------------------
* Observer
*
* Uso de Long Parameter Method
* Al usar varios campos de otros objetos como la data class, se puede estructurar de una forma más ordenada el contenido que tendría un mail por ej
* Como no están agrupados los atributos que tienen la data class, ayuda a un mejor uso
*
* Data class
* Se tiene por default metodos como equals, copy... permiten testearlos más fácil
* No necesitan getters ni setters
* Permiten representar los objetos que vamos a vincular con las APIs
* Inmutables por defecto
*
* Value Object
* Como el data class de mail, que lo usamos para representar el concepto de un mail
*
*/



