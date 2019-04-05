package com.dji.importSDKDemo.model

import com.google.android.gms.maps.model.LatLng
import java.security.InvalidParameterException

data class Field(val name: String, val polygon: List<LatLng>) {

    init {
        if (polygon.size < 3 && false) {
            throw InvalidParameterException("Invalid polygon: Contains only ${polygon.size} points," +
                    "but requires at least 3.")
        }
    }
}