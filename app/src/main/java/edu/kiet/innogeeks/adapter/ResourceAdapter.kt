package edu.kiet.innogeeks.adapter

import android.os.Parcel
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.kiet.innogeeks.R
import edu.kiet.innogeeks.model.Resource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResourceAdapter() : RecyclerView.Adapter<ResourceAdapter.ResourceViewHolder>(), Parcelable {
    private val resources = mutableListOf<Resource>()

    constructor(parcel: Parcel) : this() {
    }

    class ResourceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.resourceTitle)
        val description: TextView = itemView.findViewById(R.id.resourceDescription)
        val timestamp: TextView = itemView.findViewById(R.id.resourceTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResourceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.resource_item, parent, false)
        return ResourceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResourceViewHolder, position: Int) {
        val resource = resources[position]
        holder.title.text = resource.title
        holder.description.text = resource.description
        holder.timestamp.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Date(resource.timestamp))
    }

    override fun getItemCount() = resources.size

    fun updateResources(newResources: List<Resource>) {
        resources.clear()
        resources.addAll(newResources)
        notifyDataSetChanged()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ResourceAdapter> {
        override fun createFromParcel(parcel: Parcel): ResourceAdapter {
            return ResourceAdapter(parcel)
        }

        override fun newArray(size: Int): Array<ResourceAdapter?> {
            return arrayOfNulls(size)
        }
    }
}
