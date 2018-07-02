package com.example.kadyan.measuredistance

import java.io.Serializable

class LatLong : Serializable {

    var latitude: Double = 0.0
    var longitude: Double = 0.0

    constructor() {}

    constructor(latitude: Double, longitude: Double) {
        this.latitude = latitude
        this.longitude = longitude
    }

}
