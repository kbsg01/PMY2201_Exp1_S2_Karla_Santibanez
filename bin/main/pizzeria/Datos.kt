package pizzeria

class DatosPizzeria {
    val menu = mutableListOf(
        Producto("Margarita", 7990, listOf("masa", "salsa de tomate", "queso mozzarella", "albahaca")),
        Producto("Pepperoni", 9990, listOf("masa", "salsa de tomate", "queso mozzarella", "pepperoni")),
        Producto("Hawaiana", 9490, listOf("masa", "salsa de tomate", "queso mozzarella", "jamón", "piña")),
        Producto("Veggie", 10490, listOf("masa", "salsa de tomate", "queso mozzarella", "champiñones", "aceitunas"))
    )
    val inventario = mutableMapOf(
        "masa" to 40, "salsa de tomate" to 30, "queso mozzarella" to 50, "albahaca" to 10,
        "pepperoni" to 12, "jamón" to 15, "piña" to 4, "champiñones" to 3, "aceitunas" to 0
    )
    val clientes = mutableListOf(
        Cliente("Ana Morales", "+56 9 1234 5678", "ana.morales@gmail.com"),
        Cliente("Pedro Soto", "987654321", "pedro-soto@correo")
    )
    val repartidores = listOf(
        Repartidor("Luis Rojas", "9 8765 4321", "luis.r@gmail.com", distanciaKm = 3.5),
        Repartidor("Marta Vidal", "(000) 000-0000", "marta@pizzeria.cl", distanciaKm = 1.2),
        Repartidor("Jorge Peña", null, null, distanciaKm = 2.0)
    )
    val pedidos = mutableListOf(
        Pedido(clientes[0], listOf(menu[1], menu[1], menu[0]), EstadoPedido.LISTO),
        Pedido(clientes[1], List(4) { menu[2] }, notas = "Sin cebolla"),
        Pedido(null, listOf(menu[3]))
    )
}
