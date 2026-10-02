package com.ehome.autoperitajeia.ui.reclamos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ehome.autoperitajeia.R
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class ClaimAdapter(
    private val claims: List<ReclamosFragment.Claim>,
    private val onSelectClickListener: (ReclamosFragment.Claim) -> Unit,
) : RecyclerView.Adapter<ClaimAdapter.ClaimViewHolder>() {

    class ClaimViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val snumeroSolicitudTextView: TextView = itemView.findViewById(R.id.textViewSnumeroSolicitud)
        val claimNumberTextView: TextView = itemView.findViewById(R.id.textViewClaimNumber)
        val claimDateTextView: TextView = itemView.findViewById(R.id.textViewClaimDate)
        val selectButton: Button = itemView.findViewById(R.id.buttonSelect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClaimViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.claim_item, parent, false)
        return ClaimViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ClaimViewHolder, position: Int) {
        val currentClaim = claims[position]
        holder.snumeroSolicitudTextView.text = currentClaim.snumerosolicitud
        holder.claimNumberTextView.text = currentClaim.claimNumber
        holder.claimDateTextView.text = formatDate(currentClaim.claimDate)

        holder.selectButton.setOnClickListener {
            onSelectClickListener(currentClaim)
        }
    }

    override fun getItemCount(): Int = claims.size

    // Function to format date to DD/MM/YYYY
    private fun formatDate(inputDate: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        return try {
            val date = inputFormat.parse(inputDate)
            date?.let { outputFormat.format(it) } ?: "Fecha Inválida"
        } catch (_: ParseException) {
            "Fecha Inválida"
        }
    }
}