package com.ehome.autoperitajeia.ui.presupuesto

import android.Manifest
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ehome.autoperitajeia.R
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// URL de la API de sulmovsa
private const val BASE_URL = "http://209.126.106.199/"
private const val API_webhook_query = "solmovsa/ApiGestorSiniestros/api/Webhook/webhook-query"
private const val API_generate_pdf = "solmovsa/ApiGestorSiniestros/api/Webhook/generate-pdf"

data class LocalReportFile(
    val file: File,
    val name: String,
    val sizeFormatted: String,
    val dateFormatted: String,
    val isPdf: Boolean
)

class LocalReportAdapter(
    private val reportList: List<LocalReportFile>,
    private val onOpenClick: (LocalReportFile) -> Unit,
    private val onShareClick: (LocalReportFile) -> Unit,
    private val onDeleteClick: (LocalReportFile) -> Unit
) : RecyclerView.Adapter<LocalReportAdapter.ReportViewHolder>() {

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgIcon: ImageView = itemView.findViewById(R.id.imgFileIcon)
        val tvName: TextView = itemView.findViewById(R.id.tvFileName)
        val tvDetails: TextView = itemView.findViewById(R.id.tvFileDetails)
        val btnOpen: Button = itemView.findViewById(R.id.btnOpenFile)
        val btnShare: Button = itemView.findViewById(R.id.btnShareFile)
        val btnDelete: Button = itemView.findViewById(R.id.btnDeleteFile)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_local_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val item = reportList[position]
        holder.tvName.text = item.name
        holder.tvDetails.text = "${item.sizeFormatted} • ${item.dateFormatted}"
        holder.imgIcon.setImageResource(if (item.isPdf) R.drawable.file_format_paper_icon else R.drawable.page_search_icon)

        holder.btnOpen.setOnClickListener { onOpenClick(item) }
        holder.btnShare.setOnClickListener { onShareClick(item) }
        holder.btnDelete.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount(): Int = reportList.size
}

class ConsultaFragment : Fragment() {

    private val requestCodePermissions = 101
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var openDocumentLauncher: ActivityResultLauncher<Array<String>>

    private lateinit var toggleGroupMode: MaterialButtonToggleGroup
    private lateinit var llLocalReportsSection: LinearLayout
    private lateinit var svCloudSearchSection: ScrollView
    private lateinit var rvLocalReports: RecyclerView
    private lateinit var tvEmptyLocalReports: TextView
    private lateinit var btnBrowseFile: Button

    private lateinit var etCaseNumber: EditText
    private lateinit var etCaseToken: EditText
    private lateinit var btnConsultar: Button
    private lateinit var llResultContainer: LinearLayout
    private lateinit var btnDownloadPdf: Button
    private var pdfUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_consulta_siniestro, container, false)

        toggleGroupMode = view.findViewById(R.id.toggleGroupMode)
        llLocalReportsSection = view.findViewById(R.id.llLocalReportsSection)
        svCloudSearchSection = view.findViewById(R.id.svCloudSearchSection)
        rvLocalReports = view.findViewById(R.id.rvLocalReports)
        tvEmptyLocalReports = view.findViewById(R.id.tvEmptyLocalReports)

        etCaseNumber = view.findViewById(R.id.etCaseNumber)
        etCaseToken = view.findViewById(R.id.etCaseToken)
        btnConsultar = view.findViewById(R.id.btnConsultar)
        llResultContainer = view.findViewById(R.id.llResultContainer)
        btnDownloadPdf = view.findViewById(R.id.btnDownloadPdf)

        toggleGroupMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnTabLocalReports -> {
                        llLocalReportsSection.visibility = View.VISIBLE
                        svCloudSearchSection.visibility = View.GONE
                        cargarReportesLocales()
                    }
                    R.id.btnTabCloudSearch -> {
                        llLocalReportsSection.visibility = View.GONE
                        svCloudSearchSection.visibility = View.VISIBLE
                    }
                }
            }
        }

        btnBrowseFile = view.findViewById(R.id.btnBrowseFile)
        btnBrowseFile.setOnClickListener {
            openDocumentLauncher.launch(arrayOf("application/pdf", "application/json"))
        }

        openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { fileUri ->
                val mime = requireContext().contentResolver.getType(fileUri) ?: "application/pdf"
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(fileUri, mime)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(requireContext(), "No hay aplicación para abrir este archivo", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnConsultar.setOnClickListener { consultarCaso() }
        btnDownloadPdf.setOnClickListener { downloadPdf() }

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                downloadPdf()
            } else {
                showErrorDialog(getString(R.string.permiso_de_almacenamiento_denegado))
            }
        }

        cargarReportesLocales()

        return view
    }

    override fun onResume() {
        super.onResume()
        if (llLocalReportsSection.visibility == View.VISIBLE) {
            cargarReportesLocales()
        }
    }

    private fun cargarReportesLocales() {
        val list = mutableListOf<LocalReportFile>()

        val targetDirs = mutableListOf<File>()

        // 1. Subdirectorio oficial de reportes de AutoPeritajeIA
        val valoracionDir = File(requireContext().getExternalFilesDir(null), "ValoracionDeDannos")
        if (valoracionDir.exists()) targetDirs.add(valoracionDir)

        // 2. Directorios raíz de la app
        requireContext().getExternalFilesDir(null)?.let { targetDirs.add(it) }
        requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { targetDirs.add(it) }
        requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.let { targetDirs.add(it) }

        // 3. Almacenamiento público
        targetDirs.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
        targetDirs.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS))

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val visitedPaths = HashSet<String>()

        targetDirs.distinct().forEach { dir ->
            if (dir.exists() && dir.isDirectory) {
                try {
                    dir.walkTopDown().maxDepth(3).forEach { file ->
                        val path = file.absolutePath
                        if (!visitedPaths.contains(path) && file.isFile) {
                            visitedPaths.add(path)
                            val name = file.name
                            if (name.endsWith(".pdf", ignoreCase = true) || name.endsWith(".json", ignoreCase = true)) {
                                val isPdf = name.endsWith(".pdf", ignoreCase = true)
                                val sizeKb = file.length() / 1024
                                val sizeStr = if (sizeKb > 1024) "${String.format(Locale.US, "%.1f", sizeKb / 1024f)} MB" else "$sizeKb KB"
                                val dateStr = sdf.format(Date(file.lastModified()))

                                list.add(LocalReportFile(file, name, sizeStr, dateStr, isPdf))
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ConsultaFragment", "Error al escanear directorio $dir: ${e.message}")
                }
            }
        }

        list.sortByDescending { it.file.lastModified() }

        if (list.isEmpty()) {
            rvLocalReports.visibility = View.GONE
            tvEmptyLocalReports.visibility = View.VISIBLE
        } else {
            rvLocalReports.visibility = View.VISIBLE
            tvEmptyLocalReports.visibility = View.GONE

            rvLocalReports.layoutManager = LinearLayoutManager(requireContext())
            rvLocalReports.adapter = LocalReportAdapter(
                list,
                onOpenClick = { abrirArchivoReporte(it) },
                onShareClick = { compartirArchivoReporte(it) },
                onDeleteClick = { confirmarEliminarReporte(it) }
            )
        }
    }

    private fun abrirArchivoReporte(item: LocalReportFile) {
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            item.file
        )
        val mime = if (item.isPdf) "application/pdf" else "application/json"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "No hay aplicación para abrir este archivo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun compartirArchivoReporte(item: LocalReportFile) {
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            item.file
        )
        val mime = if (item.isPdf) "application/pdf" else "application/json"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        startActivity(Intent.createChooser(intent, "Compartir Reporte de Siniestro"))
    }

    private fun confirmarEliminarReporte(item: LocalReportFile) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Reporte")
            .setMessage("¿Desea borrar permanentemente el archivo ${item.name}?")
            .setPositiveButton("Eliminar") { dialog, _ ->
                if (item.file.delete()) {
                    Toast.makeText(requireContext(), "Archivo eliminado", Toast.LENGTH_SHORT).show()
                    cargarReportesLocales()
                } else {
                    Toast.makeText(requireContext(), "No se pudo eliminar el archivo", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun consultarCaso() {
        llResultContainer.removeAllViews()
        pdfUrl = null

        val caseNumber = etCaseNumber.text.toString()
        val caseToken = etCaseToken.text.toString()

        if (caseNumber.isEmpty() && caseToken.isEmpty()) {
            showErrorDialog(getString(R.string.ingresar_case_number_token))
            return
        }

        if (caseNumber.isEmpty()) {
            showErrorDialog(getString(R.string.debe_ingresar_case_number_token_para_descargar_el_pdf))
            return
        }

        val jsonObject = JSONObject().apply {
            put("case_number", caseNumber)
            put("case_token", caseToken)
        }

        val jsonString = jsonObject.toString()
        val url = "$BASE_URL$API_webhook_query"

        val client = OkHttpClient()
        val mediaType = "application/json".toMediaType()
        val body = jsonString.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val jsonResponse = JSONObject(responseBody ?: "")
                        displayFormattedData(jsonResponse)
                        Log.d("ConsultaFragment", getString(R.string.respuesta_del_servidor, jsonResponse))

                        if (jsonResponse.has("pdf_url")) {
                            pdfUrl = jsonResponse.getString("pdf_url")
                        }

                        if (!pdfUrl.isNullOrEmpty()) {
                            showPdfConfirmationDialog()
                        }
                    } else {
                        val errorMessage = when (response.code) {
                            400 -> getString(R.string.solicitud_incorrecta_verifique_los_datos)
                            404 -> getString(R.string.esperar_10_a_15_minutos)
                            500 -> getString(R.string.error_interno_del_servidor)
                            else -> getString(R.string.error_desconocido, response.code)
                        }
                        showErrorDialog(errorMessage)
                        Log.e("ConsultaFragment", getString(R.string.error_en_la_solicitud, response.code, responseBody))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorTextView = TextView(requireContext())
                    errorTextView.text = getString(R.string.error_message, e.message)
                    llResultContainer.addView(errorTextView)
                    Log.e("ConsultaFragment", getString(R.string.error, e.message))
                }
            }
        }
    }

    private fun displayFormattedData(jsonResponse: JSONObject) {
        val mainTitle = TextView(requireContext()).apply {
            text = getString(R.string.AI_cloud)
            textSize = 10f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 16, 0, 16)
            layoutParams = params
        }
        llResultContainer.addView(mainTitle)

        val data = jsonResponse.getJSONObject("data")
        val caseNumber = data.getString("case_number")
        val vinNumber = data.getString("vin_number")
        val pLaborRate = data.getString("p_labor_rate")
        val laborRate = data.getString("labor_rate")

        val generalInfo = TextView(requireContext()).apply {
            text = getString(R.string.general_info_format, caseNumber, vinNumber, pLaborRate, laborRate)
            setTypeface(null, Typeface.BOLD)
            setBackgroundColor("#AAACAB".toColorInt())
            setTextColor(Color.BLACK)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 16)
            layoutParams = params
        }
        llResultContainer.addView(generalInfo)

        val subTotalPart = data.getString("sub_total_part")
        val subTotalPaint = data.getString("sub_total_paint")
        val subTotalLabor = data.getString("sub_total_labor")
        val subTotal = data.getString("sub_total")
        val tax = data.getString("tax")
        val total = data.getString("total")

        val totalsContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor("#AAACAB".toColorInt())
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 16)
            layoutParams = params
            setPadding(16, 16, 16, 16)
        }

        val totalsTextView = TextView(requireContext()).apply {
            text = getString(
                R.string.totals_format,
                subTotalPart,
                subTotalPaint,
                subTotalLabor,
                subTotal,
                tax,
                total
            )
            setTextColor(Color.BLACK)
            setTypeface(null, Typeface.BOLD)
        }
        totalsContainer.addView(totalsTextView)
        llResultContainer.addView(totalsContainer)

        val details = data.getJSONArray("details")
        for (i in 0 until details.length()) {
            val detail = details.getJSONObject(i)

            val detailContainer = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor("#d0d2d1".toColorInt())
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 8)
                layoutParams = params
                setPadding(16, 16, 16, 16)
            }

            val detailTitle = TextView(requireContext()).apply {
                text = getString(R.string.partes_y_piezas_nombre, detail.getString("car_part"))
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.BLACK)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 8)
                layoutParams = params
            }
            detailContainer.addView(detailTitle)

            val detailInfo = TextView(requireContext()).apply {
                val formattedText = getString(
                    R.string.detail_info_format,
                    detail.getString("side_1"),
                    detail.getString("side_2"),
                    detail.getString("damage"),
                    detail.getString("treatment"),
                    detail.getString("part_cost"),
                    detail.getString("paint_hour"),
                    detail.getString("paint_material_cost"),
                    detail.getString("labour_hour"),
                    detail.getString("labour_cost")
                )
                text = HtmlCompat.fromHtml(formattedText, HtmlCompat.FROM_HTML_MODE_LEGACY)
                setTextColor(Color.BLACK)
            }
            detailContainer.addView(detailInfo)

            llResultContainer.addView(detailContainer)
        }
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.error_texto))
            .setMessage(message)
            .setPositiveButton((R.string.ok_texto)) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun downloadPdf() {
        val caseNumber = etCaseNumber.text.toString()
        val caseToken = etCaseToken.text.toString()

        if (caseNumber.isEmpty()) {
            showErrorDialog(getString(R.string.debe_ingresar_case_number_token_para_descargar_el_pdf))
            return
        }

        if (!checkPermissions()) {
            requestPermissions()
            return
        }

        val jsonObject = JSONObject().apply {
            put("case_number", caseNumber)
            put("case_token", caseToken)
        }

        val jsonString = jsonObject.toString()
        val url = "$BASE_URL$API_generate_pdf"

        val client = OkHttpClient()
        val mediaType = "application/json".toMediaType()
        val body = jsonString.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("DownloadPDF", getString(R.string.iniciando_descarga))
                Log.d("DownloadPDF", getString(R.string.url, url))
                Log.d("DownloadPDF", getString(R.string.cuerpo_de_la_solicitud, jsonString))

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    Log.d("DownloadPDF", getString(R.string.respuesta_exitosa, response.code))

                    val responseBody = response.body
                    if (responseBody == null) {
                        withContext(Dispatchers.Main) {
                            showErrorDialog(getString(R.string.la_respuesta_del_servidor_esta_vacia))
                        }
                        return@launch
                    }

                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val currentDate = SimpleDateFormat("ddMMyyyy", Locale.getDefault()).format(Date())
                    val newFilename = "Caso-$caseNumber-$currentDate.pdf"
                    val file = File(downloadsDir, newFilename)

                    try {
                        if (file.exists()) {
                            file.delete()
                        }

                        withContext(Dispatchers.IO) {
                            file.outputStream().use { output ->
                                responseBody.byteStream().use { input ->
                                    input.copyTo(output)
                                }
                            }
                        }

                        Log.d("DownloadPDF", getString(R.string.pdf_guardado_en, file.absolutePath))

                        withContext(Dispatchers.Main) {
                            val uri = FileProvider.getUriForFile(
                                requireContext(),
                                "${requireContext().packageName}.fileprovider",
                                file
                            )
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }
                            try {
                                startActivity(intent)
                            } catch (_: ActivityNotFoundException) {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.no_hay_aplicación_para_abrir_pdf),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: IOException) {
                        withContext(Dispatchers.Main) {
                            showErrorDialog(getString(R.string.error_al_guardar_el_archivo, e.message))
                        }
                    } catch (e: SecurityException) {
                        withContext(Dispatchers.Main) {
                            showErrorDialog(getString(R.string.error_de_seguridad_al_acceder_al_archivo, e.message))
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            showErrorDialog("Error desconocido: ${e.message ?: getString(R.string.sin_informacion)}")
                            e.printStackTrace()
                        }
                    }
                } else {
                    val responseBody = response.body?.string()
                    Log.e("DownloadPDF", getString(R.string.error_en_la_solicitud, response.code, responseBody))
                    withContext(Dispatchers.Main) {
                        val errorMessage = when (response.code) {
                            400 -> getString(R.string.solicitud_incorrecta_verifique_los_datos)
                            404 -> getString(R.string.no_se_encontr_informacion_para_los_parametros_proporcionados)
                            500 -> getString(R.string.error_interno_del_servidor)
                            else -> getString(R.string.error_desconocido, response.code)
                        }
                        showErrorDialog(errorMessage)
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    showErrorDialog(getString(R.string.error_de_red, e.message))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showErrorDialog("Error desconocido: ${e.message ?: getString(R.string.sin_informacion)}")
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showPdfConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.ver_pdf))
            .setMessage(getString(R.string.desea_ver_el_pdf))
            .setIcon(R.drawable.analyze_list_logs_search_icon)
            .setPositiveButton(getString(R.string.si)) { dialog, _ ->
                openPdfFromUrl(pdfUrl)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun openPdfFromUrl(pdfUrl: String?) {
        if (pdfUrl.isNullOrEmpty()) {
            showErrorDialog(getString(R.string.no_se_encontro_url_pdf))
            return
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = pdfUrl.toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            showErrorDialog(getString(R.string.no_hay_aplicacion_para_abrir_pdf))
        }
    }

    private fun checkPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                requestCodePermissions
            )
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodePermissions) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                downloadPdf()
            } else {
                showErrorDialog(getString(R.string.permiso_de_almacenamiento_denegado))
            }
        }
    }
}
