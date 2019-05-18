package com.dji.importSDKDemo.model

import cameraopencv.java.dji.com.model.StatisticEntry
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.heatmaps.WeightedLatLng
import kotlin.collections.ArrayList

object ApplicationModel {

    lateinit var fields: MutableList<Field>
    lateinit var statistics: MutableList<StatisticEntry>


    fun load() {
        // TODO
        fields = ArrayList()
        fields.add(Field("test", arrayListOf()))

        statistics = ArrayList()
        statistics.add(StatisticEntry("test", System.currentTimeMillis(), arrayListOf(WeightedLatLng(
            LatLng(
                51.055705,
                13.510207
            )))))
    }
}