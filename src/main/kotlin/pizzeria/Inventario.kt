package pizzeria

// Ingrediente que falta para un pedido
data class Faltante(val ingrediente: String, val requerido: Int, val disponible: Int) {
    val faltan: Int get() = requerido - disponible
}

// Cantidad de cada ingrediente necesaria para preparar un lote de pizzas con la receta indicada
fun calcularNecesidad(ingredientesReceta: List<String>, pizzasEnLote: Int): Map<String, Int> =
    ingredientesReceta
        .groupingBy { it }
        .eachCount()
        .mapValues { (_, porPizza) -> porPizza * pizzasEnLote }

// Compara lo requerido con el inventario y devuelve solo los ingredientes insuficientes
fun verificarDisponibilidad(necesidad: Map<String, Int>, inventario: Map<String, Int>): List<Faltante> {
    val faltantes = mutableListOf<Faltante>()
    for ((ingrediente, requerido) in necesidad) {
        val disponible = inventario[ingrediente] ?: 0
        when {
            disponible >= requerido -> Unit // stock suficiente, no hay faltante
            else -> faltantes.add(Faltante(ingrediente, requerido, disponible))
        }
    }
    return faltantes
}

// Descuenta del inventario las cantidades necesarias (se asume que ya fue verificado)
fun descontarInventario(inventario: MutableMap<String, Int>, necesidad: Map<String, Int>) {
    require(necesidad.values.all { it >= 0 }) { "Las cantidades necesarias no pueden ser negativas." }
    require(verificarDisponibilidad(necesidad, inventario).isEmpty()) {
        "No hay ingredientes suficientes; el inventario no fue modificado."
    }
    necesidad.forEach { (ingrediente, cantidad) ->
        inventario[ingrediente] = (inventario[ingrediente] ?: 0) - cantidad
    }
}

// Suma o resta stock; impide dejar un ingrediente con cantidad negativa
fun ajustarStock(inventario: MutableMap<String, Int>, ingrediente: String, cantidad: Int) {
    val nuevoStock = (inventario[ingrediente] ?: 0).toLong() + cantidad
    require(nuevoStock in 0..Int.MAX_VALUE.toLong()) {
        "El stock de $ingrediente no puede quedar negativo (resultado: $nuevoStock)."
    }
    inventario[ingrediente] = nuevoStock.toInt()
}

// Acepta el pedido solo si hay ingredientes suficientes; si no, muestra qué reabastecer
fun aceptarPedido(pedido: Pedido, inventario: MutableMap<String, Int>): Boolean {
    if (pedido.inventarioDescontado) {
        println("El pedido #${pedido.id} ya fue aceptado; no se descontará stock nuevamente.")
        return true
    }
    val necesidad = calcularNecesidad(pedido.productos.flatMap { it.ingredientes }, 1)
    val faltantes = verificarDisponibilidad(necesidad, inventario)

    if (faltantes.isNotEmpty()) {
        pedido.cambiarEstado(EstadoPedido.CANCELADO)
        mostrarPasosReabastecimiento(faltantes)
        return false
    }
    descontarInventario(inventario, necesidad)
    pedido.inventarioDescontado = true
    if (pedido.estado == EstadoPedido.CANCELADO) pedido.cambiarEstado(EstadoPedido.EN_PREPARACION)
    println("Pedido #${pedido.id} confirmado. Existencias restantes:")
    necesidad.keys.sorted().forEach { println("    - $it: ${inventario[it]}") }
    return true
}

fun mostrarPasosReabastecimiento(faltantes: List<Faltante>) {
    println("[!] Pedido rechazado: ingredientes insuficientes:")
    faltantes.forEach { f ->
        println("    - ${f.ingrediente}: requeridos ${f.requerido}, disponibles ${f.disponible} (faltan ${f.faltan})")
    }
    println("    Pasos para reabastecer:")
    println("    1) Confirmar con el proveedor disponibilidad de: ${faltantes.joinToString { it.ingrediente }}")
    println("    2) Comprar al menos la cantidad faltante de cada ingrediente")
    println("    3) Registrar la recepción con ajustarStock(inventario, ingrediente, cantidad)")
    println("    4) Volver a validar el pedido")
}
