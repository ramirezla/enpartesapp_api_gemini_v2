import android.util.Log
import org.json.JSONObject
import java.text.DecimalFormat

class ReportGenerator {

    fun generateDetailedCostReport(jsonString: String): String {
        val reportBuilder = StringBuilder()
        val decimalFormat = DecimalFormat("#,##0.00") // To format costs

        try {
            val jsonObject = JSONObject(jsonString)

            // Extract General Data
            val datosGenerales = jsonObject.getJSONObject("DatosGenerales")
            val marca = datosGenerales.getString("Marca")
            val modelo = datosGenerales.getString("Modelo")
            val ano = datosGenerales.getInt("Año")
            val color = datosGenerales.getString("Color")
            val ubicacionValoracion = datosGenerales.getString("UbicaciónValoracion")
            // Ensure correct key access and type for CostoHoraManoObra
            val costoHoraManoObra = datosGenerales.getDouble("CostoHoraManoObra")

            // Extract Inspector Information (if available in this JSON or needs to be passed)
            // For now, let's assume it's hardcoded or passed separately if not in JSON
            val numeroCaso = "caso4" // Assuming this comes from etCaseNumber or similar
            val fechaInspeccion = "2025-07-11" // Assuming this comes from etDateOfInspection or similar
            val nombreCompletoInspector = "Luis Ramirez" // Placeholder
            val emailInspector = "ramirezgluisalberto@gmail.com" // Placeholder
            val vin = "tgv" // Assuming this comes from etVIN or similar

            reportBuilder.append("--- Información General ---\n")
            reportBuilder.append("Número de Caso: ").append(numeroCaso).append("\n")
            reportBuilder.append("Fecha de Inspección: ").append(fechaInspeccion).append("\n")
            reportBuilder.append("--- Información Vehiculo ---\n")
            reportBuilder.append("Marca: ").append(marca).append("\n")
            reportBuilder.append("Modelo: ").append(modelo).append("\n")
            reportBuilder.append("Número de VIN: ").append(vin).append("\n")
            reportBuilder.append("Año: ").append(ano).append("\n")
            reportBuilder.append("Color: ").append(color).append("\n")
            reportBuilder.append("--- Ubicación ---\n")
            reportBuilder.append("Ubicación de Valoración: ").append(ubicacionValoracion).append("\n")
            reportBuilder.append("--- Información Inspector ---\n")
            reportBuilder.append("Nombre Completo: ").append(nombreCompletoInspector).append("\n")
            reportBuilder.append("Email: ").append(emailInspector).append("\n")
            reportBuilder.append("--- Información de Costos ---\n")
            reportBuilder.append("Costo por hora de mano de obra: ").append(decimalFormat.format(costoHoraManoObra)).append("\n")
            reportBuilder.append("---------------------------\n\n")

            reportBuilder.append("--- Reporte de Costos de Reparación ---\n")
            reportBuilder.append("**Detalle de Costos por Pieza Afectada:**\n\n")

            val listadoPiezasAfectadas = jsonObject.getJSONArray("ListadoPiezasAfectadas")

            var totalCostoManoObraGeneral = 0.0
            var totalCostoPiezaMinGeneral = 0.0
            var totalCostoPiezaMaxGeneral = 0.0 // Assuming max cost is same as min if not specified

            for (i in 0 until listadoPiezasAfectadas.length()) {
                val piezaAfectada = listadoPiezasAfectadas.getJSONObject(i)
                val piezaNombre = piezaAfectada.getString("Pieza")
                val accion = piezaAfectada.getString("Accion")
                val costoPieza = piezaAfectada.getDouble("CostoPieza")

                // Extract ManoObra details
                val manoObra = piezaAfectada.getJSONObject("ManoObra")
                var horasManoObraPieza = 0.0

                // Accumulate hours for each type of labor
                if (manoObra.has("Desmontaje")) {
                    horasManoObraPieza += manoObra.getDouble("Desmontaje")
                }
                if (manoObra.has("Montaje")) {
                    horasManoObraPieza += manoObra.getDouble("Montaje")
                }
                if (manoObra.has("Pintura")) {
                    horasManoObraPieza += manoObra.getDouble("Pintura")
                }
                if (manoObra.has("ReparacionEstructural")) {
                    horasManoObraPieza += manoObra.getDouble("ReparacionEstructural")
                }
                // If there are other labor types, add them here

                // Calculate estimated labor cost for the current piece
                val costoEstimadoManoObra = horasManoObraPieza * costoHoraManoObra

                // For piece cost, if there's no range, min and max are the same
                val costoEstimadoPiezaMin = costoPieza
                val costoEstimadoPiezaMax = costoPieza // Assuming no range for now, or you'd extract a "CostoPiezaMax" if it existed

                // Add to general totals
                totalCostoManoObraGeneral += costoEstimadoManoObra
                totalCostoPiezaMinGeneral += costoEstimadoPiezaMin
                totalCostoPiezaMaxGeneral += costoEstimadoPiezaMax // If there's a range, this would be different

                reportBuilder.append("### ").append(piezaNombre).append("\n")
                reportBuilder.append("  Acción Sugerida: ").append(accion).append("\n")
                reportBuilder.append("  Costo Estimado de Mano de Obra: $").append(decimalFormat.format(costoEstimadoManoObra)).append("\n")
                reportBuilder.append("  Subtotal de Costo de Piezas/Materiales: $").append(decimalFormat.format(costoEstimadoPiezaMin))
                .append(" - $").append(decimalFormat.format(costoEstimadoPiezaMax)).append("\n\n")
            }

            reportBuilder.append("---\n\n")
            reportBuilder.append("**Resumen de Costos Totales del Presupuesto:**\n\n")
            reportBuilder.append("* **Costo Total Estimado de Mano de Obra:** $").append(decimalFormat.format(totalCostoManoObraGeneral)).append("\n")
            reportBuilder.append("* **Costo Total Estimado de Piezas/Materiales:** $").append(decimalFormat.format(totalCostoPiezaMinGeneral))
            .append(" - $").append(decimalFormat.format(totalCostoPiezaMaxGeneral)).append("\n")
            reportBuilder.append("* **Costo Total General Estimado (Mano de Obra + Piezas):** $").append(decimalFormat.format(totalCostoManoObraGeneral + totalCostoPiezaMinGeneral))
            .append(" - $").append(decimalFormat.format(totalCostoManoObraGeneral + totalCostoPiezaMaxGeneral)).append("\n")

        } catch (e: Exception) {
            reportBuilder.clear()
            reportBuilder.append("Error al generar el reporte de costos detallado: ${e.message}\n")
            reportBuilder.append("Asegúrate de que el JSON proporcionado sea válido y tenga la estructura esperada por el Reporte 2.")
            Log.e("ReportGenerator", "Error generating detailed cost report: ${e.message}", e)
        }
        return reportBuilder.toString()
    }
}
