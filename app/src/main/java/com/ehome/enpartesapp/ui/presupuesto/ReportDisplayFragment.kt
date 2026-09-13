package com.ehome.enpartesapp.ui.presupuesto

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.ehome.enpartesapp.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
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

        val llPhotosContainer: LinearLayout = view.findViewById(R.id.llPhotosContainer)
        val cvAnalyzedPhotos: View = view.findViewById(R.id.cvAnalyzedPhotos)

        //val btnBack: Button = view.findViewById(R.id.btnVolver)
        val btnSaveLocal: Button = view.findViewById(R.id.btnSaveLocal)
        val btnSaveDrive: Button = view.findViewById(R.id.btnSaveDrive)
        val btnShare: Button = view.findViewById(R.id.btnShareReport)

        arguments?.let {
            val inputData = it.getString("input_data") ?: ""
            val apiResponse = it.getString("api_response") ?: ""
            val photoUris = it.getStringArrayList("photo_uris")
            
            parseAndPopulateInputData(inputData)
            parseApiResponse(apiResponse)
            
            if (!photoUris.isNullOrEmpty()) {
                cvAnalyzedPhotos.visibility = View.VISIBLE
                photoUris.forEach { uriString ->
                    val imageView = ImageView(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(400, 400).apply {
                            setMargins(0, 0, 16, 0)
                        }
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        setImageURI(Uri.parse(uriString))
                        setOnClickListener {
                            // Opcional: mostrar imagen en pantalla completa o diálogo
                        }
                    }
                    llPhotosContainer.addView(imageView)
                }
            }
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

        btnShare.setOnClickListener {
            shareReportPdf()
        }
    }

    private fun shareReportPdf() {
        val apiResponse = arguments?.getString("api_response") ?: return
        val formattedReport = formatFullReport(apiResponse)
        val photoUris = arguments?.getStringArrayList("photo_uris") ?: arrayListOf()
        
        try {
            val tempFile = File(requireContext().cacheDir, "Valoracion_Danos.pdf")
            generatePdfReport(formattedReport, photoUris, tempFile)
            
            val contentUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                tempFile
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Compartir Reporte con:"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al compartir: ${e.message}", Toast.LENGTH_SHORT).show()
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

            // Generar y guardar PDF
            val photoUris = arguments?.getStringArrayList("photo_uris") ?: arrayListOf()
            val pdfFile = File(reportsDir, "Reporte_${caseNumber}_${safeDate}.pdf")
            generatePdfReport(formattedReport, photoUris, pdfFile)

            Toast.makeText(
                requireContext(),
                "Reportes guardados (JSON, TXT, PDF) en:\n${reportsDir.absolutePath}",
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
        
        if (arguments?.getString("api_response").isNullOrBlank()) {
            Toast.makeText(requireContext(), "No hay datos de reporte para guardar", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val safeDate = inspectionDate.replace("/", "-").replace(":", "-")
            val fileName = "Reporte_${caseNumber}_${safeDate}.pdf"

            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/pdf"
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
                    val photoUris = arguments?.getStringArrayList("photo_uris") ?: arrayListOf()

                    // Creamos un archivo temporal para generar el PDF
                    val tempFile = File(requireContext().cacheDir, "temp_report.pdf")
                    generatePdfReport(formattedReport, photoUris, tempFile)

                    // Escribimos el contenido del archivo temporal al URI de Drive
                    requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                        tempFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    
                    // Borramos el temporal
                    tempFile.delete()

                    Toast.makeText(
                        requireContext(),
                        "Reporte PDF guardado en Google Drive",
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

        // Obtener el costo por hora de forma robusta
        val costoHoraStr = tvCostPerHourValue?.text?.toString() ?: "0"
        val costoHora = extractDouble(costoHoraStr).let { if (it == 0.0) json.findFirstDoubleIgnoreCase("CostoHoraManoObra", "CostoHoraManoObraUSD", "ManoObraCosto", defaultValue = 20.0) else it }

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
        val damage = json.optJSONObjectIgnoreCase("DescripcionDanosExistentes")
        damage?.keys()?.forEach { key ->
            builder.append("$key: ${damage.getString(key)}\n")
        }
        builder.append("\n")

        // Piezas afectadas y costos
        builder.append("--- PIEZAS AFECTADAS Y COSTOS ---\n")
        val piezas = json.optJSONArrayIgnoreCase("ListadoPiezasAfectadas")
        
        var totalManoObra = 0.0
        var totalPiezas = 0.0

        if (piezas != null) {
            for (i in 0 until piezas.length()) {
                val pieza = piezas.getJSONObject(i)
                
                // Mapeo flexible e insensible a mayúsculas
                val nombre = pieza.optStringIgnoreCase("pieza", "Pieza desconocida")
                // Soporte especial para sugerencia/accion
                val accion = pieza.findFirstStringIgnoreCase("suguerencia", "sugerencia", "accion", "Accion", defaultValue = "N/A")
                val costoPieza = pieza.findFirstDoubleIgnoreCase("CostoPieza", "CostoMateriales", "CostoReparacion", "monto", defaultValue = 0.0)
                val manoObra = getManoObraObject(pieza)

                builder.append("$nombre ($accion)\n")
                var subtotalManoObraItem = 0.0
                manoObra?.keys()?.forEach { tipo ->
                    if (!tipo.equals("TotalHoras", ignoreCase = true)) {
                        val horas = manoObra.optDouble(tipo, 0.0)
                        val costo = horas * costoHora
                        
                        // Formatear línea de mano de obra
                        val moLine = "  $tipo: $${"%.2f".format(costo)} (${horas}h * $${costoHora}/h)"
                        
                        // Word wrap para líneas de mano de obra largas en el PDF
                        builder.append(moLine).append("\n")
                        subtotalManoObraItem += costo
                    }
                }
                totalManoObra += subtotalManoObraItem
                
                val labelCosto = if (accion.equals("Reparar", ignoreCase = true)) "Costo Reparación/Mat." else "Costo pieza"
                builder.append("  $labelCosto: $${"%.2f".format(costoPieza)}\n")
                builder.append("  SUBTOTAL ÍTEM: $${"%.2f".format(subtotalManoObraItem + costoPieza)}\n\n")
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
        val consideraciones = json.optJSONArrayIgnoreCase("ConsideracionesAdicionales")
        if (consideraciones != null) {
            for (i in 0 until consideraciones.length()) {
                builder.append("- ${consideraciones.getString(i)}\n")
            }
        }

        return builder.toString()
    }

    private fun generatePdfReport(reportText: String, photoUris: List<String>, outputFile: File) {
        val pdfDocument = PdfDocument()
        
        // Pinceles para diseño
        val paint = Paint()
        val headerPaint = Paint().apply { color = Color.parseColor("#1976D2") } // Azul
        val titlePaint = Paint().apply { 
            color = Color.WHITE
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val sectionTitlePaint = Paint().apply {
            color = Color.parseColor("#1976D2")
            textSize = 16f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val labelPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val contentPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 10f
            isAntiAlias = true
        }

        val pageWidth = 595
        val pageHeight = 842
        var pageNumber = 1
        val margin = 50f
        val contentWidth = pageWidth - (margin * 2)

        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        var y = 0f

        // Función para nueva página
        fun startNewPage() {
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            
            // Pie de página en cada página
            canvas.drawText("Página $pageNumber | Generado por enpartesapp AI", margin, pageHeight - 30f, footerPaint)
            y = 50f
        }

        // --- ENCABEZADO ---
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 100f, headerPaint)
        canvas.drawText("REPORTE DE VALORACIÓN", margin, 60f, titlePaint)
        
        // Dibujar Logo si existe
        try {
            val logo = BitmapFactory.decodeResource(resources, R.drawable.logo_en_partes)
            if (logo != null) {
                val ratio = logo.width.toFloat() / logo.height.toFloat()
                val targetHeight = 70f
                val targetWidth = targetHeight * ratio
                val scaledLogo = Bitmap.createScaledBitmap(logo, targetWidth.toInt(), targetHeight.toInt(), true)
                canvas.drawBitmap(scaledLogo, pageWidth - margin - targetWidth, 15f, null)
            }
        } catch (e: Exception) { /* Ignorar si no hay logo */ }

        y = 130f
        canvas.drawText("Página $pageNumber | Generado por enpartesapp AI", margin, pageHeight - 30f, footerPaint)

        val lines = reportText.split("\n")
        for (line in lines) {
            if (line.isBlank()) {
                y += 10f
                continue
            }

            if (y > pageHeight - 80f) startNewPage()

            when {
                line.startsWith("===") -> { /* Saltamos el encabezado viejo */ }
                line.startsWith("---") -> {
                    y += 20f
                    val cleanTitle = line.replace("-", "").trim()
                    canvas.drawText(cleanTitle, margin, y, sectionTitlePaint)
                    y += 10f
                    canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                    y += 20f
                }
                line.contains(":") -> {
                    val parts = line.split(":", limit = 2)
                    val label = parts[0].trim() + ":"
                    val content = parts[1].trim()
                    
                    canvas.drawText(label, margin, y, labelPaint)
                    
                    val labelWidth = labelPaint.measureText(label) + 10f
                    
                    // Manejar contenido largo
                    if (contentPaint.measureText(content) > (contentWidth - labelWidth)) {
                        val words = content.split(" ")
                        var currentLine = ""
                        for (word in words) {
                            if (contentPaint.measureText("$currentLine $word") > (contentWidth - labelWidth)) {
                                canvas.drawText(currentLine, margin + labelWidth, y, contentPaint)
                                y += 18f
                                if (y > pageHeight - 80f) {
                                    startNewPage()
                                    canvas.drawText(label + " (cont.)", margin, y, labelPaint)
                                }
                                currentLine = word
                            } else {
                                currentLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                            }
                        }
                        canvas.drawText(currentLine, margin + labelWidth, y, contentPaint)
                    } else {
                        canvas.drawText(content, margin + labelWidth, y, contentPaint)
                    }
                    y += 22f
                }
                else -> {
                    // Manejar líneas largas para Consideraciones Adicionales y otros textos
                    val wrappedLine = if (contentPaint.measureText(line) > contentWidth) {
                        wrapText(line, contentPaint, contentWidth)
                    } else {
                        listOf(line)
                    }
                    
                    for (l in wrappedLine) {
                        canvas.drawText(l, margin, y, contentPaint)
                        y += 18f
                        if (y > pageHeight - 80f) startNewPage()
                    }
                }
            }
        }

        // --- FOTOS ---
        if (photoUris.isNotEmpty()) {
            y += 30f
            if (y > pageHeight - 200f) startNewPage()
            
            canvas.drawText("EVIDENCIA FOTOGRÁFICA", margin, y, sectionTitlePaint)
            y += 10f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 30f

            val photoSize = (contentWidth - 20f) / 2f
            var col = 0
            
            for (uriString in photoUris) {
                try {
                    val uri = Uri.parse(uriString)
                    requireContext().contentResolver.openInputStream(uri)?.use { stream ->
                        val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                        val bitmap = BitmapFactory.decodeStream(stream, null, options)
                        
                        if (bitmap != null) {
                            if (y + photoSize > pageHeight - 80f) startNewPage()

                            val rect = Rect(
                                (margin + col * (photoSize + 20f)).toInt(),
                                y.toInt(),
                                (margin + col * (photoSize + 20f) + photoSize).toInt(),
                                (y + photoSize).toInt()
                            )
                            
                            // Dibujar borde sutil
                            val borderPaint = Paint().apply {
                                color = Color.LTGRAY
                                style = Paint.Style.STROKE
                                strokeWidth = 1f
                            }
                            canvas.drawRect(rect, borderPaint)
                            
                            // Dibujar imagen centrada en el rect
                            canvas.drawBitmap(bitmap, null, rect, null)
                            bitmap.recycle()

                            if (col == 1) {
                                col = 0
                                y += photoSize + 20f
                            } else {
                                col = 1
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PdfReport", "Error con foto: ${e.message}")
                }
            }
        }

        pdfDocument.finishPage(page)
        
        try {
            pdfDocument.writeTo(FileOutputStream(outputFile))
        } catch (e: Exception) {
            Log.e("PdfReport", "Error: ${e.message}")
        } finally {
            pdfDocument.close()
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        for (word in words) {
            if (paint.measureText("$currentLine $word") > maxWidth) {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = word
            } else {
                currentLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)
        return lines
    }

    private fun getManoObraObject(pieza: JSONObject): JSONObject? {
        // Primero buscamos por las llaves conocidas
        val primaryKeys = listOf("CantidadEstimadoManoObra", "ManoObra", "ManoDeObra",
            "HorasManoObra", "HorasManoDeObra", "Trabajo", "Labores")

        for (key in primaryKeys) {
            val actualKey = pieza.findKeyIgnoreCase(key)
            if (actualKey != null) {
                val value = pieza.opt(actualKey)
                if (value is JSONObject) {
                    return value
                } else if (value is Number) {
                    return JSONObject().put("Mano de Obra ($key)", value.toDouble())
                }
            }
        }

        // Si no se encontró, buscamos cualquier llave que contenga "hora", "mano" o "cantidad" y sea un número
        val keys = pieza.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val lowerKey = key.lowercase()
            if ((lowerKey.contains("hora") || lowerKey.contains("mano") || lowerKey.contains("cantidad")) && 
                pieza.opt(key) is Number && !lowerKey.contains("costo")) {
                return JSONObject().put("Mano de Obra ($key)", pieza.optDouble(key))
            }
        }
        
        return null
    }

    // Extensiones para manejo de JSON insensible a mayúsculas/minúsculas
    private fun JSONObject.findKeyIgnoreCase(targetKey: String): String? {
        val keys = this.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key.equals(targetKey, ignoreCase = true)) return key
        }
        return null
    }

    private fun JSONObject.optStringIgnoreCase(key: String, defaultValue: String = ""): String {
        val actualKey = findKeyIgnoreCase(key)
        return if (actualKey != null) this.optString(actualKey, defaultValue) else defaultValue
    }

    private fun JSONObject.optDoubleIgnoreCase(key: String, defaultValue: Double = 0.0): Double {
        val actualKey = findKeyIgnoreCase(key)
        return if (actualKey != null) this.optDouble(actualKey, defaultValue) else defaultValue
    }

    private fun JSONObject.optJSONObjectIgnoreCase(key: String): JSONObject? {
        val actualKey = findKeyIgnoreCase(key)
        return if (actualKey != null) this.optJSONObject(actualKey) else null
    }

    private fun JSONObject.optJSONArrayIgnoreCase(key: String): JSONArray? {
        val actualKey = findKeyIgnoreCase(key)
        return if (actualKey != null) this.optJSONArray(actualKey) else null
    }

    private fun JSONObject.findFirstStringIgnoreCase(vararg keys: String, defaultValue: String = ""): String {
        for (key in keys) {
            val actualKey = findKeyIgnoreCase(key)
            if (actualKey != null) {
                val value = this.optString(actualKey)
                if (value.isNotEmpty()) return value
            }
        }
        return defaultValue
    }

    private fun JSONObject.findFirstDoubleIgnoreCase(vararg keys: String, defaultValue: Double = 0.0): Double {
        for (key in keys) {
            val actualKey = findKeyIgnoreCase(key)
            if (actualKey != null) {
                return this.optDouble(actualKey, defaultValue)
            }
        }
        return defaultValue
    }

    private fun extractDouble(text: String): Double {
        return try {
            text.replace("$", "").replace(",", "").trim().toDouble()
        } catch (e: Exception) {
            0.0
        }
    }

    private fun parseApiResponse(apiResponse: String) {
        val json = JSONObject(apiResponse)

        // Obtener costo hora
        val costoHoraStr = tvCostPerHourValue?.text?.toString() ?: "0"
        val costoHora = extractDouble(costoHoraStr).let { if (it == 0.0) json.findFirstDoubleIgnoreCase("CostoHoraManoObra", "CostoHoraManoObraUSD", "ManoObraCosto", defaultValue = 20.0) else it }

        // 1. Descripción de daños
        val damageCard = view?.findViewById<View>(R.id.cvDamageDescription)
        val damageText = view?.findViewById<TextView>(R.id.tvDamageDescriptionContent)
        val damage = json.optJSONObjectIgnoreCase("DescripcionDanosExistentes")
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
        val piezas = json.optJSONArrayIgnoreCase("ListadoPiezasAfectadas")
        
        var totalManoObra = 0.0
        var totalPiezas = 0.0

        if (piezas != null) {
            for (i in 0 until piezas.length()) {
                val pieza = piezas.getJSONObject(i)
                
                // Mapeo flexible e insensible a mayúsculas para la UI
                val nombre = pieza.optStringIgnoreCase("pieza", "Pieza desconocida")
                val accion = pieza.findFirstStringIgnoreCase("suguerencia", "sugerencia", "accion", "Accion", defaultValue = "N/A")
                val costoPieza = pieza.findFirstDoubleIgnoreCase("CostoPieza", "CostoMateriales", "CostoReparacion", "monto", defaultValue = 0.0)
                val manoObra = getManoObraObject(pieza)

                val piezaTextView = TextView(requireContext())
                piezaTextView.text = buildString {
                    append("$nombre ($accion)\n")
                    var subtotalManoObraItem = 0.0
                    manoObra?.keys()?.forEach { tipo ->
                        if (!tipo.equals("TotalHoras", ignoreCase = true)) {
                            val horas = manoObra.optDouble(tipo, 0.0)
                            val costo = horas * costoHora
                            append("  Costo de $tipo: $%.2f (%.1f horas * $%.2f/hora)\n".format(costo, horas, costoHora))
                            subtotalManoObraItem += costo
                        }
                    }
                    totalManoObra += subtotalManoObraItem
                    
                    val labelCosto = if (accion.equals("Reparar", ignoreCase = true)) "Materiales/Reparación" else "Repuesto"
                    append("  Costo de $labelCosto: $%.2f\n".format(costoPieza))
                    append("  SUBTOTAL: $%.2f\n".format(subtotalManoObraItem + costoPieza))
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
        val consideraciones = json.optJSONArrayIgnoreCase("ConsideracionesAdicionales")
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