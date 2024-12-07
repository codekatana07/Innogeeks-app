package edu.kiet.innogeeks

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CustomAdapter(
    private val dataSet: ArrayList<Student>,
    private val mContext: Context
) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    private val checkedIDs: ArrayList<String> = ArrayList()  // List of selected student IDs
    private var onItemClickListener: OnItemClickListener? = null

    // Define an interface for click events
    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        onItemClickListener = listener
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtName: TextView = view.findViewById(R.id.txtName)
        val checkBox: CheckBox = view.findViewById(R.id.checkBox)

        init {
            view.setOnClickListener {
                // Notify the activity or fragment when an item is clicked
                onItemClickListener?.onItemClick(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataSet[position]
        holder.txtName.text = item.stdName
        holder.checkBox.isChecked = item.assigned

        // Set a tag for position tracking
        holder.checkBox.tag = position

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            val checkboxPosition = holder.adapterPosition
            if (checkboxPosition != RecyclerView.NO_POSITION) {
                val studentID = dataSet[checkboxPosition].stdID

                if (isChecked) {
                    checkedIDs.add(studentID.toString()) // Add ID to selected list
                } else {
                    checkedIDs.remove(studentID) // Remove ID from selected list
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    fun getCheckedIDs(): ArrayList<String> {
        return checkedIDs
    }
}
