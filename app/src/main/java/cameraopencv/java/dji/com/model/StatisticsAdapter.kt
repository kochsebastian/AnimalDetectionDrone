package com.dji.importSDKDemo.model

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.view.LayoutInflater
import cameraopencv.java.dji.com.R
import cameraopencv.java.dji.com.RecyclerViewClickListener
import cameraopencv.java.dji.com.model.StatisticEntry
import java.util.*
import java.text.SimpleDateFormat


class StatisticsAdapter(private val listener: RecyclerViewClickListener) :
        RecyclerView.Adapter<StatisticsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View, private val listener: RecyclerViewClickListener) :
            RecyclerView.ViewHolder(itemView), View.OnClickListener {


        val textViewName: TextView = itemView.findViewById(R.id.statisticName)
        val textViewDate: TextView = itemView.findViewById(R.id.statisticsDate)
        private val buttonView: View = itemView.findViewById(R.id.statistics_select)
        lateinit var statistic: StatisticEntry


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
        val contactView = inflater.inflate(R.layout.item_statistic_entry, parent, false)

        // Return a new holder instance
        return ViewHolder(contactView, listener)
    }

    override fun getItemCount(): Int {
        return ApplicationModel.statistics.size
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val statistic = ApplicationModel.statistics[position]
        viewHolder.textViewName.text = statistic.fieldName

        val df = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.GERMAN)
        viewHolder.textViewDate.text = df.format(Date(statistic.time))
        viewHolder.statistic = statistic

    }
}