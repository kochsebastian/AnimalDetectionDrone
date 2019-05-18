package cameraopencv.java.dji.com.model

import com.google.maps.android.heatmaps.WeightedLatLng

data class StatisticEntry(val fieldName: String, val time: Long,
                          val detections: List<WeightedLatLng>)