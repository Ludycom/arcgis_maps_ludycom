package com.ludycom.arcgis_maps.entities.agml

data class AGMLChangeSpacialReferenceParams (
    val point: AGMLViewPoint,
    val fromSpacialReference: Int,
    val toSpacialReference: Int
)