package com.ehome.enpartesapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.ehome.enpartesapp.R
import org.json.JSONObject

class VehicleAdapter(
    private val vehicles: List<VehicleData>,
    private val caracteristicasNombres: List<String>
) :
    RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder>() {

    class VehicleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val brandTextView: TextView = itemView.findViewById(R.id.textViewBrand)
        val modelTextView: TextView = itemView.findViewById(R.id.textViewModel)
        val yearTextView: TextView = itemView.findViewById(R.id.textViewYear)
        val vehicleIdTextView: TextView = itemView.findViewById(R.id.textViewVehicleId)
        val characteristicsTextView: TextView = itemView.findViewById(R.id.textViewCharacteristics)
        val licensePlateTextView: TextView = itemView.findViewById(R.id.textViewLicensePlate)
        val serialTextView: TextView = itemView.findViewById(R.id.textViewSerial)
        val vinTextView: TextView = itemView.findViewById(R.id.textViewVin)
        val selectButton: Button = itemView.findViewById(R.id.buttonSelect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vehicle, parent, false)
        return VehicleViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        val currentVehicle = vehicles[position]
        val vehicleInfo = currentVehicle.datos
        val vehicleCarac = currentVehicle.carac

        if (vehicleInfo != null && vehicleCarac != null) {
            holder.brandTextView.text = vehicleInfo.brandName
            holder.modelTextView.text = vehicleInfo.modelName
            holder.yearTextView.text = vehicleInfo.year
            holder.licensePlateTextView.text = vehicleInfo.licensePlate
            holder.serialTextView.text = vehicleInfo.serialNumber
            holder.vinTextView.text = vehicleInfo.vin

            // Build the characteristics string
            val characteristicsLine = StringBuilder()
            for (carac in vehicleCarac) {
                holder.vehicleIdTextView.text = carac.vehicleId
                // Parse the inner JSON string safely
                carac.carac?.let { caracString ->
                    val caracJson = JSONObject(caracString)

                    // Create the characteristics line.
                    caracteristicasNombres.forEachIndexed { index, characteristic ->
                        if (caracJson.has(characteristic)) {
                            val value = caracJson.getString(characteristic)
                            characteristicsLine.append("$characteristic: $value, ")
                        }
                    }
                }
            }
            holder.characteristicsTextView.text = characteristicsLine.toString()

            holder.selectButton.setOnClickListener {
                // Navigate to ReclamosFragment and pass the vehicleId
                val bundle = Bundle().apply {
                    putString("brandName", vehicleInfo.brandName)
                    putString("modelName", vehicleInfo.modelName)
                    putString("year", vehicleInfo.year)
                    putString("vehicleId", vehicleCarac[0].vehicleId)
                    putString("licensePlate", vehicleInfo.licensePlate)
                    putString("serialNumber", vehicleInfo.serialNumber)
                    putString("vin", vehicleInfo.vin)
                    // AddVehicleCarac data (characteristics)
                    putString("characteristicsLine", characteristicsLine.toString())
                }
                holder.itemView.findNavController().navigate(R.id.action_nav_consultas_abiertas_to_nav_reclamos, bundle)
            }
        }
    }

    override fun getItemCount(): Int = vehicles.size
}