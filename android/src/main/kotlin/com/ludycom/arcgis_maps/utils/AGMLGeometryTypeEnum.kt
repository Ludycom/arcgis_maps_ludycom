package com.ludycom.arcgis_maps.utils

import com.arcgismaps.geometry.GeometryType

enum class AGMLGeometryTypeEnum {
    POINT {
        override fun getValue() : GeometryType {
            return GeometryType.Point
        }
    },
    POLYLINE {
        override fun getValue() : GeometryType {
            return GeometryType.Polyline
        }
    },
    POLYGON {
        override fun getValue() : GeometryType {
            return GeometryType.Polygon
        }
    },
    MULTIPOINT {
        override fun getValue() : GeometryType {
            return GeometryType.Multipoint
        }
    };

    abstract fun getValue(): GeometryType
}