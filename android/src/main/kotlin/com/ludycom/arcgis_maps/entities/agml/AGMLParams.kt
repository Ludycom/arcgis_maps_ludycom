package com.ludycom.arcgis_maps.entities.agml

import com.ludycom.arcgis_maps.utils.AGMLBasemapStyleEnum


data class AGMLParams(
    val initViewPoint: AGMLViewPoint?,
    val basemapStyle: AGMLBasemapStyleEnum = AGMLBasemapStyleEnum.NONE
)
