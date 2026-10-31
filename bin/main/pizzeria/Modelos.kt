package pizzeria

// Estados posibles de un pedido
enum class EstadoPedido(val etiqueta: String) {
    EN_PREPARACION("En preparación"),
    LISTO("Listo"),
    EN_CAMINO("En camino"),
    CANCELADO("Cancelado por falta de stock")
}

// Producto del menú: nombre, precio en pesos chilenos e ingredientes (cada elemento = 1 unidad por pizza)
data class Producto(
    val nombre: String,
    val precio: Int,
    val ingredientes: List<String>
) {
    fun detallarProducto(): String =
        "$nombre - ${formatearPesos(precio)} | Ingredientes: ${ingredientes.joinToString(", ")}"
}

// Clase base con atributos comunes. Los datos de contacto se validan al construir el objeto.
open class Usuario(
    val nombre: String,
    telefonoIngresado: String?,
    correoIngresado: String?
) {
    val telefono: String = validarTelefono(telefonoIngresado)
    val correo: String = validarCorreo(correoIngresado)

    open fun rol(): String = "Usuario"

    fun contacto(): String = "$telefono / $correo"
}

class Cliente(
    nombre: String,
    telefono: String?,
    correo: String?,
    val direccion: String = "Dirección no registrada"
) : Usuario(nombre, telefono, correo) {
    override fun rol(): String = "Cliente"
}

class Repartidor(
    nombre: String,
    telefono: String?,
    correo: String?,
    val distanciaKm: Double,
    var disponible: Boolean = true
) : Usuario(nombre, telefono, correo) {

    override fun rol(): String = "Repartidor"

    // Asigna el pedido al repartidor: cambia el estado a "En camino" y lo marca como ocupado
    fun entregarPedido(pedido: Pedido) {
        require(pedido.estado == EstadoPedido.LISTO && pedido.inventarioDescontado) {
            "Solo se pueden despachar pedidos aceptados y listos."
        }
        require(disponible) { "El repartidor no está disponible." }
        pedido.repartidor = this
        pedido.cambiarEstado(EstadoPedido.EN_CAMINO)
        disponible = false
        println("$nombre entrega el pedido de ${pedido.nombreCliente()}.")
    }
}

// Pedido: cliente opcional (puede ser venta sin registro), lista de productos y notas opcionales
class Pedido(
    val cliente: Cliente?,
    val productos: List<Producto>,
    var estado: EstadoPedido = EstadoPedido.EN_PREPARACION,
    val notas: String? = null
) {
    companion object {
        private var siguienteId = 1
    }

    val id: Int = siguienteId++
    var inventarioDescontado: Boolean = false
        internal set
    var repartidor: Repartidor? = null
        internal set

    init {
        require(productos.isNotEmpty()) { "El pedido debe contener al menos una pizza." }
        require(productos.all { it.precio >= 0 }) { "Los precios no pueden ser negativos." }
    }

    fun nombreCliente(): String = cliente?.nombre ?: "Cliente sin registrar"

    fun cambiarEstado(nuevoEstado: EstadoPedido) {
        estado = nuevoEstado
    }

    fun calcularTotal(): Int = calcularTotales(productos).total

    fun montoAPagar(): Int = if (estado == EstadoPedido.CANCELADO) 0 else calcularTotal()
}
