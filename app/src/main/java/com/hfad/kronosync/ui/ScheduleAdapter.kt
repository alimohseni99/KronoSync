package com.hfad.kronosync.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hfad.kronosync.R

data class ScheduleItem(
    val startTime: String,
    val endTime: String,
    val title: String,
    val location: String,
    val description: String
)

class ScheduleAdapter(private var items: List<ScheduleItem>) :
    RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.scheduleTitleTextView)
        val descriptionTextView: TextView = view.findViewById(R.id.scheduleDescriptionTextView)
        val scheduleStartTimeTextView: TextView = view.findViewById(R.id.scheduleStartTimeTextView)
        val scheduleEndTimeTextView: TextView = view.findViewById(R.id.scheduleEndTimeTextView)
        val scheduleLocationTextView: TextView = view.findViewById(R.id.scheduleLocationTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_schedule, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.titleTextView.text = item.title
        holder.descriptionTextView.text = item.description
        holder.scheduleStartTimeTextView.text = item.startTime
        holder.scheduleEndTimeTextView.text = item.endTime
        holder.scheduleLocationTextView.text = item.location

        val backgroundRes =
            if (position % 2 == 0) R.drawable.item_even_background else R.drawable.item_odd_background
        holder.itemView.setBackgroundResource(backgroundRes)

    }

    override fun getItemCount(): Int = items.size

}



