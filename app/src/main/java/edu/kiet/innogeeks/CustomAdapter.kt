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

    private val checkedIDs = HashSet<String>()
    private var onItemClickListener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        onItemClickListener = listener
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtName: TextView = view.findViewById(R.id.txtname)
        val checkBox: CheckBox = view.findViewById(R.id.checkbox)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClickListener?.onItemClick(position)
                }
            }

            checkBox.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val student = dataSet[position]
                    if (checkBox.isChecked) {
                        checkedIDs.add(student.stdID.toString())
                    } else {
                        checkedIDs.remove(student.stdID)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val student = dataSet[position]
        holder.txtName.text = student.stdName

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = student.assigned

        if (student.assigned) {
            checkedIDs.add(student.stdID.toString())
        } else {
            checkedIDs.remove(student.stdID)
        }
    }

    override fun getItemCount(): Int = dataSet.size

    fun getCheckedIDs(): ArrayList<String> = ArrayList(checkedIDs)

    fun updateDataSet(newDataSet: List<Student>) {
        dataSet.clear()
        dataSet.addAll(newDataSet)
        notifyDataSetChanged()
    }
}