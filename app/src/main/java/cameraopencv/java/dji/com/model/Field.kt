package com.dji.importSDKDemo.model

import android.location.Location
import java.security.InvalidParameterException

data class Field(val name: String, val polygon: List<Location>) {

    init {
        if (polygon.size < 3 && false) {
            throw InvalidParameterException("Invalid polygon: Contains only ${polygon.size} points," +
                    "but requires at least 3.")
        }
    }
}