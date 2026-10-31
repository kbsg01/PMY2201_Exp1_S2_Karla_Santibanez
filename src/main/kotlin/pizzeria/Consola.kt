package pizzeria

private class FinDeEntrada : RuntimeException()

private class EntradaConsola(val leer: () -> String?) {
    fun texto(mensaje: String): String {
        print("$mensaje: ")
        return leer()?.trim() ?: throw FinDeEntrada()
    }

    fun entero(mensaje: String, rango: IntRange): Int {
        while (true) {
            val valor = texto(mensaje).toIntOrNull()
            if (valor != null && valor in rango) return valor
            println("Entrada inválida. Ingresa un entero entre ${rango.first} y ${rango.last}.")
        }
    }

    fun <T> seleccionar(elementos: List<T>, etiqueta: (T) -> String): T {
        elementos.forEachIndexed { indice, elemento -> println("${indice + 1}) ${etiqueta(elemento)}") }
        return elementos[entero("Selecciona una opción", 1..elementos.size) - 1]
    }
}

fun ejecutarConsola(datos: DatosPizzeria = DatosPizzeria(), leer: () -> String? = { readlnOrNull() }) {
    val entrada = EntradaConsola(leer)
    println("=== PIZZERÍA ===")
    println("Se cargaron clientes, pizzas, repartidores y tres pedidos de ejemplo.")
    datos.pedidos.forEach { aceptarPedido(it, datos.inventario) }
    try {
        while (true) {
            println(
                """

                1) Ver pizzas y precios
                2) Ver clientes y repartidores
                3) Registrar cliente
                4) Crear pedido
                5) Ver resumen de pedido
                6) Ver inventario
                7) Ajustar stock
                8) Reintentar pedido cancelado
                9) Marcar pedido listo
                10) Asignar repartidor y notificar
                11) Ver reporte del día
                12) Actualizar precio de una pizza
                13) Ejecutar demostración original (datos independientes)
                0) Salir
                """.trimIndent()
            )
            when (entrada.entero("Opción", 0..13)) {
                0 -> { println("Hasta pronto."); return }
                1 -> datos.menu.forEach { println(it.detallarProducto()) }
                2 -> {
                    datos.clientes.forEach { println("${it.rol()}: ${it.nombre} | ${it.contacto()} | ${it.direccion}") }
                    datos.repartidores.forEach {
                        println("${it.rol()}: ${it.nombre} | ${it.contacto()} | ${it.distanciaKm} km | ${if (it.disponible) "Disponible" else "Ocupado"}")
                    }
                }
                3 -> {
                    val nombre = entrada.texto("Nombre (vacío: Cliente sin nombre)").ifBlank { "Cliente sin nombre" }
                    val direccion = entrada.texto("Dirección (opcional)").ifBlank { "Dirección no registrada" }
                    val telefono = entrada.texto("Teléfono")
                    val correo = entrada.texto("Correo")
                    val cliente = Cliente(nombre, telefono, correo, direccion)
                    datos.clientes.add(cliente)
                    println("Cliente registrado: ${cliente.nombre} | ${cliente.contacto()}")
                    if (cliente.telefono == TELEFONO_PREDETERMINADO) println("Teléfono vacío o inválido: se utilizó el predeterminado.")
                    if (cliente.correo == CORREO_PREDETERMINADO) println("Correo vacío o inválido: se utilizó el predeterminado.")
                }
                4 -> crearPedido(datos, entrada)
                5 -> println(generarResumen(seleccionarPedido(datos, entrada)))
                6 -> datos.inventario.toSortedMap().forEach { (ingrediente, stock) -> println("$ingrediente: $stock") }
                7 -> {
                    val ingrediente = entrada.seleccionar(datos.inventario.keys.sorted()) { it }
                    val cantidad = entrada.entero("Unidades a sumar o restar", Int.MIN_VALUE..Int.MAX_VALUE)
                    try {
                        ajustarStock(datos.inventario, ingrediente, cantidad)
                        println("Existencias de $ingrediente: ${datos.inventario[ingrediente]}")
                    } catch (e: IllegalArgumentException) {
                        println("Error de inventario: ${e.message}")
                    }
                }
                8 -> {
                    val pedido = seleccionarPedido(datos, entrada)
                    if (pedido.estado != EstadoPedido.CANCELADO) {
                        println("Solo se reintentan pedidos cancelados.")
                    } else {
                        aceptarPedido(pedido, datos.inventario)
                        notificarEstado(pedido)
                        println(generarResumen(pedido))
                    }
                }
                9 -> {
                    val pedido = seleccionarPedido(datos, entrada)
                    if (!pedido.inventarioDescontado || pedido.estado != EstadoPedido.EN_PREPARACION) {
                        println("Solo se pueden marcar listos los pedidos aceptados en preparación.")
                    } else {
                        pedido.cambiarEstado(EstadoPedido.LISTO)
                        notificarEstado(pedido)
                    }
                }
                10 -> {
                    val pedido = seleccionarPedido(datos, entrada)
                    val repartidor = asignarRepartidor(pedido, datos.repartidores)
                    if (repartidor != null) notificarEstado(pedido)
                    else if (pedido.estado == EstadoPedido.LISTO && pedido.inventarioDescontado) println("Sin repartidores disponibles.")
                }
                11 -> imprimirReporte(datos.pedidos.toTypedArray())
                12 -> {
                    val producto = entrada.seleccionar(datos.menu) { it.detallarProducto() }
                    while (true) {
                        val texto = entrada.texto("Nuevo precio en pesos enteros")
                        try {
                            val precio = ingresarPrecio(texto)
                            calcularTotales(listOf(producto.copy(precio = precio)))
                            datos.menu[datos.menu.indexOf(producto)] = producto.copy(precio = precio)
                            println("Precio actualizado; los pedidos existentes conservan su precio.")
                            break
                        } catch (e: IllegalArgumentException) {
                            println("Error de ingreso: ${e.message}")
                        }
                    }
                }
                13 -> ejecutarDemostracion()
            }
        }
    } catch (_: FinDeEntrada) {
        println("\nFin de entrada. Sesión terminada.")
    }
}

private fun seleccionarPedido(datos: DatosPizzeria, entrada: EntradaConsola): Pedido =
    entrada.seleccionar(datos.pedidos) { "#${it.id}: ${it.nombreCliente()} | ${it.estado.etiqueta}" }

private fun crearPedido(datos: DatosPizzeria, entrada: EntradaConsola) {
    println("0) Cliente sin registrar")
    datos.clientes.forEachIndexed { indice, cliente -> println("${indice + 1}) ${cliente.nombre}") }
    val indiceCliente = entrada.entero("Cliente", 0..datos.clientes.size)
    val cliente = if (indiceCliente == 0) null else datos.clientes[indiceCliente - 1]
    val productos = mutableListOf<Producto>()
    while (true) {
        val producto = entrada.seleccionar(datos.menu) { it.detallarProducto() }
        val cantidad = entrada.entero("Cantidad de pizzas (máximo 100 por pedido)", 1..(100 - productos.size))
        val candidato = productos + List(cantidad) { producto }
        try {
            calcularTotales(candidato)
            productos.addAll(List(cantidad) { producto })
        } catch (e: IllegalArgumentException) {
            println("Error de pedido: ${e.message}")
            continue
        }
        if (productos.size == 100 || entrada.entero("Agregar otra pizza: 1 sí, 0 no", 0..1) == 0) break
    }
    val notas = entrada.texto("Notas (opcional)").ifBlank { null }
    val pedido = Pedido(cliente, productos.toList(), notas = notas)
    datos.pedidos.add(pedido)
    aceptarPedido(pedido, datos.inventario)
    notificarEstado(pedido)
    println(generarResumen(pedido))
}
