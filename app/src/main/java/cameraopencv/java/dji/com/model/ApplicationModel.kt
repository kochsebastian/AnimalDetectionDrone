package com.dji.importSDKDemo.model

import android.content.Context
import android.util.Log
import cameraopencv.java.dji.com.FPVDemoApplication
import cameraopencv.java.dji.com.model.StatisticEntry
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.heatmaps.WeightedLatLng
import kotlin.collections.ArrayList

import cameraopencv.java.dji.com.utils.FileUtils
import cameraopencv.java.dji.com.utils.GeneralUtils
import com.google.maps.android.geometry.Point
import org.json.JSONArray
import org.json.JSONObject


object ApplicationModel {


    const val FILE_PATH = "save.json"

    var fields: MutableList<Field> = ArrayList()
    var statistics: MutableList<StatisticEntry> = ArrayList()

    var statisticEntrySelected: StatisticEntry? = null


    fun load() {
        fields.clear()
        statistics.clear()

        val jsonString = FileUtils.readFromFile(FPVDemoApplication.getInstance(), FILE_PATH)

        if (!jsonString.isEmpty()) {
            val obj = JSONObject(jsonString)

            // Fields
            if (obj.has("fields")) {
                val jsonFields = obj.getJSONArray("fields")
                for (i in 0 until jsonFields.length()) {
                    val jsonField = jsonFields.getJSONObject(i)
                    val name = jsonField.getString("name")
                    val jsonPolygon = jsonField.getJSONArray("polygon")
                    val polygon = mutableListOf<LatLng>()
                    for (i in 0 until jsonPolygon.length()) {
                        val jsonPoint = jsonPolygon.getJSONObject(i)
                        val lat = jsonPoint.getDouble("lat")
                        val lng = jsonPoint.getDouble("lng")
                        polygon.add(LatLng(lat, lng))
                    }
                    val field = Field(name, polygon)
                    fields.add(field)
                }
            }

            // Statistics
            if (obj.has("statistics")) {
                val jsonStatistics = obj.getJSONArray("statistics")
                for (i in 0 until jsonStatistics.length()) {
                    val jsonStatisticEntry = jsonStatistics.getJSONObject(i)
                    val name = jsonStatisticEntry.getString("name")
                    val time = jsonStatisticEntry.getLong("time")
                    val jsonDetections = jsonStatisticEntry.getJSONArray("detections")
                    val detections = mutableListOf<WeightedLatLng>()
                    for (i in 0 until jsonDetections.length()) {
                        val jsonDetection = jsonDetections.getJSONObject(i)
                        val x = jsonDetection.getDouble("x")
                        val y = jsonDetection.getDouble("y")
                        val i = jsonDetection.getDouble("i")
                        detections.add(WeightedLatLng(GeneralUtils.pointToLatLng(Point(x, y)), i))
                    }
                    val statisticEntry = StatisticEntry(name, time, detections)
                    statistics.add(statisticEntry)
                }
            }
        }


        // TODO remove demo code
       /* fields.add(Field("test", arrayListOf()))
        statistics.add(StatisticEntry("test", System.currentTimeMillis(), arrayListOf(WeightedLatLng(
            LatLng(
                51.055705,
                13.510207
            )))))*/
    }

    fun save() {
        // Fields
        val jsonFields = JSONArray()
        for (field in this.fields) {
            val jsonField = JSONObject()
            jsonField.put("name", field.name)

            val jsonPolygon = JSONArray()
            field.polygon.forEach {
                val jsonPoint = JSONObject()
                jsonPoint.put("lat", it.latitude)
                jsonPoint.put("lng", it.longitude)
                jsonPolygon.put(jsonPoint)
            }

            jsonField.put("polygon", jsonPolygon)
            jsonFields.put(jsonField)
        }


        // Statistics
        val jsonStatistics = JSONArray()
        for (statisticEntry in this.statistics) {
            val jsonStatisticEntry = JSONObject()
            jsonStatisticEntry.put("name", statisticEntry.fieldName)
            jsonStatisticEntry.put("time", statisticEntry.time)

            val jsonDetections = JSONArray()
            statisticEntry.detections.forEach {
                val jsonDetection = JSONObject()
                jsonDetection.put("x", it.point.x)
                jsonDetection.put("y", it.point.y)
                jsonDetection.put("i", it.intensity)
                jsonDetections.put(jsonDetection)
            }

            jsonStatisticEntry.put("detections", jsonDetections)
            jsonStatistics.put(jsonStatisticEntry)
        }

        val obj = JSONObject()
        obj.put("fields", jsonFields)
        obj.put("statistics", jsonStatistics)

        FileUtils.writeToFile(FPVDemoApplication.getInstance(), FILE_PATH, obj.toString())
    }


}