# Pizzería – Aplicando utilidades de Kotlin

Karla Santinbáñez Gutiérrez · Actividad formativa 2 · S2 · Desarrollo de APP Móviles I(PMY2201) · Duoc UC

## Ejecutar
- IntelliJ IDEA: abrir la carpeta del proyecto (Gradle) y ejecutar `src/main/kotlin/pizzeria/Main.kt`.
- Consola en Windows, con un JDK disponible: `.\gradlew.bat --console=plain run`.
- Compilar: `.\gradlew.bat build`. Ejecutar pruebas: `.\gradlew.bat test`.
- Sin Gradle, con Kotlin instalado, compilar todos los archivos conjuntamente desde PowerShell:

```powershell
$fuentes = Get-ChildItem src\main\kotlin\pizzeria\*.kt | Select-Object -ExpandProperty FullName
kotlinc $fuentes -include-runtime -d pizzeria.jar
java -jar pizzeria.jar
```

## Menú interactivo
Se cargan las cuatro pizzas, el inventario, los dos clientes, los tres repartidores y los
tres pedidos originales. Al iniciar se valida el stock de los pedidos de ejemplo:
Ana queda lista, Pedro en preparación y el pedido Veggie se cancela por falta de
aceitunas. No se repone stock automáticamente.

| Opción | Operación |
|---|---|
| 1 / 2 | Consultar pizzas y precios / clientes y repartidores |
| 3 / 4 | Registrar cliente con dirección y contactos / crear pedido con una o varias pizzas |
| 5 / 6 | Consultar resumen individual / existencias |
| 7 / 8 | Sumar o restar stock / reintentar un pedido cancelado después de reponer |
| 9 / 10 | Marcar listo un pedido aceptado / asignar el repartidor disponible más cercano y notificar |
| 11 / 12 | Consultar reporte del día / actualizar precio sin cambiar pedidos anteriores |
| 13 / 0 | Ejecutar la demostración original con datos independientes / salir |

Las entradas numéricas inválidas se solicitan nuevamente. Los textos opcionales vacíos
usan valores predeterminados; se informa cuando se reemplaza un contacto inválido.
El fin de entrada cierra la sesión, incluso durante un registro, sin guardar datos parciales.
Cada pedido admite hasta 100 pizzas. Los datos permanecen durante la sesión, no se guardan
en disco. La demostración independiente no altera el inventario ni los pedidos del menú.

## Estructura
| Archivo | Contenido |
|---|---|
| `Modelos.kt` | `EstadoPedido` (enum), `Producto`, `Usuario` (open) → `Cliente` / `Repartidor` (polimorfismo con `rol()`), `Pedido` |
| `Validaciones.kt` | Validación de teléfono y correo con valores predeterminados; `ingresarPrecio` con excepciones |
| `Inventario.kt` | `calcularNecesidad`, `verificarDisponibilidad` (`when`), `descontarInventario`, `ajustarStock`, `aceptarPedido` |
| `Pedidos.kt` | Totales (IVA 19 %, descuento 10 % si hay más de 3 pizzas), reporte con `for`, resumen con plantillas, `asignarRepartidor` (`find`), `notificarEstado` (`?.let`) |
| `Datos.kt` | Datos originales compartidos por consola y demostración, con instancias independientes |
| `Consola.kt` / `Main.kt` | Menú, captura de datos y punto de entrada |
| `Demostracion.kt` | Caso original de punta a punta, disponible desde la opción 13 |

## Criterios de decisión
- Precios en pesos chilenos (enteros, sin decimales); IVA 19 % aplicado sobre el valor con descuento.
- Cada elemento de `ingredientes` de un producto equivale a 1 unidad por pizza.
- Pedidos "completados" = `En camino`; "pendientes" = `En preparación` y `Listo`.
- Solo se asigna repartidor a pedidos en estado `Listo`; el más cercano disponible se elige ordenando por `distanciaKm` y usando `find`.
- Los pedidos sin cliente registrado (`cliente = null`) se permiten; no reciben notificación.
- La falta de stock cancela el pedido sin descontar ingredientes. Reintentar después de reponer
  lo devuelve a preparación. Un pedido aceptado no vuelve a descontar stock.
- El resumen y el reporte distinguen total calculado y monto a pagar (cero si está cancelado).
- `.gitignore` excluye cachés Gradle/Kotlin, IDE, compilados y archivos ZIP/RAR; conserva el JAR del wrapper.

## Revisión del feedback del profesor (semana 1)
| Observación | Implementación |
|---|---|
| Distinguir total y cobro en cancelados | Estado `CANCELADO` y `montoAPagar()`; se muestran ambos valores |
| Mostrar existencias restantes | `aceptarPedido` imprime los ingredientes actualizados después de confirmar |
| Incorporar subtotal y monto de descuento | El resumen conserva subtotal, descuento monetario, porcentaje e IVA |
| Incluir instrucciones de compilación conjunta | Comandos Gradle y `kotlinc` en la sección Ejecutar |
| Resumen completo | Identificador, dirección, contactos, precios, cantidad, totales, estado y repartidor |
| Validar teléfono | Prefijo `+` opcional y 9–15 dígitos; admite espacios y conserva formato de móviles chilenos |

Se mantienen las decisiones de semana 2, distintas a las descritas en la evaluación de
semana 1: precios enteros CLP (no coma ni punto decimal), IVA del 19 % y una unidad por
elemento de la receta (no dos unidades genéricas por pizza). El precio admite cero, como
en la implementación original. El descuento sigue siendo del 10 % a partir de cuatro pizzas.
Las pruebas automatizadas cubren las mejoras y el recorrido de todas las opciones del menú.
