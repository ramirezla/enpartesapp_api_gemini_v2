package com.ehome.enpartesapp.ui.presupuesto

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.ehome.enpartesapp.R
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportDisplayFragment : Fragment() {

    private var tvCaseNumberValue: TextView? = null
    private var tvInspectionDateValue: TextView? = null
    private var tvBrandValue: TextView? = null
    private var tvModelValue: TextView? = null
    private var tvVinValue: TextView? = null
    private var tvYearValue: TextView? = null
    private var tvColorValue: TextView? = null
    private var tvLocationValue: TextView? = null
    private var tvInspectorNameValue: TextView? = null
    private var tvInspectorEmailValue: TextView? = null
    private var tvCostPerHourValue: TextView? = null

    companion object {
        private const val REQUEST_CODE_CREATE_FILE = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_report_display, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicialización de vistas
        tvCaseNumberValue = view.findViewById(R.id.tvCaseNumberValue)
        tvInspectionDateValue = view.findViewById(R.id.tvInspectionDateValue)
        tvBrandValue = view.findViewById(R.id.tvBrandValue)
        tvModelValue = view.findViewById(R.id.tvModelValue)
        tvVinValue = view.findViewById(R.id.tvVinValue)
        tvYearValue = view.findViewById(R.id.tvYearValue)
        tvColorValue = view.findViewById(R.id.tvColorValue)
        tvLocationValue = view.findViewById(R.id.tvLocationValue)
        tvInspectorNameValue = view.findViewById(R.id.tvInspectorNameValue)
        tvInspectorEmailValue = view.findViewById(R.id.tvInspectorEmailValue)
        tvCostPerHourValue = view.findViewById(R.id.tvCostPerHourValue)

        //val btnBack: Button = view.findViewById(R.id.btnVolver)
        val btnSaveLocal: Button = view.findViewById(R.id.btnSaveLocal)
        val btnSaveDrive: Button = view.findViewById(R.id.btnSaveDrive)

        // Procesar datos de entrada
        arguments?.let {
            val inputData = it.getString("input_data") ?: ""
            val apiResponse = it.getString("api_response") ?: ""
            parseAndPopulateInputData(inputData)
            parseApiResponse(apiResponse)
        }

        // Configurar listeners de botones
//        btnBack.setOnClickListener {
//            findNavController().popBackStack()
//        }

        btnSaveLocal.setOnClickListener {
            saveReportToLocalStorage()
        }

        btnSaveDrive.setOnClickListener {
            saveReportToGoogleDrive()
        }
    }

    private fun saveReportToLocalStorage() {
        val caseNumber = tvCaseNumberValue?.text.toString()
        val inspectionDate = tvInspectionDateValue?.text.toString()
        val apiResponse = arguments?.getString("api_response") ?: run {
            Toast.makeText(requireContext(), "No hay datos de reporte para guardar", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Crear directorio si no existe
            val reportsDir = File(requireContext().getExternalFilesDir(null), "ValoracionDeDannos")
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }

            // Formatear fecha para nombre de archivo
            val safeDate = inspectionDate.replace("/", "-").replace(":", "-")

            // Guardar JSON original
            val jsonFile = File(reportsDir, "Reporte_${caseNumber}_${safeDate}.json")
            jsonFile.writeText(apiResponse)

            // Guardar reporte formateado
            val formattedReport = formatFullReport(apiResponse)
            val reportFile = File(reportsDir, "Reporte_${caseNumber}_${safeDate}.txt")
            reportFile.writeText(formattedReport)

            Toast.makeText(
                requireContext(),
                "Reportes guardados en:\n${reportsDir.absolutePath}",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Error al guardar localmente: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            Log.e("ReportDisplay", "Error saving locally", e)
        }
    }

    private fun saveReportToGoogleDrive() {
        val caseNumber = tvCaseNumberValue?.text.toString()
        val inspectionDate = tvInspectionDateValue?.text.toString()
        val apiResponse = arguments?.getString("api_response") ?: run {
            Toast.makeText(requireContext(), "No hay datos de reporte para guardar", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            //val formattedReport = formatFullReport(apiResponse)
            val safeDate = inspectionDate.replace("/", "-").replace(":", "-")
            val fileName = "Reporte_${caseNumber}_${safeDate}.txt"

            // Crear intent para guardar en Google Drive
            // https://drive.google.com/drive/folders/1P6M15944n7ALXk4HAs1lgb3tZSxSNXBh?usp=drive_link

            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain"
                putExtra(Intent.EXTRA_TITLE, fileName)

                // El ID real de la carpeta de Google Drive
                val folderId = "https://drive.google.com/drive/folders/1P6M15944n7ALXk4HAs1lgb3tZSxSNXBh?usp=drive_link"
                val driveUri = DocumentsContract.buildDocumentUri(
                    "com.google.android.apps.docs.storage",
                    "acc=1;doc=$folderId"
                )

                putExtra(DocumentsContract.EXTRA_INITIAL_URI, driveUri)
            }

//          Cambio para usar drive.google.com/drive/folders/1P6M15944n7ALXk4HAs1lgb3tZSxSNXBh?usp=drive_link
//            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
//                addCategory(Intent.CATEGORY_OPENABLE)
//                type = "text/plain"
//                putExtra(Intent.EXTRA_TITLE, fileName)
//                // Intentar abrir directamente la carpeta ValoracionDeDannos
//                putExtra(DocumentsContract.EXTRA_INITIAL_URI,
//                    "content://com.google.android.apps.docs.storage/document/acc=1;root=ValoracionDeDannos")
//            }

            startActivityForResult(intent, REQUEST_CODE_CREATE_FILE)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Error al preparar para Google Drive: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            Log.e("ReportDisplay", "Error preparing for Drive", e)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_CREATE_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                try {
                    val apiResponse = arguments?.getString("api_response") ?: return
                    val formattedReport = formatFullReport(apiResponse)

                    requireContext().contentResolver.openOutputStream(uri)?.use {
                        it.write(formattedReport.toByteArray())
                    }

                    Toast.makeText(
                        requireContext(),
                        "Reporte guardado en Google Drive",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Error al guardar en Drive: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("ReportDisplay", "Error saving to Drive", e)
                }
            }
        }
    }

    private fun formatFullReport(jsonResponse: String): String {
        val json = JSONObject(jsonResponse)
        val builder = StringBuilder()

        // Encabezado
        builder.append("=== INFORME DE VALORACIÓN DE DAÑOS ===\n\n")
        builder.append("Fecha generación: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}\n\n")

        // Información general
        builder.append("--- INFORMACIÓN GENERAL ---\n")
        builder.append("Número de Caso: ${tvCaseNumberValue?.text}\n")
        builder.append("Fecha de Inspección: ${tvInspectionDateValue?.text}\n")
        builder.append("Inspector: ${tvInspectorNameValue?.text}\n")
        builder.append("Email inspector: ${tvInspectorEmailValue?.text}\n\n")

        // Información del vehículo
        builder.append("--- INFORMACIÓN DEL VEHÍCULO ---\n")
        builder.append("Marca: ${tvBrandValue?.text}\n")
        builder.append("Modelo: ${tvModelValue?.text}\n")
        builder.append("Año: ${tvYearValue?.text}\n")
        builder.append("Color: ${tvColorValue?.text}\n")
        builder.append("VIN: ${tvVinValue?.text}\n")
        builder.append("Ubicación: ${tvLocationValue?.text}\n")
        builder.append("Costo por hora: ${tvCostPerHourValue?.text}\n\n")

        // Descripción de daños
        builder.append("--- DESCRIPCIÓN DE DAÑOS ---\n")
        val damage = json.optJSONObject("DescripcionDanosExistentes")
        damage?.keys()?.forEach { key ->
            builder.append("$key: ${damage.getString(key)}\n")
        }
        builder.append("\n")

        // Piezas afectadas y costos
        builder.append("--- PIEZAS AFECTADAS Y COSTOS ---\n")
        val piezas = json.optJSONArray("ListadoPiezasAfectadas")
        //val costoHora = json.getJSONObject("DatosGenerales").getDouble("CostoHoraManoObra")
        val costoHora = json.optJSONObject("DatosGenerales")?.optDouble("CostoHoraManoObra", 0.0) ?: 0.0
        var totalManoObra = 0.0
        var totalPiezas = 0.0

        if (piezas != null) {
            for (i in 0 until piezas.length()) {
                val pieza = piezas.getJSONObject(i)
                val nombre = pieza.optString("Pieza", "Pieza desconocida")
                val accion = pieza.optString("Accion", "N/A")
                val costoPieza = pieza.optDouble("CostoPieza", 0.0)
                val manoObra = getManoObraObject(pieza)

                builder.append("$nombre ($accion)\n")
                manoObra?.keys()?.forEach { tipo ->
                    if (tipo != "TotalHoras") {
                        val horas = manoObra.optDouble(tipo, 0.0)
                        val costo = horas * costoHora
                        builder.append("  $tipo: $${"%.2f".format(costo)} (${horas}h * $${costoHora}/h)\n")
                        totalManoObra += costo
                    }
                }
                builder.append("  Costo pieza: $${"%.2f".format(costoPieza)}\n\n")
                totalPiezas += costoPieza
            }
        }

        // Totales
        builder.append("--- TOTALES ---\n")
        builder.append("Mano de obra: $${"%.2f".format(totalManoObra)}\n")
        builder.append("Piezas: $${"%.2f".format(totalPiezas)}\n")
        builder.append("TOTAL: $${"%.2f".format(totalManoObra + totalPiezas)}\n\n")

        // Consideraciones adicionales
        builder.append("--- CONSIDERACIONES ADICIONALES ---\n")
        val consideraciones = json.optJSONArray("ConsideracionesAdicionales")
        if (consideraciones != null) {
            for (i in 0 until consideraciones.length()) {
                builder.append("- ${consideraciones.getString(i)}\n")
            }
        }

        return builder.toString()
    }

    private fun getManoObraObject(pieza: JSONObject): JSONObject? {
        val possibleKeys = listOf("CantidadEstimadoManoObra", "ManoObra", "ManoDeObra",
            "HorasManoObra", "HorasManoDeObra", "Trabajo", "Labores")

        for (key in possibleKeys) {
            val value = pieza.opt(key)
            if (value is JSONObject) {
                return value
            } else if (value is Number) {
                // Si es un número (horas), lo envolvemos en un JSONObject para que el resto de la lógica funcione
                return JSONObject().put("Mano de Obra ($key)", value.toDouble())
            }
        }
        return null
    }

    private fun JSONObject.getFirstMatchingKey(vararg keys: String): JSONObject? {
        for (key in keys) {
            val value = this.opt(key)
            if (value is JSONObject) return value
        }
        return null
    }

    private fun parseApiResponse(apiResponse: String) {
        val json = JSONObject(apiResponse)

        // 1. Descripción de daños
        val damageCard = view?.findViewById<View>(R.id.cvDamageDescription)
        val damageText = view?.findViewById<TextView>(R.id.tvDamageDescriptionContent)
        val damage = json.optJSONObject("DescripcionDanosExistentes")
        if (damage != null) {
            val damageFormatted = damage.keys().asSequence().joinToString("\n\n") { key ->
                "$key: ${damage.getString(key)}"
            }
            damageText?.text = damageFormatted
            damageCard?.visibility = View.VISIBLE
        }

        // 2. Listado de piezas
        val partsCard = view?.findViewById<View>(R.id.cvAffectedParts)
        val container = view?.findViewById<LinearLayout>(R.id.llAffectedPartsContainer)
        val piezas = json.optJSONArray("ListadoPiezasAfectadas")
        //val costoHora = json.getJSONObject("DatosGenerales").getDouble("CostoHoraManoObra")
        val costoHora = json.optJSONObject("DatosGenerales")?.optDouble("CostoHoraManoObra", 0.0) ?: 0.0

        var totalManoObra = 0.0
        var totalPiezas = 0.0

        if (piezas != null) {
            for (i in 0 until piezas.length()) {
                val pieza = piezas.getJSONObject(i)
                val nombre = pieza.optString("Pieza", "Pieza desconocida")
                val accion = pieza.optString("Accion", "N/A")
                val costoPieza = pieza.optDouble("CostoPieza", 0.0)
                val manoObra = getManoObraObject(pieza)

                val piezaTextView = TextView(requireContext())
                piezaTextView.text = buildString {
                    append("$nombre\n")
                    manoObra?.keys()?.forEach { tipo ->
                        if (tipo != "TotalHoras") {
                            val horas = manoObra.optDouble(tipo, 0.0)
                            val costo = horas * costoHora
                            append("  Costo de $tipo: $%.2f (%.1f horas * $%.2f/hora)\n".format(costo, horas, costoHora))
                            totalManoObra += costo
                        }
                    }
                    append("  Costo de Pieza ($accion): $%.2f\n".format(costoPieza))
                    totalPiezas += costoPieza
                }
                piezaTextView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                piezaTextView.setPadding(0, 0, 0, 16)
                container?.addView(piezaTextView)
            }
        }

        view?.findViewById<TextView>(R.id.tvTotalLaborCost)?.text = "$%.2f".format(totalManoObra)
        view?.findViewById<TextView>(R.id.tvTotalPartsCost)?.text = "$%.2f".format(totalPiezas)
        view?.findViewById<TextView>(R.id.tvGrandTotalCost)?.text = "$%.2f".format(totalManoObra + totalPiezas)
        partsCard?.visibility = View.VISIBLE

        // 3. Consideraciones adicionales
        val considerationsCard = view?.findViewById<View>(R.id.cvAdditionalConsiderations)
        val considerationsText = view?.findViewById<TextView>(R.id.tvAdditionalConsiderationsContent)
        val consideraciones = json.optJSONArray("ConsideracionesAdicionales")
        if (consideraciones != null) {
            val formatted = (0 until consideraciones.length()).joinToString("\n\n") {
                "- ${consideraciones.getString(it)}"
            }
            considerationsText?.text = formatted
            considerationsCard?.visibility = View.VISIBLE
        }
    }

    private fun parseAndPopulateInputData(inputData: String) {
        val lines = inputData.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val dataMap = mutableMapOf<String, String>()
        var currentSectionKey = ""

        for (line in lines) {
            when {
                line.startsWith("--- Información General ---") -> currentSectionKey = "general"
                line.startsWith("--- Información Vehiculo ---") -> currentSectionKey = "vehicle"
                line.startsWith("--- Ubicación ---") -> currentSectionKey = "location"
                line.startsWith("--- Información Inspector ---") -> currentSectionKey = "inspector"
                line.startsWith("--- Información de Costos ---") -> currentSectionKey = "costs"
                line.startsWith("---------------------------") -> currentSectionKey = ""
                line.contains(":") && currentSectionKey.isNotEmpty() -> {
                    val parts = line.split(":", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim()
                        when (currentSectionKey) {
                            "general" -> {
                                if (key == "Número de Caso") dataMap["caseNumber"] = value
                                else if (key == "Fecha de Inspección") dataMap["inspectionDate"] = value
                            }
                            "vehicle" -> {
                                if (key == "Marca") dataMap["brand"] = value
                                else if (key == "Modelo") dataMap["model"] = value
                                else if (key == "Número de VIN") dataMap["vin"] = value
                                else if (key == "Año") dataMap["year"] = value
                                else if (key == "Color") dataMap["color"] = value
                            }
                            "location" -> if (key == "Ubicación de Valoración") dataMap["location"] = value
                            "inspector" -> {
                                if (key == "Nombre Completo") dataMap["inspectorName"] = value
                                else if (key == "Email") dataMap["inspectorEmail"] = value
                            }
                            "costs" -> if (key == "Costo por hora de mano de obra") dataMap["costPerHour"] = value
                        }
                    }
                }
            }
        }

        tvCaseNumberValue?.text = dataMap["caseNumber"] ?: "N/A"
        tvInspectionDateValue?.text = dataMap["inspectionDate"] ?: "N/A"
        tvBrandValue?.text = dataMap["brand"] ?: "N/A"
        tvModelValue?.text = dataMap["model"] ?: "N/A"
        tvVinValue?.text = dataMap["vin"] ?: "N/A"
        tvYearValue?.text = dataMap["year"] ?: "N/A"
        tvColorValue?.text = dataMap["color"] ?: "N/A"
        tvLocationValue?.text = dataMap["location"] ?: "N/A"
        tvInspectorNameValue?.text = dataMap["inspectorName"] ?: "N/A"
        tvInspectorEmailValue?.text = dataMap["inspectorEmail"] ?: "N/A"
        tvCostPerHourValue?.text = dataMap["costPerHour"] ?: "N/A"
    }
}