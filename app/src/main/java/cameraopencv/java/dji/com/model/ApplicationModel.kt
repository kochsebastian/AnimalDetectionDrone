package com.dji.importSDKDemo.model

import java.util.*

object ApplicationModel {

    lateinit var fields: MutableList<Field>


    fun load() {
        // TODO
        fields = ArrayList()
        fields.add(Field("test", arrayListOf()))
    }
}