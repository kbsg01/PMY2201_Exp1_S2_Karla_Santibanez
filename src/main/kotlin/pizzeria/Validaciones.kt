package pizzeria

const val TELEFONO_PREDETERMINADO = "(000) 000-0000"
const val CORREO_PREDETERMINADO = "correo@invalido.com"

// Se permiten espacios de presentación; el número debe tener entre 9 y 15 dígitos.
private val REGEX_TELEFONO = Regex("""^\+?[0-9]{9,15}$""")
private val REGEX_CORREO = Regex("""^[\w.+-]+@[\w-]+(\.[\w-]+)+$""")

// Devuelve el teléfono normalizado o el valor predeterminado si el formato es incorrecto
fun validarTelefono(entrada: String?): String {
    val limpio = entrada?.trim().orEmpty().filterNot { it.isWhitespace() }
    if (!REGEX_TELEFONO.matches(limpio)) return TELEFONO_PREDETERMINADO

    val digitos = limpio.removePrefix("+")
    val movilChileno = when {
        digitos.length == 9 && digitos.startsWith("9") -> digitos
        digitos.length == 11 && digitos.startsWith("569") -> digitos.takeLast(9)
        else -> null
    }
    return if (movilChileno != null) {
        "+56 ${movilChileno[0]} ${movilChileno.substring(1, 5)} ${movilChileno.substring(5)}"
    } else {
        limpio
    }
}

// Devuelve el correo en minúsculas o el valor predeterminado si el formato es incorrecto
fun validarCorreo(entrada: String?): String {
    val limpio = entrada?.trim()?.lowercase() ?: return CORREO_PREDETERMINADO
    return if (REGEX_CORREO.matches(limpio)) limpio else CORREO_PREDETERMINADO
}

// Convierte el texto ingresado en un costo válido; lanza IllegalArgumentException si no lo es
fun ingresarPrecio(texto: String): Int {
    val valor = texto.trim().toIntOrNull()
        ?: throw IllegalArgumentException("El costo '$texto' no es un número válido.")
    require(valor >= 0) { "El costo no puede ser negativo (ingresaste $valor)." }
    return valor
}
