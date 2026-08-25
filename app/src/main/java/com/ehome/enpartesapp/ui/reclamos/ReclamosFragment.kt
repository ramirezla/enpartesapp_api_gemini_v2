package com.ehome.enpartesapp.ui.reclamos

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ehome.enpartesapp.R
import com.ehome.enpartesapp.databinding.FragmentReclamosBinding
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

//private const val BASE_URL = "http://192.168.0.100/" // ip URL desde olax ethernet
private const val BASE_URL = "http://192.168.1.143/"  // ip desde eHome wifi
//private const val BASE_URL = "http://192.168.1.127/" // ip URL desde eHome ethernet
private const val PATH = "integracion"
private const val ACTIONGETVEHICLECLAIMS = "GETVEHICLECLAIMS"
private const val ACTIONGETPARTSCLAIM = "GETPARTSCLAIM"
private const val ACCESS_CODE = "123456"
private const val C_KEY = "12345"

class ReclamosFragment : Fragment() {

    private var _binding: FragmentReclamosBinding? = null
    private val binding get() = _binding!!
    private lateinit var claimAdapter: ClaimAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReclamosBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Ejemplo
    // "http://192.168.1.143/integracion?q={\"action\":\"GETVEHICLECLAIMS\",\"accessCode\":\"123456\",\"cKey\":\"12345\",\"vehicleId\":\"$vehicleId\"}"
    private fun buildVehiculoUrl(vehicleId: String): String {
        val jsonQuery = """
        {
            "action": "$ACTIONGETVEHICLECLAIMS",
            "accessCode": "$ACCESS_CODE",
            "cKey": "$C_KEY",
            "vehicleId": "$vehicleId"
        }
    """.trimIndent()

        val uri = Uri.parse(BASE_URL).buildUpon()
            .appendPath(PATH)
            .appendQueryParameter("q", jsonQuery)
            .build()

        return uri.toString()
    }

    // Ejemplo
    //"http://192.168.1.143/integracion?q={\"action\":\"GETPARTSCLAIM\",\"accessCode\":\"123456\",\"cKey\":\"12345\",\"claimId\":\"${claim.claimId}\"}"
    private fun buildPartesUrl(claimId: Int): String {
        val jsonQuery = """
        {
            "action": "$ACTIONGETPARTSCLAIM",
            "accessCode": "$ACCESS_CODE",
            "cKey": "$C_KEY",
            "claimId": "$claimId"
        }
    """.trimIndent()

        val uri = Uri.parse(BASE_URL).buildUpon()
            .appendPath(PATH)
            .appendQueryParameter("q", jsonQuery)
            .build()

        return uri.toString()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Now that binding is inflated, we can use it to access views
        recyclerView = binding.recyclerViewClaims
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val bundle = arguments
        if (bundle != null) {
            val brandName = bundle.getString("brandName")
            val modelName = bundle.getString("modelName")
            val year = bundle.getString("year")
            val vehicleId = bundle.getString("vehicleId")
            val licensePlate = bundle.getString("licensePlate")
            val serialNumber = bundle.getString("serialNumber")
            val vin = bundle.getString("vin")
            val characteristicsLine = bundle.getString("characteristicsLine")

            val receivedDataString = StringBuilder()
                .append("Brand: $brandName\n")
                .append("Model: $modelName\n")
                .append("Year: $year\n")
                .append("Vehicle ID: $vehicleId\n")
                .append("License Plate: $licensePlate\n")
                .append("Serial Number: $serialNumber\n")
                .append("Vin: $vin\n")
                .append("Characteristics: $characteristicsLine\n")
                .toString()

            // Use binding to access the TextView
            binding.receivedDataVehicle.text = receivedDataString

            if (vehicleId != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val vehiculoUrl = buildVehiculoUrl(vehicleId)
                        val response = makeApiRequest(vehiculoUrl)

                        withContext(Dispatchers.Main) {
                            if (response.isEmpty()) {
                                showToast(getString(R.string.respuesta_vacia_del_servidor))
                                return@withContext
                            }

                            val claimsList: List<Claim> = try {
                                Gson().fromJson(
                                    response,
                                    object : TypeToken<List<Claim>>() {}.type
                                )
                            } catch (e: JsonSyntaxException) {
                                showToast(getString(R.string.error_al_procesar_la_respuesta_del_servidor))
                                Log.e("ReclamosFragment", "Error parsing JSON: ${e.message}")
                                return@withContext
                            }

                            if (claimsList.isNotEmpty()) {
                                claimAdapter = ClaimAdapter(claimsList) { claim ->
                                    if (claim.claimId != 0) {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            val partesUrl = buildPartesUrl(claim.claimId)
                                            val partsResponse = makeApiRequest(partesUrl)

                                            withContext(Dispatchers.Main) {
                                                val partsList: List<Part> =
                                                    Gson().fromJson(
                                                        partsResponse,
                                                        object : TypeToken<List<Part>>() {}.type
                                                    )
                                                showPartsPopup(partsList)
                                            }
                                        }
                                    } else {
                                        Toast.makeText(
                                            requireContext(),
                                            getString(R.string.id_del_reclamo_es_cero),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                recyclerView.adapter = claimAdapter
                            } else {
                                addTextViewToContainer(getString(R.string.no_se_encontraron_reclamos))
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(
                            "ReclamosFragment",
                            getString(R.string.error_en_la_solicitud_del_api, e.message)
                        )
                        withContext(Dispatchers.Main) {
                            addTextViewToContainer(getString(R.string.error_cargando_la_informacion_de_los_reclamos))
                        }
                    }
                }
            }
        }
    }

    // Function to show parts data in a popup
    private fun showPartsPopup(partsList: List<Part>) {
        val builder = AlertDialog.Builder(requireContext())

        val titleTextView = TextView(requireContext()).apply {
            text = getString(R.string.partes_y_piezas)
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 16)
        }
        builder.setCustomTitle(titleTextView)

        val recyclerViewParts = RecyclerView(requireContext())
        recyclerViewParts.layoutManager = LinearLayoutManager(requireContext())
        val partAdapter = PartAdapter(partsList)
        recyclerViewParts.adapter = partAdapter

        builder.setView(recyclerViewParts)
        builder.setPositiveButton(getString(R.string.ok_texto)) { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    // Function to make the API request
    private fun makeApiRequest(url: String): String {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected code $response")
            }
            return response.body!!.string()
        }
    }

    private fun addTextViewToContainer(text: String) {
        val textView = TextView(requireContext())
        textView.text = text
        //resultContainer.addView(textView)
    }

    // Data class for Part (adjust fields as needed)
    data class Part(
        val className: String, // Renamed from __className
        val description: String,
        val code: String,
        val additionalinformation: String,
        val quality: String,
        val codstatus: String,
        val status: String,
        val ammount: String,
        val tax: String,
        val disponibility: Int,
        val condition: String
    )

    // Data class for Claim (adjust fields as needed)
    data class Claim(
        val className: String, // Renamed from __className
        val claimId: Int,
        val policyNumber: String,
        val claimNumber: String,
        val certificateNumber: String,
        val snumerosolicitud: String,
        val claimDate: String,
        val sumInsured: String,val splaca: String,
        val scarroceria: String,
        val cliente: String
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}