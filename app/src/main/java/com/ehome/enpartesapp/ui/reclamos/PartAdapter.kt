package com.ehome.enpartesapp.ui.reclamos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ehome.enpartesapp.R

class PartAdapter(private val parts: List<ReclamosFragment.Part>) :
    RecyclerView.Adapter<PartAdapter.PartViewHolder>() {

    class PartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
        val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
        val amountTextView: TextView = itemView.findViewById(R.id.amountTextView)
        val conditionTextView: TextView = itemView.findViewById(R.id.conditionTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_partes, parent, false)
        return PartViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PartViewHolder, position: Int) {
        val part = parts[position]
        holder.descriptionTextView.text = holder.itemView.context.getString(R.string.descripcion, part.description)
        holder.statusTextView.text = holder.itemView.context.getString(R.string.status, part.status)
        holder.amountTextView.text = holder.itemView.context.getString(R.string.cantidad, part.ammount)
        holder.conditionTextView.text = holder.itemView.context.getString(R.string.condicion, part.condition)
    }

    override fun getItemCount(): Int = parts.size
}