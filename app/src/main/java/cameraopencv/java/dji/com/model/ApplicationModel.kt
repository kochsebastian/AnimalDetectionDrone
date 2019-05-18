package com.dji.importSDKDemo.model

import cameraopencv.java.dji.com.model.StatisticEntry
import kotlin.collections.ArrayList

object ApplicationModel {

    lateinit var fields: MutableList<Field>
    lateinit var statistics: MutableList<StatisticEntry>


    fun load() {
        // TODO
        fields = ArrayList()
        fields.add(Field("test", arrayListOf()))

        statistics = ArrayList()
    }
}