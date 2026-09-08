/**
 * Archivo: PresupuestoFragment.kt
 * Propósito: Gestiona la interfaz de usuario y la lógica para generar informes de presupuesto y daños de vehículos mediante IA.
 *
 * Este fragmento permite a los usuarios ingresar información del vehículo, capturar o subir fotos de daños,
 * y generar un informe detallado utilizando el modelo de IA Google Gemini.
 *
 * Información Crítica (Muy Relevante para la IA), Estos datos afectan directamente la precisión de la estimación de costos y la identificación de piezas:
 * 1.- Marca y Modelo: Es fundamental. Los costos de las piezas y la complejidad del desarme varían drásticamente entre un "Toyota Corolla" y un "Acura MDX".
 * 2.- Año del Vehículo: Crucial para la compatibilidad de piezas. Un modelo 2010 y uno 2024 del mismo vehículo pueden tener componentes estructurales y tecnológicos (como sensores) totalmente diferentes.
 * 3.- Imágenes (Fotos): Es el dato más importante. Sin las fotos, la IA no tiene base para "ver" el daño.
 * 4.- Ubicación (País/Ciudad): Muy relevante para que la IA ajuste los precios de los repuestos y la mano de obra al mercado local (ej. precios en Ecuador vs. Chile).
 * 5.- Costo de Mano de Obra: Esencial para que el cálculo matemático del presupuesto sea correcto.
 * 6.- Color: Relevante para el cálculo de los materiales de pintura, ya que algunos colores (como perlados o tricapas) son más costosos de aplicar y preparar.

 * 1. Nueva Sección Visual: He añadido un componente CardView en la pantalla de reporte titulado "--- Fotos Analizadas ---".
 * 2. Carrusel de Imágenes: Las fotos se muestran en un carrusel horizontal justo antes de los botones de guardado. Esto permite ver todas las imágenes capturadas sin ocupar demasiado espacio vertical.
 * 3. Carga Dinámica: El ReportDisplayFragment ahora recibe las rutas de las fotos desde el formulario inicial y las renderiza automáticamente al cargar el informe.
 * 4. Internacionalización: He añadido el recurso de texto necesario en   strings.xml para mantener las buenas prácticas del proyecto.

Nuevo Mapa de Costos (laborCostByCountry): He definido valores estimados en USD por hora para cada país basándome en promedios de mercado para talleres independientes:
 * Estados Unidos: $120.0
 * Argentina: $60.0 (ajustado por volatilidad)
 * Brasil / Uruguay: $35.0
 * Chile / Panamá: $30.0
 * Colombia: $28.0
 * México / Costa Rica: $25.0
 * Ecuador / Perú / Venezuela: $20.0
 * Bolivia / Paraguay: $15.0 - $18.0
 * Lógica Dinámica en el Reporte: En el método generateDamageReport(), ahora la variable costoHoraManoObra ya no es fija. El sistema detecta qué país seleccionó el usuario en el formulario y extrae el valor correspondiente del mapa. Si el país no está en la lista, utiliza el valor base de $20.0 por seguridad.
 */

package com.ehome.enpartesapp.ui.presupuesto

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.util.Calendar
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ehome.enpartesapp.R
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.ehome.enpartesapp.BuildConfig

/**
 * Clase de datos que representa un ítem de foto en el informe de presupuesto.
 *
 * @property imagenUri El URI de la imagen capturada o subida.
 * @property tipoFoto El tipo o categoría de la foto (ej. frontal, lateral, VIN).
 * @property isFotoTomada Bandera booleana que indica si la foto fue tomada con la cámara o subida desde la galería.
 */
data class FotoItem(
    var imagenUri: Uri? = null,
    var tipoFoto: String = "",
    var isFotoTomada: Boolean = false
)

/**
 * Adaptador para el RecyclerView que muestra y gestiona las fotos del vehículo.
 *
 * @property context El contexto de la aplicación.
 * @property fotoList La lista de [FotoItem] a mostrar.
 * @property onAddClickListener Callback para cuando se hace clic en el botón "Agregar".
 * @property onDeleteClickListener Callback para cuando se hace clic en el botón "Borrar" para una posición específica.
 * @property onTakePhotoClickListener Callback para cuando se hace clic en el botón "Tomar Foto" para una posición específica.
 * @property onUploadPhotoClickListener Callback para cuando se hace clic en el botón "Subir Foto" para una posición específica.
 */
class FotoAdapter(
    private val context: Context,
    private val fotoList: MutableList<FotoItem>,
    private val onAddClickListener: () -> Unit,
    private val onDeleteClickListener: (Int) -> Unit,
    private val onTakePhotoClickListener: (Int) -> Unit,
    private val onUploadPhotoClickListener: (Int) -> Unit
) : RecyclerView.Adapter<FotoAdapter.FotoViewHolder>() {

    var currentPosition: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_foto, parent, false)
        return FotoViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        val fotoItem = fotoList[position]

        val tiposFoto = context.resources.getStringArray(R.array.tipos_foto)
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, tiposFoto)
        holder.spinnerTipoFoto.adapter = adapter

        val selectedIndex = tiposFoto.indexOf(fotoItem.tipoFoto)
        if (selectedIndex >= 0) {
            holder.spinnerTipoFoto.setSelection(selectedIndex)
        }

        holder.spinnerTipoFoto.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                fotoItem.tipoFoto = tiposFoto[pos]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        holder.btnTomarFoto.setOnClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                currentPosition = adapterPosition
                onTakePhotoClickListener(adapterPosition)
            }
        }

        holder.btnSubirFoto.setOnClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                currentPosition = adapterPosition
                onUploadPhotoClickListener(adapterPosition)
            }
        }

        if (fotoItem.imagenUri != null) {
            holder.imgFoto.setImageURI(fotoItem.imagenUri)
        } else {
            holder.imgFoto.setImageResource(R.drawable.ic_menu_gallery)
        }

        holder.btnBorrar.setOnClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                onDeleteClickListener(adapterPosition)
            }
        }

        holder.btnAgregar.setOnClickListener {
            onAddClickListener()
        }
    }

    override fun getItemCount(): Int = fotoList.size

    class FotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val btnTomarFoto: ImageButton = itemView.findViewById(R.id.btnTomarFoto)
        val btnSubirFoto: ImageButton = itemView.findViewById(R.id.btnSubirFoto)
        val imgFoto: ImageView = itemView.findViewById(R.id.imgFoto)
        val spinnerTipoFoto: Spinner = itemView.findViewById(R.id.spinnerTipoFoto)
        val btnBorrar: ImageButton = itemView.findViewById(R.id.btnBorrar)
        val btnAgregar: ImageButton = itemView.findViewById(R.id.btnAgregar)
    }
}

/**
 * Fragmento para crear un informe de presupuesto/daños de vehículo.
 *
 * Este fragmento recopila datos del vehículo (marca, modelo, año, etc.) y fotos de los daños.
 * Utiliza el modelo de IA Google Gemini para generar un informe de daños detallado
 * y costos estimados basados en la información e imágenes proporcionadas.
 */
class PresupuestoFragment : Fragment() {

    private var fotoList: MutableList<FotoItem> = mutableListOf()

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FotoAdapter
    private var currentPhotoUri: Uri? = null

    private lateinit var spinnerTipoFotoVin: Spinner
    private lateinit var spinnerTipoVehiculo: Spinner
    private lateinit var spinnerMarcaVehiculo: Spinner
    private lateinit var spinnerModeloVehiculo: Spinner
    private lateinit var spinnerVehicleColor: Spinner
    private lateinit var spinnerCountry: Spinner
    private lateinit var spinnerState: Spinner
    private lateinit var spinnerCity: Spinner

    private lateinit var progressBar: ProgressBar // <-- AÑADIR ESTA LÍNEA

    private lateinit var takePhotoLauncher: ActivityResultLauncher<Uri>
    private lateinit var uploadPhotoLauncher: ActivityResultLauncher<String>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    private lateinit var etCaseNumber: TextInputEditText
    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etVINnumber: TextInputEditText
    private lateinit var etDateOfInspection: TextInputEditText
    private lateinit var etVehicleYear: TextInputEditText

    private lateinit var btnCancelar: Button
    private lateinit var btnAceptar: Button

    private lateinit var geminiModel: GenerativeModel

    private val modelosPorMarca: Map<String, Array<String>> = mapOf(
        "Seleccione una marca..." to arrayOf("Seleccione un modelo..."),
        "Acura" to arrayOf("ILX","MDX","RDX","RL","RLX","RSX","TL","TSX"),
        "BAIC" to arrayOf("Seleccione un modelo...", "X35", "X55", "BJ40"),
        "Chevrolet" to arrayOf("Seleccione un modelo...", "Astro", "Avalanche", "Aveo/Kalos/Sonic","Bolt","C/K Pickup",
            "Camaro","Caprice","Captiva","Cavalier","Cobalt","Colorado","Corvette","Cruze","D-Max","Epica/Tosca",
            "Equinox","Express","HHR","Impala","Joy","Lanus","Lumina","Malibu","Monte Carlo","Nexia/Cielo/Racer","N300",
            "N400","Optra/Lacetti/Nubira","Onix","Orlando","Prizm","S-10 Blazer/Jimmy","S10 Pickup/Sonoma","SS","SSR","Sail",
            "Silverado","Spark/Matiz/Beato","Suburban","Tahoe","TrailBlazer","Tracker","Traverse",
            "Uplander","Venture","Volt"),
        "Chery" to arrayOf("Seleccione un modelo...", "A1","A11","A13","A15","A3/M11","A5","Eastar/B11","Exceed/Exceed TX",
            "IndiS","Karry Youya","QQ3/S11","QQ6/S21","Tiggo","Tiggo 2 Pro","Tiggo 3/3x/2 (A13T)", "Tiggo 4 Pro","Tiggo 5 (T21)",
            "Tiggo 7 Pro", "Tiggo 8 Pro", "Tiggo 8 Pro Max", "Arrizo 5","V5/B14"),
        "Ford" to arrayOf("Seleccione un modelo...", "Bronco","Bronco Sport","C-Max","Cargo","Crown Victoria"
            ,"E-Series/Econoline","Ecosport","Edge","Escape","Escort","Everest","Excursion","Expedition","Explorer",
            "F-Series","F-150","Falcon","Fiesta","Figo/Ka","Five Hundred","Flex","Focus","Freestart","Fusion",
            "Galaxy","Grand C-Max","Grand Tourneo Connect","Kuga","Laser","Mondeo","Mustang","Orion","Probe","Puma",
            "Ranger","S-Series","S-Max","Taurus","Taurus X/Freestyle","Tempo/Topaz","Territory (SY)","Thunderbird","Transit",
            "Transit Connect","Transit Courier/Turneo","Transit Courier/Turneo Custom","Windstar","ZX2/Escort ZX2"),
        "Great Wall" to arrayOf("Seleccione un modelo...", "Deer","Poer","Voleex C30","Wingle 5", "Wingle 7"),
        "Haval" to arrayOf("Seleccione un modelo...", "Dargo","H6","Haval F7","Haval H2","Haval H3/Hover H3","Haval H5/Hover H5",
            "Haval H6","Haval H8","Haval H9","Haval M4","H9","Jolion"),
        "Hyundai" to arrayOf("Seleccione un modelo...", "Tucson", "Creta", "Accent", "Grand i10", "Santa Fe", "Palisade", "Venue", "Staria"),
        "Jetour" to arrayOf("Seleccione un modelo...", "Dashing","X70","X70 Plus"),
        "Kia" to arrayOf("Seleccione un modelo...", "Sportage", "Picanto", "Rio", "Soluto", "Seltos", "Sonet", "Stonic", "Carnival", "K2500/K2700"),
        "Mazda" to arrayOf("Seleccione un modelo...", "BT-50", "CX-5", "CX-30", "Mazda2", "Mazda3", "CX-9"),
        "Nissan" to arrayOf("Seleccione un modelo...", "Frontier", "Kicks", "Versa", "X-Trail", "Pathfinder", "Qashqai", "Murano"),
        "Renault" to arrayOf("Seleccione un modelo...", "Duster", "Logan", "Sandero", "Stepway", "Kwid", "Oroch", "Captur"),
        "Suzuki" to arrayOf("Seleccione un modelo...", "Grand Vitara", "Jimny", "Swift", "S-Preso", "Baleno", "Vitara (nuevo)", "S-Cross"),
        "Toyota" to arrayOf("Seleccione un modelo...", "Hilux", "Fortuner", "RAV4", "Corolla", "Yaris", "Prado", "Land Cruiser", "Rush", "Agya", "Stout"),
        "Volkswagen" to arrayOf("Seleccione un modelo...", "Amarok", "T-Cross", "Nivus", "Virtus", "Polo", "Tiguan", "Saveiro", "Taos"),
        "Otra" to arrayOf("Seleccione un modelo...", "Otro Modelo")
    )

    private val colors = arrayOf(
        "Seleccione un color...",
        "Amarillo", "Azul", "Azul Cielo", "Azul Eléctrico", "Azul Marino", "Azul Metálico", "Azul Noche", "Azul Petróleo", "Azul Rey",
        "Beige", "Blanco", "Blanco Hueso", "Blanco Perlado", "Bronce",
        "Café / Marrón", "Champán", "Cobre", "Crema", "Dorado",
        "Gris", "Gris Acero", "Gris Cemento", "Gris Grafito", "Gris Oscuro", "Gris Plata",
        "Morado / Púrpura", "Naranja", "Naranja Cobrizo",
        "Negro", "Negro Brillante", "Negro Mate", "Plateado",
        "Rojo", "Rojo Brillante", "Rojo Cereza", "Rojo Vino", "Rosado",
        "Turquesa", "Verde", "Verde Esmeralda", "Verde Lima", "Verde Menta", "Verde Oliva", "Verde Oscuro",
        "Otro"
    )

    private val countries = arrayOf(
        "Seleccione un país...",
        "Argentina", "Bolivia", "Brasil", "Chile", "Colombia", "Costa Rica", "Ecuador", 
        "Estados Unidos", "México", "Panamá", "Paraguay", "Perú", "Uruguay", "Venezuela"
    )

    private val statesByCountry: Map<String, Array<String>> = mapOf(
        "Seleccione un país..." to arrayOf("Seleccione un estado..."),
        "Argentina" to arrayOf("Seleccione un estado...", "Buenos Aires", "Córdoba", "Mendoza", "Santa Fe", "Tucumán"),
        "Bolivia" to arrayOf("Seleccione un estado...", "Cochabamba", "La Paz", "Santa Cruz"),
        "Brasil" to arrayOf("Seleccione un estado...", "Bahia", "Minas Gerais", "Paraná", "Rio de Janeiro", "São Paulo"),
        "Chile" to arrayOf("Seleccione un estado...", "Antofagasta", "Biobío", "Región Metropolitana", "Valparaíso"),
        "Colombia" to arrayOf("Seleccione un estado...", "Amazonas", "Antioquia", "Arauca", "Atlántico", "Bolívar", "Boyacá", "Caldas", "Caquetá", "Casanare", "Cauca", "Cesar", "Chocó", "Córdoba", "Cundinamarca", "Guainía", "Guaviare", "Huila", "La Guajira", "Magdalena", "Meta", "Nariño", "Norte de Santander", "Putumayo", "Quindío", "Risaralda", "San Andrés y Providencia", "Santander", "Sucre", "Tolima", "Valle del Cauca", "Vaupés", "Vichada"),
        "Costa Rica" to arrayOf("Seleccione un estado...", "Alajuela", "Cartago", "Guanacaste", "Heredia", "Limón", "Puntarenas", "San José"),
        "Ecuador" to arrayOf(
            "Seleccione un estado...",
            "Azuay", "Bolívar", "Cañar", "Carchi", "Chimborazo", "Cotopaxi", "El Oro", "Esmeraldas",
            "Galápagos", "Guayas", "Imbabura", "Loja", "Los Ríos", "Manabí", "Morona Santiago",
            "Napo", "Orellana", "Pastaza", "Pichincha", "Santa Elena", "Santo Domingo de los Tsáchilas",
            "Sucumbíos", "Tungurahua", "Zamora Chinchipe"
        ),
        "Estados Unidos" to arrayOf("Seleccione un estado...", "Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado", "Connecticut", "Delaware", "Florida", "Georgia", "Hawaii", "Idaho", "Illinois", "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana", "Maine", "Maryland", "Massachusetts", "Michigan", "Minnesota", "Mississippi", "Missouri", "Montana", "Nebraska", "Nevada", "New Hampshire", "New Jersey", "New Mexico", "New York", "North Carolina", "North Dakota", "Ohio", "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island", "South Carolina", "South Dakota", "Tennessee", "Texas", "Utah", "Vermont", "Virginia", "Washington", "West Virginia", "Wisconsin", "Wyoming"),
        "México" to arrayOf("Seleccione un estado...", "Aguascalientes", "Baja California", "Baja California Sur", "Campeche", "Chiapas", "Chihuahua", "Ciudad de México", "Coahuila", "Colima", "Durango", "Estado de México", "Guanajuato", "Guerrero", "Hidalgo", "Jalisco", "Michoacán", "Morelos", "Nayarit", "Nuevo León", "Oaxaca", "Puebla", "Querétaro", "Quintana Roo", "San Luis Potosí", "Sinaloa", "Sonora", "Tabasco", "Tamaulipas", "Tlaxcala", "Veracruz", "Yucatán", "Zacatecas"),
        "Panamá" to arrayOf("Seleccione un estado...", "Bocas del Toro", "Chiriquí", "Coclé", "Colón", "Darién", "Herrera", "Los Santos", "Panamá", "Panamá Oeste", "Veraguas"),
        "Paraguay" to arrayOf("Seleccione un estado...", "Alto Paraguay", "Alto Paraná", "Amambay", "Asunción", "Boquerón", "Caaguazú", "Caazapá", "Canindeyú", "Central", "Concepción", "Cordillera", "Guairá", "Itapúa", "Misiones", "Ñeembucú", "Paraguarí", "Presidente Hayes", "San Pedro"),
        "Perú" to arrayOf("Seleccione un estado...", "Amazonas", "Ancash", "Apurímac", "Arequipa", "Ayacucho", "Cajamarca", "Callao", "Cusco", "Huancavelica", "Huánuco", "Ica", "Junín", "La Libertad", "Lambayeque", "Lima", "Loreto", "Madre de Dios", "Moquegua", "Pasco", "Piura", "Puno", "San Martín", "Tacna", "Tumbes", "Ucayali"),
        "Uruguay" to arrayOf("Seleccione un estado...", "Artigas", "Canelones", "Cerro Largo", "Colonia", "Durazno", "Flores", "Florida", "Lavalleja", "Maldonado", "Montevideo", "Paysandú", "Río Negro", "Rivera", "Rocha", "Salto", "San José", "Soriano", "Tacuarembó", "Treinta y Tres"),
        "Venezuela" to arrayOf("Seleccione un estado...", "Amazonas", "Anzoátegui", "Apure", "Aragua", "Barinas", "Bolívar", "Carabobo", "Cojedes", "Delta Amacuro", "Distrito Capital", "Falcón", "Guárico", "Lara", "Mérida", "Miranda", "Monagas", "Nueva Esparta", "Portuguesa", "Sucre", "Táchira", "Trujillo", "Vargas (La Guaira)", "Yaracuy", "Zulia")
    )
    private val citiesByState: Map<String, Array<String>> = mapOf(
        "Seleccione un estado..." to arrayOf("Seleccione una ciudad..."),
        // Argentina
        "Buenos Aires" to arrayOf("Seleccione una ciudad...", "La Plata", "Mar del Plata", "Bahía Blanca", "Tandil"),
        "Córdoba" to arrayOf("Seleccione una ciudad...", "Córdoba", "Villa Carlos Paz", "Río Cuarto"),
        "Mendoza" to arrayOf("Seleccione una ciudad...", "Mendoza", "San Rafael"),
        "Santa Fe" to arrayOf("Seleccione una ciudad...", "Santa Fe", "Rosario"),
        "Tucumán" to arrayOf("Seleccione una ciudad...", "San Miguel de Tucumán"),

        // Bolivia
        "Cochabamba" to arrayOf("Seleccione una ciudad...", "Cochabamba"),
        "La Paz" to arrayOf("Seleccione una ciudad...", "La Paz", "El Alto"),
        "Santa Cruz" to arrayOf("Seleccione una ciudad...", "Santa Cruz de la Sierra"),

        // Brasil
        "Bahia" to arrayOf("Seleccione una ciudad...", "Salvador"),
        "Minas Gerais" to arrayOf("Seleccione una ciudad...", "Belo Horizonte"),
        "Paraná" to arrayOf("Seleccione una ciudad...", "Curitiba"),
        "Rio de Janeiro" to arrayOf("Seleccione una ciudad...", "Rio de Janeiro", "Niterói", "Duque de Caxias"),
        "São Paulo" to arrayOf("Seleccione una ciudad...", "São Paulo", "Campinas", "Santos", "SBC"),

        // Chile
        "Antofagasta" to arrayOf("Seleccione una ciudad...", "Antofagasta", "Calama"),
        "Biobío" to arrayOf("Seleccione una ciudad...", "Concepción", "Talcahuano"),
        "Región Metropolitana" to arrayOf("Seleccione una ciudad...", "Santiago", "Puente Alto", "Maipú"),
        "Valparaíso" to arrayOf("Seleccione una ciudad...", "Valparaíso", "Viña del Mar", "Quilpué"),

        // Colombia
        "Amazonas" to arrayOf("Seleccione una ciudad...", "Leticia"),
        "Antioquia" to arrayOf("Seleccione una ciudad...", "Medellín", "Bello", "Itagüí", "Envigado", "Apartadó", "Rionegro"),
        "Arauca" to arrayOf("Seleccione una ciudad...", "Arauca"),
        "Atlántico" to arrayOf("Seleccione una ciudad...", "Barranquilla", "Soledad", "Malambo"),
        "Bolívar" to arrayOf("Seleccione una ciudad...", "Cartagena", "Magangué"),
        "Boyacá" to arrayOf("Seleccione una ciudad...", "Tunja", "Duitama", "Sogamoso"),
        "Caldas" to arrayOf("Seleccione una ciudad...", "Manizales"),
        "Caquetá" to arrayOf("Seleccione una ciudad...", "Florencia"),
        "Casanare" to arrayOf("Seleccione una ciudad...", "Yopal"),
        "Cauca" to arrayOf("Seleccione una ciudad...", "Popayán"),
        "Cesar" to arrayOf("Seleccione una ciudad...", "Valledupar"),
        "Chocó" to arrayOf("Seleccione una ciudad...", "Quibdó"),
        "Córdoba" to arrayOf("Seleccione una ciudad...", "Montería"),
        "Cundinamarca" to arrayOf("Seleccione una ciudad...", "Bogotá", "Soacha", "Facatativá", "Girardot", "Zipaquirá"),
        "Guainía" to arrayOf("Seleccione una ciudad...", "Inírida"),
        "Guaviare" to arrayOf("Seleccione una ciudad...", "San José del Guaviare"),
        "Huila" to arrayOf("Seleccione una ciudad...", "Neiva"),
        "La Guajira" to arrayOf("Seleccione una ciudad...", "Riohacha", "Maicao"),
        "Magdalena" to arrayOf("Seleccione una ciudad...", "Santa Marta", "Ciénaga"),
        "Meta" to arrayOf("Seleccione una ciudad...", "Villavicencio"),
        "Nariño" to arrayOf("Seleccione una ciudad...", "Pasto", "Tumaco"),
        "Norte de Santander" to arrayOf("Seleccione una ciudad...", "Cúcuta", "Ocaña"),
        "Putumayo" to arrayOf("Seleccione una ciudad...", "Mocoa"),
        "Quindío" to arrayOf("Seleccione una ciudad...", "Armenia"),
        "Risaralda" to arrayOf("Seleccione una ciudad...", "Pereira", "Dosquebradas"),
        "San Andrés y Providencia" to arrayOf("Seleccione una ciudad...", "San Andrés"),
        "Santander" to arrayOf("Seleccione una ciudad...", "Bucaramanga", "Floridablanca", "Barrancabermeja"),
        "Sucre" to arrayOf("Seleccione una ciudad...", "Sincelejo"),
        "Tolima" to arrayOf("Seleccione una ciudad...", "Ibagué"),
        "Valle del Cauca" to arrayOf("Seleccione una ciudad...", "Cali", "Buenaventura", "Palmira", "Tuluá", "Cartago", "Buga"),
        "Vaupés" to arrayOf("Seleccione una ciudad...", "Mitú"),
        "Vichada" to arrayOf("Seleccione una ciudad...", "Puerto Carreño"),

        // Costa Rica
        "Alajuela" to arrayOf("Seleccione una ciudad...", "Alajuela"),
        "Heredia" to arrayOf("Seleccione una ciudad...", "Heredia"),
        "San José" to arrayOf("Seleccione una ciudad...", "San José", "Escazú"),

        // Ecuador
        "Azuay" to arrayOf("Seleccione una ciudad...", "Cuenca", "Girón", "Gualaceo", "Paute", "Sígsig"),
        "Bolívar" to arrayOf("Seleccione una ciudad...", "Caluma", "Chimbo", "Guaranda", "San Miguel"),
        "Cañar" to arrayOf("Seleccione una ciudad...", "Azogues", "Biblián", "Cañar", "La Troncal"),
        "Carchi" to arrayOf("Seleccione una ciudad...", "El Ángel", "Mira", "San Gabriel", "Tulcán"),
        "Chimborazo" to arrayOf("Seleccione una ciudad...", "Alausí", "Chambo", "Colta", "Guano", "Riobamba"),
        "Cotopaxi" to arrayOf("Seleccione una ciudad...", "La Maná", "Latacunga", "Pujilí", "Salcedo", "Saquisilí"),
        "El Oro" to arrayOf("Seleccione una ciudad...", "Huaquillas", "Machala", "Pasaje", "Piñas", "Santa Rosa", "Zaruma"),
        "Esmeraldas" to arrayOf("Seleccione una ciudad...", "Atacames", "Esmeraldas", "Muisne", "Quinindé", "San Lorenzo"),
        "Galápagos" to arrayOf("Seleccione una ciudad...", "Puerto Ayora", "Puerto Baquerizo Moreno", "Puerto Villamil"),
        "Guayas" to arrayOf("Seleccione una ciudad...", "Daule", "Durán", "El Triunfo", "Guayaquil", "Milagro", "Naranjal", "Playas", "Samborondón", "Yaguachi"),
        "Imbabura" to arrayOf("Seleccione una ciudad...", "Atuntaqui", "Cotacachi", "Ibarra", "Otavalo", "Pimampiro"),
        "Loja" to arrayOf("Seleccione una ciudad...", "Alamor", "Cariamanga", "Catamayo", "Loja", "Macará"),
        "Los Ríos" to arrayOf("Seleccione una ciudad...", "Baba", "Babahoyo", "Puebloviejo", "Quevedo", "Ventanas", "Vinces"),
        "Manabí" to arrayOf("Seleccione una ciudad...", "Bahía de Caráquez", "Chone", "El Carmen", "Jipijapa", "Manta", "Pedernales", "Portoviejo"),
        "Morona Santiago" to arrayOf("Seleccione una ciudad...", "Gualaquiza", "Macas", "Palora", "Sucúa"),
        "Napo" to arrayOf("Seleccione una ciudad...", "Archidona", "Baeza", "El Chaco", "Tena"),
        "Orellana" to arrayOf("Seleccione una ciudad...", "La Joya de los Sachas", "Loreto", "Puerto Francisco de Orellana (Coca)"),
        "Pastaza" to arrayOf("Seleccione una ciudad...", "Arajuno", "Mera", "Puyo", "Santa Clara"),
        "Pichincha" to arrayOf("Seleccione una ciudad...", "Cayambe", "Machachi", "Pedro Vicente Maldonado", "Quito", "Sangolquí", "Tabacundo"),
        "Santa Elena" to arrayOf("Seleccione una ciudad...", "La Libertad", "Manglaralto", "Salinas", "Santa Elena"),
        "Santo Domingo de los Tsáchilas" to arrayOf("Seleccione una ciudad...", "La Concordia", "Santo Domingo"),
        "Sucumbíos" to arrayOf("Seleccione una ciudad...", "Cascales", "Nueva Loja (Lago Agrio)", "Putumayo", "Shushufindi"),
        "Tungurahua" to arrayOf("Seleccione una ciudad...", "Ambato", "Baños de Agua Santa", "Patate", "Pelileo", "Píllaro"),
        "Zamora Chinchipe" to arrayOf("Seleccione una ciudad...", "El Pangui", "Yantzaza", "Zamora", "Zumba"),

        // Estados Unidos
        "California" to arrayOf("Seleccione una ciudad...", "Los Angeles", "San Diego", "San Francisco", "San Jose", "Fresno"),
        "Florida" to arrayOf("Seleccione una ciudad...", "Miami", "Orlando", "Tampa", "Jacksonville", "Tallahassee"),
        "Illinois" to arrayOf("Seleccione una ciudad...", "Chicago", "Aurora", "Rockford"),
        "New York" to arrayOf("Seleccione una ciudad...", "New York City", "Buffalo", "Rochester", "Albany"),
        "Texas" to arrayOf("Seleccione una ciudad...", "Houston", "San Antonio", "Dallas", "Austin", "Fort Worth", "El Paso"),

        // México
        "Aguascalientes" to arrayOf("Seleccione una ciudad...", "Aguascalientes"),
        "Baja California" to arrayOf("Seleccione una ciudad...", "Tijuana", "Mexicali", "Ensenada"),
        "Baja California Sur" to arrayOf("Seleccione una ciudad...", "La Paz", "Los Cabos"),
        "Campeche" to arrayOf("Seleccione una ciudad...", "Campeche"),
        "Chiapas" to arrayOf("Seleccione una ciudad...", "Tuxtla Gutiérrez", "Tapachula"),
        "Chihuahua" to arrayOf("Seleccione una ciudad...", "Chihuahua", "Ciudad Juárez"),
        "Ciudad de México" to arrayOf("Seleccione una ciudad...", "Ciudad de México"),
        "Coahuila" to arrayOf("Seleccione una ciudad...", "Saltillo", "Torreón"),
        "Colima" to arrayOf("Seleccione una ciudad...", "Colima", "Manzanillo"),
        "Durango" to arrayOf("Seleccione una ciudad...", "Durango"),
        "Estado de México" to arrayOf("Seleccione una ciudad...", "Toluca", "Ecatepec", "Naucalpan", "Tlalnepantla"),
        "Guanajuato" to arrayOf("Seleccione una ciudad...", "Guanajuato", "León", "Irapuato", "Celaya"),
        "Guerrero" to arrayOf("Seleccione una ciudad...", "Chilpancingo", "Acapulco"),
        "Hidalgo" to arrayOf("Seleccione una ciudad...", "Pachuca"),
        "Jalisco" to arrayOf("Seleccione una ciudad...", "Guadalajara", "Zapopan", "Tlaquepaque", "Puerto Vallarta"),
        "Michoacán" to arrayOf("Seleccione una ciudad...", "Morelia"),
        "Morelos" to arrayOf("Seleccione una ciudad...", "Cuernavaca"),
        "Nayarit" to arrayOf("Seleccione una ciudad...", "Tepic"),
        "Nuevo León" to arrayOf("Seleccione una ciudad...", "Monterrey", "San Nicolás de los Garza", "San Pedro Garza García", "Guadalupe"),
        "Oaxaca" to arrayOf("Seleccione una ciudad...", "Oaxaca"),
        "Puebla" to arrayOf("Seleccione una ciudad...", "Puebla", "Tehuacán"),
        "Querétaro" to arrayOf("Seleccione una ciudad...", "Querétaro"),
        "Quintana Roo" to arrayOf("Seleccione una ciudad...", "Cancún", "Chetumal", "Playa del Carmen"),
        "San Luis Potosí" to arrayOf("Seleccione una ciudad...", "San Luis Potosí"),
        "Sinaloa" to arrayOf("Seleccione una ciudad...", "Culiacán", "Mazatlán"),
        "Sonora" to arrayOf("Seleccione una ciudad...", "Hermosillo", "Ciudad Obregón"),
        "Tabasco" to arrayOf("Seleccione una ciudad...", "Villahermosa"),
        "Tamaulipas" to arrayOf("Seleccione una ciudad...", "Ciudad Victoria", "Reynosa", "Matamoros", "Nuevo Laredo"),
        "Tlaxcala" to arrayOf("Seleccione una ciudad...", "Tlaxcala"),
        "Veracruz" to arrayOf("Seleccione una ciudad...", "Xalapa", "Veracruz", "Coatzacoalcos"),
        "Yucatán" to arrayOf("Seleccione una ciudad...", "Mérida"),
        "Zacatecas" to arrayOf("Seleccione una ciudad...", "Zacatecas"),

        // Panamá
        "Chiriquí" to arrayOf("Seleccione una ciudad...", "David"),
        "Colón" to arrayOf("Seleccione una ciudad...", "Colón"),
        "Panamá" to arrayOf("Seleccione una ciudad...", "Ciudad de Panamá", "San Miguelito"),
        "Veraguas" to arrayOf("Seleccione una ciudad...", "Santiago"),

        // Perú
        "Arequipa" to arrayOf("Seleccione una ciudad...", "Arequipa"),
        "Callao" to arrayOf("Seleccione una ciudad...", "Callao"),
        "Cusco" to arrayOf("Seleccione una ciudad...", "Cusco"),
        "La Libertad" to arrayOf("Seleccione una ciudad...", "Trujillo"),
        "Lima" to arrayOf("Seleccione una ciudad...", "Lima Metropolitana"),

        // Venezuela
        "Amazonas" to arrayOf("Seleccione una ciudad...", "Puerto Ayacucho"),
        "Anzoátegui" to arrayOf("Seleccione una ciudad...", "Barcelona", "Puerto La Cruz", "Lecheria", "Guanta", "El Tigre", "Anaco"),
        "Apure" to arrayOf("Seleccione una ciudad...", "San Fernando de Apure"),
        "Aragua" to arrayOf("Seleccione una ciudad...", "Maracay", "Turmero", "La Victoria"),
        "Barinas" to arrayOf("Seleccione una ciudad...", "Barinas"),
        "Bolívar" to arrayOf("Seleccione una ciudad...", "Ciudad Bolívar", "Ciudad Guayana", "Upata"),
        "Carabobo" to arrayOf("Seleccione una ciudad...", "Valencia", "Puerto Cabello", "Guacara"),
        "Cojedes" to arrayOf("Seleccione una ciudad...", "San Carlos"),
        "Delta Amacuro" to arrayOf("Seleccione una ciudad...", "Tucupita"),
        "Distrito Capital" to arrayOf("Seleccione una ciudad...", "Caracas"),
        "Falcón" to arrayOf("Seleccione una ciudad...", "Coro", "Punto Fijo"),
        "Guárico" to arrayOf("Seleccione una ciudad...", "San Juan de los Morros", "Valle de la Pascua", "Calabozo"),
        "Lara" to arrayOf("Seleccione una ciudad...", "Barquisimeto", "Cabudare", "Carora"),
        "Mérida" to arrayOf("Seleccione una ciudad...", "Mérida", "El Vigía"),
        "Miranda" to arrayOf("Seleccione una ciudad...", "Los Teques", "Chacao", "Baruta", "Guarenas", "Guatire", "Petare"),
        "Monagas" to arrayOf("Seleccione una ciudad...", "Maturín"),
        "Nueva Esparta" to arrayOf("Seleccione una ciudad...", "La Asunción", "Porlamar"),
        "Portuguesa" to arrayOf("Seleccione una ciudad...", "Guanare", "Acarigua"),
        "Sucre" to arrayOf("Seleccione una ciudad...", "Cumaná", "Carúpano"),
        "Táchira" to arrayOf("Seleccione una ciudad...", "San Cristóbal"),
        "Trujillo" to arrayOf("Seleccione una ciudad...", "Trujillo", "Valera"),
        "Vargas (La Guaira)" to arrayOf("Seleccione una ciudad...", "La Guaira", "Catia La Mar"),
        "Yaracuy" to arrayOf("Seleccione una ciudad...", "San Felipe"),
        "Zulia" to arrayOf("Seleccione una ciudad...", "Maracaibo", "Cabimas", "Ciudad Ojeda", "San Francisco")
    )

    private val laborCostByCountry: Map<String, Double> = mapOf(
        "Argentina" to 60.0,
        "Bolivia" to 15.0,
        "Brasil" to 35.0,
        "Chile" to 30.0,
        "Colombia" to 28.0,
        "Costa Rica" to 25.0,
        "Ecuador" to 20.0,
        "Estados Unidos" to 120.0,
        "México" to 25.0,
        "Panamá" to 30.0,
        "Paraguay" to 18.0,
        "Perú" to 20.0,
        "Uruguay" to 35.0,
        "Venezuela" to 20.0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cambiado a gemini-1.5-flash para corregir el error 404
        // Otros modelos: gemini-2.5-flash, gemini-3.6-flash, gemini-3.7-flash
        geminiModel = GenerativeModel(modelName = "gemini-3.6-flash", apiKey = BuildConfig.GEMINI_API_KEY)

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                tomarFoto(adapter.currentPosition)
            } else {
                Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }

        takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentPhotoUri?.let { uri ->
                    fotoList[adapter.currentPosition].imagenUri = uri
                    fotoList[adapter.currentPosition].isFotoTomada = true
                    adapter.notifyItemChanged(adapter.currentPosition)
                }
            }
        }

        uploadPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                fotoList[adapter.currentPosition].imagenUri = it
                fotoList[adapter.currentPosition].isFotoTomada = false
                adapter.notifyItemChanged(adapter.currentPosition)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_presupuesto, container, false)

        etCaseNumber = view.findViewById(R.id.etCaseNumber)
        etFullName = view.findViewById(R.id.etFullName)
        etEmail = view.findViewById(R.id.etEmail)
        etDateOfInspection = view.findViewById(R.id.etDateOfInspection)
        etVINnumber = view.findViewById(R.id.etVINnumber)
        etVehicleYear = view.findViewById(R.id.etVehicleYear)
        spinnerTipoVehiculo = view.findViewById(R.id.spinnerTipoVehiculo)
        spinnerMarcaVehiculo = view.findViewById(R.id.spinnerMarcaVehiculo)
        spinnerModeloVehiculo = view.findViewById(R.id.spinnerModeloVehiculo)
        spinnerVehicleColor = view.findViewById(R.id.spinnerVehicleColor)
        spinnerCountry = view.findViewById(R.id.spinnerCountry)
        spinnerState = view.findViewById(R.id.spinnerState)
        spinnerCity = view.findViewById(R.id.spinnerCity)

        //spinnerTipoFotoVin = view.findViewById(R.id.spinnerTipoFotoVin) // Se mantiene si se usa en otra parte

        btnCancelar = view.findViewById(R.id.btnCancelar)
        btnAceptar = view.findViewById(R.id.btnAceptar)
        progressBar = view.findViewById(R.id.progressBar)

        btnCancelar.setOnClickListener {
            Log.d("PresupuestoFragment", "Cancelar button clicked.")
            limpiarFormulario()
        }

        btnAceptar.setOnClickListener {
            Log.d("PresupuestoFragment", "Aceptar button clicked.")
            validarYProcesarDatos()
        }

        etDateOfInspection.setOnClickListener { mostrarDatePicker() }
        etVehicleYear.setOnClickListener { mostrarYearPicker() }

        configurarSpinnerMarcasVehiculosEcuador()
        configurarSpinnerTipoVehiculo()
        configurarSpinnerVehicleColor()
        configurarSpinnerCountry()

        fotoList.add(FotoItem())
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = FotoAdapter(
            requireContext(),
            fotoList,
            onAddClickListener = {
                Log.d("PresupuestoFragment", "Add photo button clicked.")
                agregarLinea()
            },
            onDeleteClickListener = {
                Log.d("PresupuestoFragment", "Delete photo button clicked for position $it.")
                borrarLinea(it)
            },
            onTakePhotoClickListener = { position ->
                Log.d("PresupuestoFragment", "Take photo button clicked for position $position.")
                tomarFoto(position)
            },
            onUploadPhotoClickListener = { position ->
                Log.d("PresupuestoFragment", "Upload photo button clicked for position $position.")
                subirFoto(position)
            }
        )
        recyclerView.adapter = adapter
        return view
    }

    /**
     * Limpia todos los campos del formulario y restablece la lista de fotos.
     */
    private fun limpiarFormulario() {
        etCaseNumber.text?.clear()
        etFullName.text?.clear()
        etEmail.text?.clear()
        etDateOfInspection.text?.clear()
        etVINnumber.text?.clear()
        etVehicleYear.text?.clear()

        spinnerTipoVehiculo.setSelection(0)
        spinnerMarcaVehiculo.setSelection(0)
        spinnerModeloVehiculo.setSelection(0)
        spinnerVehicleColor.setSelection(0)
        spinnerCountry.setSelection(0)
        spinnerState.setSelection(0)
        spinnerCity.setSelection(0)

        spinnerTipoFotoVin.setSelection(0)

        val oldSize = fotoList.size
        fotoList.clear()
        fotoList.add(FotoItem())
        
        if (oldSize > 0) {
            adapter.notifyItemRangeRemoved(0, oldSize)
        }
        adapter.notifyItemInserted(0)
        
        Log.d("PresupuestoFragment", "Formulario limpiado.")
    }

    /**
     * Valida los datos del formulario y las fotos del vehículo. Si son válidos, inicia la generación del informe de daños.
     */
    private fun validarYProcesarDatos() {
        Log.d("PresupuestoFragment", "validarYProcesarDatos() called.")
        if (validarCampos() && validarFotosVehiculo()) {
            Log.d("PresupuestoFragment", "Validación exitosa. Llamando a Gemini API.")
            generateDamageReport() // Llama directamente a la API de Gemini
        } else {
            Log.w("PresupuestoFragment", "Validación fallida. Mostrando toast de error.")
            Toast.makeText(requireContext(), "Por favor, complete todos los campos y una foto del vehículo", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Valida que los campos obligatorios (Año, Marca, Modelo, Color y Ubicación) estén completos.
     * Los campos como Número de Caso, Nombre, Email, VIN y Fecha de Inspección son opcionales.
     *
     * @return Verdadero si los campos obligatorios son válidos, falso en caso contrario.
     */
    private fun validarCampos(): Boolean {
        Log.d("Validacion", "Iniciando validación de campos obligatorios.")

        // Validar Año (Obligatorio)
        if (etVehicleYear.text.isNullOrBlank()) {
            Log.d("Validacion", "Año del vehículo vacío.")
            Toast.makeText(requireContext(), "El año del vehículo es obligatorio", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validar Spinners Obligatorios
        val spinnersObligatorios = mapOf(
            spinnerMarcaVehiculo to "Marca",
            spinnerModeloVehiculo to "Modelo",
            spinnerVehicleColor to "Color",
            spinnerCountry to "País",
            spinnerState to "Estado/Provincia",
            spinnerCity to "Ciudad"
        )

        for ((spinner, nombre) in spinnersObligatorios) {
            if (spinner.selectedItemPosition == 0) {
                Log.d("Validacion", "Spinner $nombre no seleccionado.")
                Toast.makeText(requireContext(), "El campo $nombre es obligatorio", Toast.LENGTH_SHORT).show()
                return false
            }
        }

        // Validar formato de Email SOLO si no está vacío
        val email = etEmail.text.toString()
        if (email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Log.d("Validacion", "Formato de email inválido.")
            Toast.makeText(requireContext(), "Por favor, ingrese un email válido o déjelo en blanco", Toast.LENGTH_SHORT).show()
            return false
        }

        Log.d("Validacion", "Validación de campos obligatorios exitosa.")
        return true
    }

    /**
     * Valida que se haya agregado al menos una foto y que todas las fotos tengan un URI de imagen y un tipo seleccionado.
     *
     * @return Verdadero si las fotos son válidas, falso en caso contrario.
     */
    private fun validarFotosVehiculo(): Boolean {
        Log.d("Validacion", "Iniciando validación de fotos del vehículo.")
        if (fotoList.isEmpty()) {
            Log.d("Validacion", "No hay fotos en la lista.")
            Toast.makeText(requireContext(), "Por favor, agregue al menos una foto del vehículo", Toast.LENGTH_SHORT).show()
            return false
        }
        for (fotoItem in fotoList) {
            if (fotoItem.imagenUri == null) {
                Log.d("Validacion", "Falta imagen URI en un ítem de foto.")
                Toast.makeText(requireContext(), "Por favor, agregue al menos una foto del vehículo", Toast.LENGTH_SHORT).show()
                return false
            }
            if (fotoItem.tipoFoto == "Seleccione un tipo...") {
                Log.d("Validacion", "Tipo de foto de vehículo no seleccionado en un ítem.")
                Toast.makeText(requireContext(), "Por favor, seleccione un tipo para todas las fotos del vehículo", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        Log.d("Validacion", "Validación de fotos del vehículo completada y exitosa.")
        return true
    }

    /**
     * Agrega un nuevo [FotoItem] vacío a la lista y notifica al adaptador.
     */
    private fun agregarLinea() {
        fotoList.add(FotoItem())
        adapter.notifyItemInserted(fotoList.size - 1)
        Log.d("PresupuestoFragment", "Nueva línea de foto agregada. Total: ${fotoList.size}")
    }

    /**
     * Elimina un [FotoItem] en la posición especificada.
     * Si es el último ítem, lo limpia en lugar de eliminarlo para mantener al menos una fila.
     *
     * @param position La posición del ítem a eliminar.
     */
    private fun borrarLinea(position: Int) {
        if (fotoList.size > 1) {
            fotoList.removeAt(position)
            adapter.notifyItemRemoved(position)
            adapter.notifyItemRangeChanged(position, fotoList.size - position)
            Log.d("PresupuestoFragment", "Línea de foto borrada en posición $position. Total: ${fotoList.size}")
        } else {
            fotoList.clear()
            fotoList.add(FotoItem())
            adapter.notifyItemChanged(0)
            Log.d("PresupuestoFragment", "Última línea de foto borrada y restablecida a una por defecto.")
        }
    }

    /**
     * Muestra un [DatePickerDialog] para seleccionar la fecha de inspección.
     */
    private fun mostrarDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDayOfMonth ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(selectedYear, selectedMonth, selectedDayOfMonth)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            etDateOfInspection.setText(dateFormat.format(selectedDate.time))
            Log.d("PresupuestoFragment", "Fecha de inspección seleccionada: ${dateFormat.format(selectedDate.time)}")
        }, year, month, day)
        datePickerDialog.show()
    }

    /**
     * Muestra un diálogo para seleccionar el año del vehículo utilizando un NumberPicker.
     * Esto evita el uso de DatePickerDialog para selección de solo año, eliminando warnings de deprecación.
     */
    private fun mostrarYearPicker() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        
        val numberPicker = android.widget.NumberPicker(requireContext()).apply {
            minValue = 1950 // Año mínimo razonable para vehículos
            maxValue = currentYear + 1 // Permitir el año siguiente (modelos nuevos)
            value = if (etVehicleYear.text.isNullOrBlank()) currentYear else etVehicleYear.text.toString().toInt()
            wrapSelectorWheel = false
        }

        val container = android.widget.FrameLayout(requireContext()).apply {
            val params = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER
            }
            addView(numberPicker, params)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Seleccione el Año")
            .setView(container)
            .setPositiveButton("Aceptar") { _, _ ->
                etVehicleYear.setText(numberPicker.value.toString())
                Log.d("PresupuestoFragment", "Año del vehículo seleccionado: ${numberPicker.value}")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Activa la cámara para tomar una foto para una posición específica en la lista.
     * Verifica primero los permisos de la cámara.
     *
     * @param position La posición del ítem de foto en el adaptador.
     */
    private fun tomarFoto(position: Int) {
        Log.d("PresupuestoFragment", "Attempting to take photo for position $position.")
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val photoFile: File? = try {
                crearArchivoTemporal(fotoList[position].tipoFoto)
            } catch (ex: IOException) {
                Log.e("PresupuestoFragment", "Error al crear el archivo temporal para la foto: ${ex.message}", ex)
                Toast.makeText(requireContext(), "Error al crear el archivo", Toast.LENGTH_SHORT).show()
                null
            }
            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    it
                )
                currentPhotoUri = photoURI
                takePhotoLauncher.launch(photoURI)
                Log.d("PresupuestoFragment", "Launched camera for URI: $photoURI")
            }
        } else {
            Log.d("PresupuestoFragment", "Requesting camera permission.")
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    /**
     * Lanza el selector de archivos del sistema para subir una imagen desde la galería.
     *
     * @param position La posición del ítem de foto en el adaptador.
     */
    private fun subirFoto(position: Int) {
        Log.d("PresupuestoFragment", "Attempting to upload photo for position $position.")
        uploadPhotoLauncher.launch("image/*")
    }

    /**
     * Crea un archivo temporal en el directorio de almacenamiento externo de la aplicación para guardar una foto capturada.
     *
     * @param tipoFoto El tipo de foto, utilizado en el nombre del archivo.
     * @return El [File] creado.
     */
    private fun crearArchivoTemporal(tipoFoto: String): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(null)
        val file = File.createTempFile(
            "JPEG_${tipoFoto}_${timeStamp}_",
            ".jpg",
            storageDir
        )
        Log.d("PresupuestoFragment", "Archivo temporal creado: ${file.absolutePath}")
        return file
    }

    private fun configurarSpinnerTipoVehiculo() {
        val tiposVehiculo = arrayOf(
            "Seleccione un tipo de vehículo...",
            "Automóvil", "Camioneta", "Camión", "Motocicleta", "Bus", "Tractor", "Remolque", "Otros"
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tiposVehiculo)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipoVehiculo.adapter = adapter
        Log.d("PresupuestoFragment", "Spinner Tipo Vehículo configurado.")
    }

    /**
     * Configura el Spinner para las marcas de vehículos y establece un listener para actualizar el Spinner de modelos.
     */
    private fun configurarSpinnerMarcasVehiculosEcuador() {
        val marcas = modelosPorMarca.keys.toTypedArray()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, marcas)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMarcaVehiculo.adapter = adapter

        spinnerMarcaVehiculo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val marcaSeleccionada = parent?.getItemAtPosition(position).toString()
                configurarSpinnerModeloVehiculo(marcaSeleccionada)
                Log.d("PresupuestoFragment", "Marca seleccionada: $marcaSeleccionada")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No hacer nada
            }
        }
        Log.d("PresupuestoFragment", "Spinner Marcas Vehículos configurado.")
    }

    private fun configurarSpinnerModeloVehiculo(marca: String) {
        val modelos = modelosPorMarca[marca] ?: arrayOf("Seleccione un modelo...")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, modelos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerModeloVehiculo.adapter = adapter
        Log.d("PresupuestoFragment", "Spinner Modelo Vehículo configurado para marca: $marca")
    }

    private fun configurarSpinnerVehicleColor() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, colors)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerVehicleColor.adapter = adapter
        Log.d("PresupuestoFragment", "Spinner Vehicle Color configurado.")
    }

    private fun configurarSpinnerCountry() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, countries)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCountry.adapter = adapter

        spinnerCountry.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val countrySelected = parent?.getItemAtPosition(position).toString()
                configurarSpinnerState(countrySelected)
                Log.d("PresupuestoFragment", "País seleccionado: $countrySelected")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        Log.d("PresupuestoFragment", "Spinner Country configurado.")
    }

    private fun configurarSpinnerState(country: String) {
        val states = statesByCountry[country] ?: arrayOf("Seleccione un estado...")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, states)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerState.adapter = adapter

        spinnerState.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val stateSelected = parent?.getItemAtPosition(position).toString()
                configurarSpinnerCity(stateSelected)
                Log.d("PresupuestoFragment", "Estado seleccionado: $stateSelected")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        Log.d("PresupuestoFragment", "Spinner State configurado para país: $country")
    }

    private fun configurarSpinnerCity(state: String) {
        val cities = citiesByState[state] ?: arrayOf("Seleccione una ciudad...")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, cities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCity.adapter = adapter
        Log.d("PresupuestoFragment", "Spinner City configurado para estado: $state")
    }

    /**
     * Genera un informe de daños utilizando el modelo de IA Google Gemini.
     *
     * Este método prepara un prompt con los datos del vehículo, convierte las imágenes seleccionadas en bitmaps,
     * y las envía a la API de Gemini. Se espera que la respuesta sea una cadena JSON
     * que represente el informe de daños, la cual luego se procesa y muestra.
     */
    private fun generateDamageReport() {
        Log.d("GeminiReport", "generateDamageReport() called. Preparando prompt e imágenes.")

        // MOSTRAR EL PROGRESS BAR Y DESHABILITAR EL BOTÓN ANTES DE LA LLAMADA A LA API
        progressBar.visibility = VISIBLE
        btnAceptar.isEnabled = false
        btnCancelar.isEnabled = false

        val vehicleData = mapOf(
            "marca" to spinnerMarcaVehiculo.selectedItem.toString(),
            "modelo" to spinnerModeloVehiculo.selectedItem.toString(),
            "anio" to etVehicleYear.text.toString(),
            "ubicacion" to spinnerCity.selectedItem.toString() + ", " + spinnerCountry.selectedItem.toString(),
            "color" to spinnerVehicleColor.selectedItem.toString()
        )

        val imagesForGemini = mutableListOf<Bitmap>()
        fotoList.forEach { fotoItem ->
            fotoItem.imagenUri?.let { uri ->
                uriToBitmap(uri)?.let { bitmap ->
                    imagesForGemini.add(bitmap)
                    Log.d("GeminiReport", "Imagen agregada para Gemini desde URI: $uri")
                } ?: Log.e("GeminiReport", "No se pudo convertir URI a Bitmap para Gemini: $uri")
            } ?: Log.w("GeminiReport", "URI de imagen nula para Gemini.")
        }

        if (imagesForGemini.isEmpty()) {
            // OCULTAR EL PROGRESS BAR Y HABILITAR EL BOTÓN SI NO HAY IMÁGENES
            progressBar.visibility = GONE
            btnAceptar.isEnabled = true
            btnCancelar.isEnabled = true

            Toast.makeText(requireContext(), "No se encontraron imágenes válidas para generar el informe de daños.", Toast.LENGTH_SHORT).show()
            Log.w("GeminiReport", "No hay imágenes disponibles para la generación del informe de Gemini.")
            return
        }

        val selectedCountry = spinnerCountry.selectedItem.toString()
        val costoHoraManoObra = laborCostByCountry[selectedCountry] ?: 20.0
        
        val promptText = """
            Eres un perito automotriz profesional especializado en valoración de daños de vehículos. Tu tarea es generar un informe detallado de daños, indicando si hay partes para reemplazar y reparar para un vehículo chocado.

            Información del vehículo:
            - Marca: ${vehicleData["marca"]}
            - Modelo: ${vehicleData["modelo"]}
            - Año: ${vehicleData["anio"]}
            - Color: ${vehicleData["color"]}
            - Ubicación de Valoración: ${vehicleData["ubicacion"]}
            - Costo por hora de mano de obra: $costoHoraManoObra

            Analiza exhaustivamente las imágenes proporcionadas para identificar todos los daños visibles en la carrocería, estructura y componentes mecánicos.

            El informe debe contener las siguientes secciones estructuradas en formato JSON:
            1.  **"DatosGenerales"**: Con la información del vehículo y el costo por hora de mano de obra utilizada.
            2.  **"DescripcionDanosExistentes"**: Una descripción detallada de los daños por zona (ej. "Parte Trasera Izquierda").
            3.  **"ListadoPiezasAfectadas"**: Una lista de componentes que necesitan reemplazo o reparación, con la acción sugerida en una clave llamada suguerencia, 
                un promedio de horas estimadas para la mano de obra en una clave llamada CantidadEstimadoManoObra a un costo de $costoHoraManoObra por hora de trabajo, 
                desglosado por tareas (desmontaje, reparación estructural, montaje, pintura, etc.), un promedio de costo estimado en USD para las piezas a reemplazar en una clave llamada CostoPieza, 
                ajustado a la ubicación de ${vehicleData["ubicacion"]} y notas..
            4.  **"ConsideracionesAdicionales"**: Puntos importantes a tener en cuenta (daños ocultos, etc.).

            Genera el JSON completo sin ninguna explicación adicional antes o después del código. Asegúrate de que el JSON sea válido.
        """.trimIndent()
        Log.d("GeminiReport", "Prompt de Gemini preparado.")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("GeminiReport", "Llamando a geminiModel.generateContent().")
                val content = content {
                    text(promptText)
                    imagesForGemini.forEach { image(it) }
                }

                val response = geminiModel.generateContent(content)
                Log.d("GeminiReport", "Respuesta recibida de la API de Gemini.")

                withContext(Dispatchers.Main) {
                    // OCULTAR EL PROGRESS BAR Y HABILITAR EL BOTÓN DESPUÉS DE LA RESPUESTA
                    progressBar.visibility = GONE
                    btnAceptar.isEnabled = true
                    btnCancelar.isEnabled = true

                    try {
                        val jsonOutput = response.text
                        Log.d("GeminiReport", "Raw Gemini response text: $jsonOutput")
                        val cleanJsonOutput = if (jsonOutput != null && "```json" in jsonOutput) {
                            jsonOutput.substringAfter("```json").substringBefore("```").trim()
                        } else {
                            jsonOutput?.trim()
                        }
                        Log.d("GeminiReport", "Cleaned Gemini JSON output: $cleanJsonOutput")

                        if (!cleanJsonOutput.isNullOrEmpty()) {
                            val reportJson = JSONObject(cleanJsonOutput)
                            Log.d("GeminiReport", "Informe generado por Gemini:\n${reportJson.toString(2)}")

                            // Función auxiliar para manejar valores vacíos
                            fun getValOrDefault(text: String?): String = if (text.isNullOrBlank()) "No especificado" else text

                            // Información común del usuario y vehículo para el resumen
                            val commonInfo = """
                                --- Información General ---
                                Número de Caso: ${getValOrDefault(etCaseNumber.text?.toString())}
                                Fecha de Inspección: ${getValOrDefault(etDateOfInspection.text?.toString())}
                                --- Información Vehiculo ---
                                Marca: ${vehicleData["marca"]}
                                Modelo: ${vehicleData["modelo"]}
                                Número de VIN: ${getValOrDefault(etVINnumber.text?.toString())}
                                Año: ${vehicleData["anio"]}
                                Color: ${vehicleData["color"]}
                                --- Ubicación ---
                                Ubicación de Valoración: ${vehicleData["ubicacion"]}
                                --- Información Inspector ---
                                Nombre Completo: ${getValOrDefault(etFullName.text?.toString())}
                                Email: ${getValOrDefault(etEmail.text?.toString())}
                                --- Información de Costos ---
                                Costo por hora de mano de obra: $costoHoraManoObra
                                Fotos adjuntas: ${imagesForGemini.size}
                                ---------------------------
                            """.trimIndent()

                            // El showDialog original con el JSON completo
                            showDialog("Informe de Daños Generado (JSON Completo):\n${reportJson.toString(2)}",reportJson.toString(2))

                            val bundle = Bundle().apply {
                                putString("input_data", commonInfo) // Tus datos de entrada
                                putString("api_response", reportJson.toString(2)) // La respuesta JSON de la API
                                // Pasar las URIs de las fotos
                                val photoUris = ArrayList<String>()
                                fotoList.forEach { item ->
                                    item.imagenUri?.let { photoUris.add(it.toString()) }
                                }
                                putStringArrayList("photo_uris", photoUris)
                            }
                            findNavController().navigate(R.id.action_nav_presupuestofragment_to_reportDisplayFragment, bundle)
                            //limpiarFormulario()

                        } else {
                            Toast.makeText(requireContext(), "Gemini no generó un JSON válido para el informe de daños.", Toast.LENGTH_LONG).show()
                            Log.e("GeminiReport", "La respuesta de Gemini estaba vacía o no era un JSON válido: $jsonOutput")
                        }
                    } catch (e: JSONException) {
                        Toast.makeText(requireContext(), "Error al decodificar la respuesta JSON de Gemini: ${e.message}", Toast.LENGTH_LONG).show()
                        Log.e("GeminiReport", "Error de análisis JSON en la respuesta de Gemini: ${response.text}", e)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error al procesar la respuesta de Gemini: ${e.message}", Toast.LENGTH_LONG).show()
                        Log.e("GeminiReport", "Error al procesar la respuesta de Gemini: ${response.text}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiReport", "Excepción durante la llamada a la API de Gemini: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    // OCULTAR EL PROGRESS BAR Y HABILITAR EL BOTÓN EN CASO DE ERROR DE CONEXIÓN
                    progressBar.visibility = GONE
                    btnAceptar.isEnabled = true
                    btnCancelar.isEnabled = true

                    Toast.makeText(requireContext(), "Error al conectar con la API de Gemini: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("GeminiReport", "Error al conectar con la API de Gemini (UI Thread)", e)
                }
            }
        }
    }

    /**
     * Convierte un [Uri] en un [Bitmap].
     *
     * @param uri El URI de la imagen a convertir.
     * @return El [Bitmap] resultante, o nulo si falla la conversión.
     */
    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Log.e("PresupuestoFragment", "Error al convertir URI a Bitmap para Gemini: ${e.message}", e)
            null
        }
    }

    private fun bitmapToFile(context: Context, bitmap: Bitmap, fileName: String): File? {
        val file = File(context.cacheDir, fileName) // Usar cacheDir para archivos temporales
        return try {
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) // Comprimir con calidad 90
            }
            Log.d("PresupuestoFragment", "Bitmap convertido a archivo: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("PresupuestoFragment", "Error al convertir Bitmap a archivo: ${e.message}", e)
            null
        }
    }

    private fun showDialog(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title) // Usar el título pasado como parámetro
            .setMessage(message)
            .setIcon(R.drawable.analyze_list_logs_search_icon)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
        Log.d("PresupuestoFragment", "Diálogo de información '$title' mostrado.")
    }
}