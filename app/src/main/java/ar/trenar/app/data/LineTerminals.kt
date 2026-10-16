package ar.trenar.app.data

import java.text.Normalizer

/**
 * Head stations and branch end-points (cabeceras) for each line. Used to label the terminal
 * stations on the live-network map. Matched case/accent-insensitively against catalog names.
 */
object LineTerminals {

    private val byLine: Map<String, Set<String>> = mapOf(
        "Mitre" to setOf(
            "Retiro (LGM)", "Tigre", "Bmé. Mitre", "J.L. Suárez", "Capilla del Señor", "Zárate",
        ),
        "Roca" to setOf(
            "Constitución", "La Plata", "Bosques", "J. M. Gutiérrez", "Ezeiza", "Cañuelas",
            "Alejandro Korn", "Chascomús", "Monte",
        ),
        "Sarmiento" to setOf(
            "Once", "Moreno", "Mercedes", "Lobos", "Luján", "Bragado", "Chivilcoy Sud",
        ),
        "San Martín" to setOf(
            "Retiro (LSM)", "Dr. Cabred", "Pilar", "Mercedes P", "Junin",
        ),
        "Belgrano Sur" to setOf(
            "Dr. A. Sáenz Viad.", "González Catán", "Marinos C. G Belgrano", "Marcos Paz", "20 de Junio",
        ),
        "Tren de la Costa" to setOf("Maipú", "Delta"),
        "Urquiza" to setOf("Federico Lacroze", "General Lemos"),
        "Belgrano Norte" to setOf("Retiro", "Villa Rosa"),
    )

    private val normalized: Map<String, Set<String>> =
        byLine.mapValues { (_, names) -> names.map(::norm).toSet() }

    fun isTerminal(line: String, stationName: String): Boolean =
        normalized[line]?.contains(norm(stationName)) == true

    private fun norm(s: String): String =
        Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
}
