package pizzeria

import kotlin.math.roundToInt

const val TASA_IVA = 0.19
const val UMBRAL_DESCUENTO_PIZZAS = 3   // más de 3 pizzas => descuento
const val PORCENTAJE_DESCUENTO = 0.10

data class Totales(val subtotal: Int, val impuestos: Int, val descuento: Int) {
    val total: Int get() = subtotal - descuento + impuestos
}

// Subtotal, descuento (10% si hay más de 3 pizzas) e IVA calculado sobre el valor con descuento
fun calcularTotales(productos: List<Producto>): Totales {
    require(productos.all { it.precio >= 0 }) { "Los precios no pueden ser negativos." }
    val subtotalLargo = productos.sumOf { it.precio.toLong() }
    require(subtotalLargo <= Int.MAX_VALUE / (1 + TASA_IVA)) { "El total del pedido excede el rango permitido." }
    val subtotal = subtotalLargo.toInt()
    val descuento = if (productos.size > UMBRAL_DESCUENTO_PIZZAS) {
        (subtotal * PORCENTAJE_DESCUENTO).roundToInt()
    } else {
        0
    }
    val impuestos = ((subtotal - descuento) * TASA_IVA).roundToInt()
    return Totales(subtotal, impuestos, descuento)
}

// Formato chileno: $12.990
fun formatearPesos(monto: Int): String =
    "\$" + monto.toString().reversed().chunked(3).joinToString(".").reversed()

// Reporte del día con ciclo for; "completados" = En camino, "pendientes" = En preparación o Listo
fun imprimirReporte(pedidos: Array<Pedido>) {
    println("REPORTE DE PEDIDOS DEL DÍA")
    println("-".repeat(70))
    for (pedido in pedidos) {
        val pizzas = pedido.productos
            .groupingBy { it.nombre }
            .eachCount()
            .entries
            .joinToString(", ") { (nombre, cantidad) -> "${cantidad}x $nombre" }
        println("Pedido #${pedido.id} | Cliente: ${pedido.nombreCliente()} | Pizzas: $pizzas | Total calculado: ${formatearPesos(pedido.calcularTotal())} | A pagar: ${formatearPesos(pedido.montoAPagar())} | Estado: ${pedido.estado.etiqueta}")
    }

    val completados = pedidos.filter { it.estado == EstadoPedido.EN_CAMINO }
    val pendientes = pedidos.filter { it.estado == EstadoPedido.EN_PREPARACION || it.estado == EstadoPedido.LISTO }
    val cancelados = pedidos.count { it.estado == EstadoPedido.CANCELADO }
    println("-".repeat(70))
    println("Completados: ${completados.size} | Pendientes: ${pendientes.size} | Cancelados: $cancelados")
}

// Resumen para confirmación al cliente y a la pizzería, con plantillas de strings
fun generarResumen(pedido: Pedido): String {
    val cliente = pedido.cliente
    val nombre = cliente?.nombre ?: "Cliente sin registrar"
    val telefono = cliente?.telefono ?: "No registrado"
    val correo = cliente?.correo ?: "No registrado"
    val notas = pedido.notas ?: "Sin notas"
    val totales = calcularTotales(pedido.productos)

    val lineasProductos = pedido.productos
        .groupBy { it.nombre }
        .map { (nombreProducto, lista) ->
            val precio = lista.first().precio
            "  - $nombreProducto | ${formatearPesos(precio)} x ${lista.size} = ${formatearPesos(precio * lista.size)}"
        }
        .joinToString("\n")

    val lineaDescuento = if (totales.descuento > 0) {
        "Descuento (${(PORCENTAJE_DESCUENTO * 100).roundToInt()}%): -${formatearPesos(totales.descuento)}"
    } else {
        "Descuento: no aplica"
    }

    // Se arma línea a línea (no con raw string + trimIndent) para no alterar la sangría de los productos
    return listOf(
        "==================== RESUMEN DEL PEDIDO ====================",
        "Pedido   : #${pedido.id}",
        "Cliente : $nombre",
        "Dirección: ${cliente?.direccion ?: "No registrada"}",
        "Teléfono: $telefono",
        "Correo  : $correo",
        "Notas   : $notas",
        "",
        "Productos:",
        lineasProductos,
        "Cantidad : ${pedido.productos.size} pizzas",
        "",
        "Subtotal : ${formatearPesos(totales.subtotal)}",
        "Impuestos (IVA ${(TASA_IVA * 100).roundToInt()}%): ${formatearPesos(totales.impuestos)}",
        lineaDescuento,
        "Total calculado: ${formatearPesos(totales.total)}",
        "Monto a pagar : ${formatearPesos(pedido.montoAPagar())}",
        "",
        "Estado   : ${pedido.estado.etiqueta}",
        "Repartidor: ${pedido.repartidor?.nombre ?: "Sin asignar"}",
        "============================================================"
    ).joinToString("\n")
}

// Asigna el repartidor disponible más cercano (ordena por distancia y usa find)
fun asignarRepartidor(pedido: Pedido, repartidores: List<Repartidor>): Repartidor? {
    if (pedido.estado != EstadoPedido.LISTO || !pedido.inventarioDescontado) {
        println("El pedido #${pedido.id} debe estar aceptado y listo para asignar repartidor.")
        return null
    }
    val elegido = repartidores.sortedBy { it.distanciaKm }.find { it.disponible }
    elegido?.entregarPedido(pedido)
    return elegido
}

// Notifica solo si el pedido tiene cliente registrado
fun notificarEstado(pedido: Pedido) {
    pedido.cliente?.let { cliente ->
        println("[Notificación para ${cliente.nombre}] Su pedido está: ${pedido.estado.etiqueta}")
    }
}
