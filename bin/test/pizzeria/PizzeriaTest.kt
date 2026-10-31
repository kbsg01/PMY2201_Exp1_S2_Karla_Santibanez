package pizzeria

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PizzeriaTest {
    private fun capturar(accion: () -> Unit): String {
        val anterior = System.out
        val bytes = ByteArrayOutputStream()
        val salida = PrintStream(bytes, true, Charsets.UTF_8)
        try {
            System.setOut(salida)
            accion()
        } finally {
            System.setOut(anterior)
            salida.close()
        }
        return bytes.toString(Charsets.UTF_8)
    }

    @Test
    fun descuentoSoloDesdeCuatroPizzas() {
        val pizza = Producto("Prueba", 1000, listOf("masa"))
        assertEquals(Totales(3000, 570, 0), calcularTotales(List(3) { pizza }))
        assertEquals(Totales(4000, 684, 400), calcularTotales(List(4) { pizza }))
        assertEquals(4284, calcularTotales(List(4) { pizza }).total)
        assertFailsWith<IllegalArgumentException> {
            calcularTotales(listOf(pizza.copy(precio = Int.MAX_VALUE)))
        }
    }

    @Test
    fun cancelarReponerYReintentarSinDobleDescuento() {
        val datos = DatosPizzeria()
        val pedido = datos.pedidos.last()
        val original = datos.inventario.toMap()
        val salida = capturar {
            assertFalse(aceptarPedido(pedido, datos.inventario))
            assertEquals(original, datos.inventario)
            assertEquals(EstadoPedido.CANCELADO, pedido.estado)
            assertEquals(0, pedido.montoAPagar())
            assertTrue(generarResumen(pedido).contains("Monto a pagar : \$0"))
            ajustarStock(datos.inventario, "aceitunas", 10)
            assertTrue(aceptarPedido(pedido, datos.inventario))
            assertEquals(EstadoPedido.EN_PREPARACION, pedido.estado)
            assertEquals(9, datos.inventario["aceitunas"])
            assertEquals(pedido.calcularTotal(), pedido.montoAPagar())
            val aceptado = datos.inventario.toMap()
            assertTrue(aceptarPedido(pedido, datos.inventario))
            assertEquals(aceptado, datos.inventario)
        }
        assertTrue(salida.contains("Existencias restantes"))
        assertTrue(salida.contains("aceitunas: 9"))
    }

    @Test
    fun stockInsuficienteYOverflowNoModificanInventario() {
        val inventario = mutableMapOf("masa" to 2, "queso" to 0)
        val original = inventario.toMap()
        assertFailsWith<IllegalArgumentException> {
            descontarInventario(inventario, mapOf("masa" to 1, "queso" to 1))
        }
        assertFailsWith<IllegalArgumentException> { ajustarStock(inventario, "masa", -3) }
        assertFailsWith<IllegalArgumentException> { ajustarStock(inventario, "masa", Int.MAX_VALUE) }
        assertEquals(original, inventario)
        assertEquals(mapOf("masa" to 4), calcularNecesidad(listOf("masa", "masa"), 2))
    }

    @Test
    fun asignacionRespetaEstadoDisponibilidadYResumen() {
        val datos = DatosPizzeria()
        val pedido = datos.pedidos[1]
        capturar {
            assertNull(asignarRepartidor(pedido, datos.repartidores))
            aceptarPedido(pedido, datos.inventario)
            assertNull(asignarRepartidor(pedido, datos.repartidores))
            pedido.cambiarEstado(EstadoPedido.LISTO)
            val repartidor = assertNotNull(asignarRepartidor(pedido, datos.repartidores))
            assertEquals("Marta Vidal", repartidor.nombre)
            assertFalse(repartidor.disponible)
            assertEquals(EstadoPedido.EN_CAMINO, pedido.estado)
            assertNull(asignarRepartidor(pedido, datos.repartidores))
            val resumen = generarResumen(pedido)
            listOf("Pedido   : #", "Dirección:", "Cantidad : 4", "Subtotal", "Descuento (10%)",
                "Total calculado:", "Monto a pagar", "Repartidor: Marta Vidal").forEach {
                assertTrue(resumen.contains(it), it)
            }
        }
    }

    @Test
    fun validacionDeContactosYPrecios() {
        assertEquals("+56 9 1234 5678", validarTelefono("+56 9 1234 5678"))
        assertEquals("+123456789012345", validarTelefono("+123456789012345"))
        assertEquals("123456789", validarTelefono("123456789"))
        listOf(null, "", "12345678", "+1234567890123456", "++123456789").forEach {
            assertEquals(TELEFONO_PREDETERMINADO, validarTelefono(it))
        }
        assertEquals(CORREO_PREDETERMINADO, validarCorreo(null))
        assertEquals("ana@correo.cl", validarCorreo(" ANA@CORREO.CL "))
        assertEquals(8990, ingresarPrecio(" 8990 "))
        listOf("abc", "-500", "1,5", "1.5").forEach {
            assertFailsWith<IllegalArgumentException> { ingresarPrecio(it) }
        }
    }

    @Test
    fun recorreTodasLasOpcionesConDatosPersistentesEnSesion() {
        val datos = DatosPizzeria()
        val pasos = listOf(
            "abc", "99", "1", "2",
            "3", "Nueva", "Calle 123", "+123456789", "nueva@correo.cl",
            "4", "3", "1", "abc", "0", "1", "0", "Sin sal",
            "5", "4", "6",
            "7", "1", "-1",
            "7", "1", "10",
            "8", "3",
            "9", "3",
            "10", "3",
            "11",
            "12", "1", "abc", "-1", "8500",
            "13", "0"
        ).iterator()
        val salida = capturar { ejecutarConsola(datos) { if (pasos.hasNext()) pasos.next() else null } }
        assertFalse(pasos.hasNext())
        assertTrue(salida.contains("Hasta pronto."))
        assertTrue(salida.contains("Error de inventario"))
        assertTrue(salida.contains("Error de ingreso"))
        assertTrue(salida.contains("Entrada inválida"))
        assertEquals(3, datos.clientes.size)
        assertEquals("Calle 123", datos.clientes.last().direccion)
        assertEquals(4, datos.pedidos.size)
        assertEquals(EstadoPedido.EN_CAMINO, datos.pedidos[2].estado)
        assertEquals("Marta Vidal", datos.pedidos[2].repartidor?.nombre)
        assertEquals(9, datos.inventario["aceitunas"])
        assertEquals(8500, datos.menu[0].precio)
        assertEquals(7990, datos.pedidos.last().productos.single().precio)
    }

    @Test
    fun finDeEntradaNoGuardaRegistrosParciales() {
        val datos = DatosPizzeria()
        val pasos = listOf("3", "Parcial").iterator()
        val salida = capturar { ejecutarConsola(datos) { if (pasos.hasNext()) pasos.next() else null } }
        assertEquals(2, datos.clientes.size)
        assertTrue(salida.contains("Fin de entrada"))
    }

    @Test
    fun reporteSeparaCanceladosYNotificaSoloClientesRegistrados() {
        val datos = DatosPizzeria()
        val salida = capturar {
            datos.pedidos.forEach { aceptarPedido(it, datos.inventario) }
            imprimirReporte(datos.pedidos.toTypedArray())
        }
        assertTrue(salida.contains("Completados: 0 | Pendientes: 2 | Cancelados: 1"))
        assertTrue(salida.contains("A pagar: \$0"))
        assertEquals("", capturar { notificarEstado(datos.pedidos.last()) })
        assertTrue(capturar { notificarEstado(datos.pedidos.first()) }.contains("Ana Morales"))
    }
}
