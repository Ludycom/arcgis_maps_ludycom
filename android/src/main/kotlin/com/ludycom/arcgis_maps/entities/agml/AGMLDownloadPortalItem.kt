package com.ludycom.arcgis_maps.entities.agml

import com.ludycom.arcgis_maps.utils.AGMLDownloadStatusEnum

data class AGMLDownloadPortalItem (
    var portalItem: AGMLPortalItem,
    var downloadStatus: AGMLDownloadStatusEnum = AGMLDownloadStatusEnum.NONE,
    var pathLocation: String
)