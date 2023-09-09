package com.ludycom.arcgis_maps.entities

import com.ludycom.arcgis_maps.utils.AGMLBasemapStyleEnum

data class AGMLParams(
    val apiKey: String,
    val initViewPoint: AGMLViewPoint?,
    val basemapStyle: AGMLBasemapStyleEnum
)
