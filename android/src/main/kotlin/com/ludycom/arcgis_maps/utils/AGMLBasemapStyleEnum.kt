package com.ludycom.arcgis_maps.utils

import com.arcgismaps.mapping.BasemapStyle

enum class AGMLBasemapStyleEnum {
    NONE {
         override fun getBasemapStyle() : BasemapStyle? {
             return null
         }
    },
    ARCGIS_TOPOGRAPHIC {
        override fun getBasemapStyle() : BasemapStyle {
            return BasemapStyle.ArcGISTopographic
        }
    };

    abstract fun getBasemapStyle(): BasemapStyle?
}

