package com.ehome.enpartesapp.ui.home

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ehome.enpartesapp.R
import com.ehome.enpartesapp.databinding.FragmentConsultasabiertasBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

//private const val BASE_URL = "http://192.168.1.127/" // ip URL desde eHome ethernet
private const val BASE_URL = "http://192.168.1.143/" // ip URL desde eHome 5.0 wifi
//private const val BASE_URL = "http://192.168.0.100/" // ip URL desde olax ethernet

private const val PATH = "integracion"
private const val ACTIONFINDVEHICLE = "FINDVEHICLE"
private const val ACCESS_CODE = "123456"
private const val C_KEY = "12345"

data class VehicleData(
    val datos: VehicleInfo?,
    val carac: List<VehicleCarac>?
)

data class VehicleInfo(
    val vehicleId: String?,
    val brandId: Int?,
    val brandName: String?,
    val modelId: Int?,
    val modelName: String?,
    val licensePlate: String?,
    val vin: String?,
    val serialNumber: String?,
    val year: String?
)

data class VehicleCarac(
    val vehicleId: String?,
    val carac: String?
)

// API interface
interface ApiService {
    //@GET("/integracion")
    @GET("/$PATH")
    suspend fun findVehicle(
        @Query("q") query: String
    ): Response<Map<String, VehicleData>> // Changed return type to Map<String, VehicleData>
}

private fun buildVehiculoQuery(licensePlate: String, serialNumber: String): String {
    val jsonQuery = """
        {
            "action": "$ACTIONFINDVEHICLE",
            "accessCode": "$ACCESS_CODE",
            "cKey": "$C_KEY",
            "licensePlate": "$licensePlate",
            "serialNumber": "$serialNumber"
        }
    """.trimIndent()

    return jsonQuery
}

// Retrofit client
object RetrofitClient {

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }
}

class ConsultasAbiertasFragment : Fragment() {

    private var _binding: FragmentConsultasabiertasBinding? = null
    private val binding get() = _binding!!

    private lateinit var licensePlateInputLayout: TextInputLayout
    private lateinit var licensePlateEditText: TextInputEditText
    private lateinit var searchButton: Button
    private lateinit var resultTextView: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var vehicleAdapter: VehicleAdapter

    // Variables to hold the state
    private var currentLicensePlate: String? = null
    private var currentResults: String? = null

    private var savedQuery: String? = null //Variable para guardar la consulta

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConsultasabiertasBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize views
        licensePlateInputLayout = binding.licensePlateInputLayout
        licensePlateEditText = binding.licensePlateEditText
        searchButton = binding.searchButton
        resultTextView = binding.resultTextView
        recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Set up the search button click listener
        searchButton.setOnClickListener {
            searchVehicle()
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Restaurar la consulta y ejecutar la búsqueda
        if (savedQuery != null) {
            licensePlateEditText.setText(savedQuery)
            searchButton.performClick() // Simular el clic del botón al regresar de los reclamos
        }

        licensePlateEditText.setText(currentLicensePlate)
        resultTextView.text = currentResults
        resultTextView.visibility = if (currentResults.isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the state of your views here
        outState.putString("licensePlate", licensePlateEditText.text.toString())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun searchVehicle() {
        val licensePlate = licensePlateEditText.text.toString().trim()
        currentLicensePlate = licensePlate

        // Validate input (one or the other, not both)
        if (licensePlate.isEmpty()) {
            licensePlateInputLayout.error = getString(R.string.ingrese_la_placa_o_el_serial)
            return
        } else {
            licensePlateInputLayout.error = null
        }

        // Prepare the query
        //val query = "{\"action\":\"$ACTIONFINDVEHICLE\",\"accessCode\":\"$ACCESS_CODE\",\"cKey\":\"$C_KEY\",\"licensePlate\":\"$licensePlate\",\"serialNumber\":\"$licensePlate\"}"
        val query = buildVehiculoQuery(licensePlate, licensePlate)

        // Guardar la consulta
        savedQuery = licensePlate // Guardar solo el texto ingresado

        // Make the API request
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.findVehicle(query)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val vehicleResponse = response.body()
                        if (vehicleResponse != null) {
                            displayVehicleData(vehicleResponse)
                        } else {
                            resultTextView.text = getString(R.string.no_se_encontraron_resultados)
                            resultTextView.visibility = View.VISIBLE
                            currentResults = getString(R.string.no_se_encontraron_resultados)
                        }
                    } else {
                        resultTextView.text = getString(R.string.error_en_la_solicitud, response.code(), "")
                        resultTextView.visibility = View.VISIBLE
                        currentResults = getString(R.string.error_en_la_solicitud, response.code(), "")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    resultTextView.text = getString(R.string.error, e.message)
                    resultTextView.visibility = View.VISIBLE
                    currentResults = getString(R.string.error, e.message)
                }
            }
        }
    }

    private fun displayVehicleData(vehicleResponse: Map<String, VehicleData>) {
        resultTextView.visibility = View.GONE
        // Define the list of characteristics you want to display
        //val caracteristicasMostrar = listOf("Clase", "cylinder", "Categoría", "liters", "Carrocería", "transmission/Transmisión", "Transmisión/Tracción", "Tipo de Carrocería")
        val caracteristicasNombres = listOf("Clase", "Cilindro", "Categoría", "Litros", "Carrocería", "Caja/Transmisión", "Transmisión/Tracción", "Tipo de Carrocería")

        val vehicleList = mutableListOf<VehicleData>()
        // Iterate through the Map using entries
        if (vehicleResponse.isNotEmpty()) {
            for (entry in vehicleResponse.entries) {
                val vehicleData = entry.value
                vehicleList.add(vehicleData)
            }
            vehicleAdapter = VehicleAdapter(vehicleList, caracteristicasNombres)
            recyclerView.adapter = vehicleAdapter
        } else {
            showDialog(getString(R.string.no_se_encontraron_resultados))
        }
    }

    private fun showDialog(message: String) {
        AlertDialog.Builder(requireContext()) // Usar requireContext() directamente
            .setTitle(getString(R.string.informacion)) // Título del diálogo
            .setMessage(message)     // Mensaje a mostrar
            .setIcon(R.drawable.analyze_list_logs_search_icon) // Ícono personalizado
            .setPositiveButton(getString(R.string.ok_texto)) { dialog, _ ->
                dialog.dismiss()     // Cerrar el diálogo al hacer clic en "OK"
            }
            .show() // Mostrar el diálogo
    }

}

