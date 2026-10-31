package pizzeria

fun ejecutarDemostracion() {
    println("=== PASO 4: Manejo de errores de ingreso ===")
    for (texto in listOf("8990", "abc", "-500")) {
        try {
            println("Costo registrado: ${formatearPesos(ingresarPrecio(texto))}")
        } catch (e: IllegalArgumentException) {
            println("Error de ingreso: ${e.message}")
        }
    }

    val datos = DatosPizzeria()
    val menu = datos.menu
    val inventario = datos.inventario
    val usuarios: List<Usuario> = datos.clientes
    val repartidores = datos.repartidores

    println("\n=== PASO 1 y 3: Validación de datos y POO ===")
    usuarios.forEach { println("${it.rol()}: ${it.nombre} | ${it.contacto()}") }
    println(menu.first().detallarProducto())

    println("\n=== PASO 4: Error de inventario controlado ===")
    try {
        ajustarStock(inventario, "aceitunas", -5)   // stock 0 => resultado negativo, se rechaza
    } catch (e: IllegalArgumentException) {
        println("Error de inventario: ${e.message}")
    }

    println("\n=== PASO 1 y 2: Disponibilidad de ingredientes y pedidos del día ===")
    val pedidosAceptados = mutableListOf<Pedido>()
    for (pedido in datos.pedidos) {
        if (aceptarPedido(pedido, inventario)) {
            pedidosAceptados.add(pedido)
        } else {
            // Simula la llegada de aceitunas al proveedor y reintenta el pedido
            println("Registrando reposición de aceitunas (+10)...")
            ajustarStock(inventario, "aceitunas", 10)
            if (aceptarPedido(pedido, inventario)) pedidosAceptados.add(pedido)
        }
    }
    val pedidosDelDia: Array<Pedido> = pedidosAceptados.toTypedArray()

    println("\n=== PASO 2: Asignación de repartidor y notificaciones (let) ===")
    for (pedido in pedidosDelDia) {
        // Solo se despachan los pedidos listos; los demás quedan pendientes
        if (pedido.estado == EstadoPedido.LISTO) {
            val repartidor = asignarRepartidor(pedido, repartidores)
            if (repartidor == null) println("Sin repartidores disponibles para ${pedido.nombreCliente()}.")
        }
        notificarEstado(pedido)
    }

    println("\n=== PASO 5: Reporte del día ===")
    imprimirReporte(pedidosDelDia)

    println("\n=== PASO 5 y 6: Resúmenes de pedido ===")
    pedidosDelDia.forEach { println(generarResumen(it)); println() }

    println("=== Inventario final ===")
    inventario.toSortedMap().forEach { (ingrediente, cantidad) -> println("$ingrediente: $cantidad") }
}
