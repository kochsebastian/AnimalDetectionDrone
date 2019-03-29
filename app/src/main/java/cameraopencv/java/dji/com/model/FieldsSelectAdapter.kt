package com.dji.importSDKDemo.model

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.view.LayoutInflater
import cameraopencv.java.dji.com.R
import cameraopencv.java.dji.com.RecyclerViewClickListener


class FieldsSelectAdapter(private val listener: RecyclerViewClickListener) :
        RecyclerView.Adapter<FieldsSelectAdapter.ViewHolder>() {

    class ViewHolder(itemView: View, private val listener: RecyclerViewClickListener) :
            RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val textView: TextView = itemView.findViewById(R.id.fieldName)
        private val buttonView: View = itemView.findViewById(R.id.selectfield_select)
        lateinit var field: Field


        init {
            buttonView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            listener.recyclerViewListClicked(v, this.layoutPosition)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)

        // Inflate the custom layout
        val contactView = inflater.inflate(R.layout.item_field_select, parent, false)

        // Return a new holder instance
        return ViewHolder(contactView, listener)
    }

    override fun getItemCount(): Int {
        return ApplicationModel.fields.size
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val field = ApplicationModel.fields[position]
        viewHolder.textView.text = field.name
        viewHolder.field = field

    }
}