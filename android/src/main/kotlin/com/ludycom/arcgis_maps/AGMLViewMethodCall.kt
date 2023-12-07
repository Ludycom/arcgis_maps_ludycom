@file:OptIn(FlowPreview::class)

package com.ludycom.arcgis_maps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.arcgismaps.Color
import com.arcgismaps.data.Feature
import com.arcgismaps.data.GeoPackage
import com.arcgismaps.data.Geodatabase
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.data.ShapefileFeatureTable
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.mapping.MobileMapPackage
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.portal.Portal
import com.google.gson.Gson
import com.ludycom.arcgis_maps.entities.agml.AGMLArcGISOnlinePortalItem
import com.ludycom.arcgis_maps.entities.agml.AGMLLocalFeatureLayer
import com.ludycom.arcgis_maps.entities.agml.AGMLPortalItem
import com.ludycom.arcgis_maps.entities.agml.AGMLFeatureServiceLayer
import com.ludycom.arcgis_maps.entities.agml.AGMLGeodatabase
import com.ludycom.arcgis_maps.entities.agml.AGMLMobileMapPackage
import com.ludycom.arcgis_maps.entities.agml.AGMLSelectedLayerArguments
import com.ludycom.arcgis_maps.entities.agml.AGMLViewPoint
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.withTimeoutOrNull
import java.lang.Exception


class AGMLViewMethodCall(
    private val context: Context,
    messenger: BinaryMessenger,
    channel: String,
    private val lifecycle: Lifecycle,
    private val mapView: MapView,
    private val graphicsOverlay: GraphicsOverlay
) {

    private val methodChannel = MethodChannel(messenger, channel)


    private suspend fun getSelectedFeatureLayer(featureLayer: FeatureLayer, screenCoordinate: ScreenCoordinate, maxResults: Int) {
        featureLayer.clearSelection()

        val tolerance = 25.0
        val identifyLayerResult = mapView.identifyLayer(featureLayer, screenCoordinate, tolerance, false, maxResults).onFailure {
            Log.e("identifyLayerResult", "Select feature failed: " + it.message)
        }

        identifyLayerResult.apply {
            onSuccess { identifyLayerResult ->
                val features = identifyLayerResult.geoElements.filterIsInstance<Feature>()
                featureLayer.selectFeatures(features)

                val jsonSelectedLayers = mutableListOf<String>()
                val gson = Gson()
                features.forEach { item ->
                    jsonSelectedLayers.add(gson.toJson(item.attributes))
//                    if(item.geometry != null) {
//                        mapView.setViewpoint(
//                            Viewpoint(item.geometry!!.extent)
//                        )
//                    }
                    if(features.size == jsonSelectedLayers.size) {
                        methodChannel.invokeMethod("/getSelectedFeatureInFeatureLayer", jsonSelectedLayers)
                    }
                }
            }
            onFailure {
                Log.e("getSelectedFeatureLayer", "Select feature failed: " + it.message)
            }
        }
    }

    private fun getTapEventCoordinate(tapEvent: SingleTapConfirmedEvent): ScreenCoordinate {
        return tapEvent.screenCoordinate
    }

    private fun setOnSingleTapConfirmedListener(layer: FeatureLayer, maxResults: Int) {
        mapView.apply {
            lifecycle.coroutineScope.launch {
                onSingleTapConfirmed.flatMapConcat { tapEvent -> flow {
                    emit(getTapEventCoordinate(tapEvent))
                }}.collect { coordinate ->
                    getSelectedFeatureLayer(layer, coordinate, maxResults)
                }
            }

            if(layer.item?.extent?.center != null) {
                setViewpoint(
                    Viewpoint(layer.item?.extent?.center!!)
                )
            }

//            if(layer.fullExtent != null) {
//                mapView.setViewpoint(
//                    Viewpoint(layer.fullExtent!!.center)
//                )
//            }
        }
    }

    private fun setFeatureLayer(layer: FeatureLayer, viewPoint: AGMLViewPoint?) {
        mapView.apply {
            map?.operationalLayers?.add(layer)

            if (viewPoint != null) {
                val newViewpoint = Viewpoint(
                    viewPoint.latitude,
                    viewPoint.longitude,
                    viewPoint.scale
                )
                mapView.setViewpoint(newViewpoint)
            } else {
                if(layer.fullExtent != null) {
                    mapView.setViewpoint(
                        Viewpoint(layer.fullExtent!!.center)
                    )
                }
            }
        }
    }


    private val locationDisplay = mapView.locationDisplay
    private var isStartedLocation = false

    private val permissionErrorCode = "-1"
    private val permissionErrorMessage = "Permissions Denied"
    private val permissionErrorDetail = "The Manifest.permission.ACCESS_COARSE_LOCATION and Manifest.permission.ACCESS_FINE_LOCATION permissions are required."

    private fun checkHavePermissions(): Boolean {

        val permissionCheckCoarseLocation =
            ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val permissionCheckFineLocation =
            ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        return permissionCheckCoarseLocation && permissionCheckFineLocation;
    }


    fun onMethodCall(
        call: MethodCall,
        result: MethodChannel.Result,
    ) {
        ///Feature layers manage
        when (call.method) {
            "/loadServiceFeatureTable" -> {
                val arguments = call.arguments as Map<*, *>
                val arcGISMapFeatureServiceTable = Gson().fromJson(JSONObject(arguments).toString(), AGMLFeatureServiceLayer::class.java)

                val serviceFeatureTable = ServiceFeatureTable(arcGISMapFeatureServiceTable.url)

                lifecycle.coroutineScope.launch {
                    withTimeoutOrNull(5000.milliseconds) {
                        serviceFeatureTable.load().onSuccess {
                            val featureLayer = FeatureLayer.createWithFeatureTable(serviceFeatureTable)
                            setFeatureLayer(featureLayer, arcGISMapFeatureServiceTable.viewPoint)
                            result.success(featureLayer.id)
                        }.onFailure {
                            result.success("failure");
                        }
                    }
                }
            }
            "/loadPortalItemFeatureLayer" -> {
                val arguments = call.arguments as Map<*, *>
                val arcGISMapServicePortalItem = Gson().fromJson(JSONObject(arguments).toString(), AGMLPortalItem::class.java)

                val portalItem = PortalItem(arcGISMapServicePortalItem.url)

                lifecycle.coroutineScope.launch {
                    withTimeoutOrNull(5000.milliseconds) {
                        portalItem.load().onSuccess {
                            val featureLayer = FeatureLayer.createWithItem(portalItem)
                            setFeatureLayer(featureLayer, arcGISMapServicePortalItem.viewPoint)
                            result.success(featureLayer.id)
                        }.onFailure {
                            result.success("failure");
                        }
                    }
                }
            }
            "/loadArcGISOnlinePortalItemFeatureLayer" -> {
                val arguments = call.arguments as Map<*, *>
                val arcGISMapServicePortalItem = Gson().fromJson(JSONObject(arguments).toString(), AGMLArcGISOnlinePortalItem::class.java)

                val portal = Portal("https://www.arcgis.com")
                val portalItem = PortalItem(portal, arcGISMapServicePortalItem.itemID)

                lifecycle.coroutineScope.launch {
                    withTimeoutOrNull(5000.milliseconds) {
                        portalItem.load().onSuccess {
                            val featureLayer = FeatureLayer.createWithItem(portalItem)
                            setFeatureLayer(featureLayer, arcGISMapServicePortalItem.viewPoint)
                            result.success(featureLayer.id)
                        }.onFailure {
                            result.success("failure");
                        }
                    }
                }
            }
            "/loadGeoDatabaseFeatureLayer" -> {
                val arguments = call.arguments as Map<*, *>
                val agmlGeodatabase = Gson().fromJson(JSONObject(arguments).toString(), AGMLGeodatabase::class.java)

                val geoDatabaseFile = File(agmlGeodatabase.path!!)
                val geoDatabase = Geodatabase(geoDatabaseFile.path)

                lifecycle.coroutineScope.launch {
                    withTimeoutOrNull(5000.milliseconds) {
                        geoDatabase.load().onSuccess {
                            val featureTables = geoDatabase.featureTables
                            val geoDatabaseFeatureTable = geoDatabase.getFeatureTable(featureTables.first().tableName) //todo: Implementar un for
                            val featureLayer = FeatureLayer.createWithFeatureTable(geoDatabaseFeatureTable!!)
                            setFeatureLayer(featureLayer, agmlGeodatabase.viewPoint)
                            result.success(featureLayer.id)
                        }.onFailure {
                            result.success("failure");
                        }
                    }
                }
            }
            "/loadMobileMapPackage" -> {
                val arguments = call.arguments as Map<*, *>
                val agmlMobileMapPackage = Gson().fromJson(JSONObject(arguments).toString(), AGMLMobileMapPackage::class.java)

                val mapPackage = MobileMapPackage(agmlMobileMapPackage.path!!)

                lifecycle.coroutineScope.launch {
                    withTimeoutOrNull(5000.milliseconds) {
                        mapPackage.load().onSuccess {
                            mapView.map = mapPackage.maps.first()
                            result.success("basemap")
                        }.onFailure {
                            result.success("failure");
                        }
                    }
                }
            }
            "/loadSyncGeodatabase" -> {
                val arguments = call.arguments as Map<*, *>
                val agmlGeodatabase = Gson().fromJson(JSONObject(arguments).toString(), AGMLGeodatabase::class.java)

                val geoDatabaseFile = File(agmlGeodatabase.path!!)
                val geoDatabase = Geodatabase(geoDatabaseFile.path)

                mapView.map?.operationalLayers?.clear();

                lifecycle.coroutineScope.launch {
                    geoDatabase.load().onFailure {
                        result.error("LOAD_ERROR", "Failed in geodatabase load", "loadSyncGeodatabase failed")
                    }

                    mapView.map!!.operationalLayers += geoDatabase.featureTables.map { featureTable ->
                        FeatureLayer.createWithFeatureTable(featureTable)
                    }
                }
            }
            "/loadGeoPackageFeatureLayer" -> {
                val arguments = call.arguments as Map<*, *>
                val arcGISMapLocalPortalItem = Gson().fromJson(JSONObject(arguments).toString(), AGMLLocalFeatureLayer::class.java)

                val geoPackageFile = File(arcGISMapLocalPortalItem.path)
                val geoPackage = GeoPackage(geoPackageFile.path)

                lifecycle.coroutineScope.launch {
                    withTimeoutOrNull(5000.milliseconds) {
                        geoPackage.load().onSuccess {
                            val geoPackageFeatureTable = geoPackage.geoPackageFeatureTables.first() //todo: Implementar un for
                            val featureLayer = FeatureLayer.createWithFeatureTable(geoPackageFeatureTable)
                            setFeatureLayer(featureLayer, arcGISMapLocalPortalItem.viewPoint)
                            result.success(featureLayer.id)
                        }.onFailure {
                            result.success("failure");
                        }
                    }
                }
            }
            "/loadShapefileFeatureLayer" -> {
                val arguments = call.arguments as Map<*, *>
                val arcGISMapLocalPortalItem = Gson().fromJson(JSONObject(arguments).toString(), AGMLLocalFeatureLayer::class.java)

                val file = File(arcGISMapLocalPortalItem.path)
                val shapeFileTable = ShapefileFeatureTable(file.path)

                lifecycle.coroutineScope.launch {
                    withTimeoutOrNull(5000.milliseconds) {
                        shapeFileTable.load().onSuccess {
                            val featureLayer = FeatureLayer.createWithFeatureTable(shapeFileTable)
                            setFeatureLayer(featureLayer, arcGISMapLocalPortalItem.viewPoint)
                            result.success(featureLayer.id)
                        }.onFailure {
                            result.success("failure");
                        }
                    }
                }
            }
            "/removeAllFeatureLayers" -> {
                mapView.map?.operationalLayers?.clear()
                result.success("success");
            }
            "/removeFeatureLayer" -> {
                val layerId = call.arguments as String
                val removeResult = mapView.map?.operationalLayers?.removeIf { layerId == it.id }

                if(removeResult != null && removeResult) {
                    result.success("success");
                } else {
                    result.success("failure");
                }
            }
            "/setSelectedFeatureLayer" -> {
                val arguments = call.arguments as Map<*, *>
                val agmlSelectedLayerArguments = Gson().fromJson(JSONObject(arguments).toString(), AGMLSelectedLayerArguments::class.java)

                val featureLayer = mapView.map?.operationalLayers?. find { agmlSelectedLayerArguments.layerId == it.id }

                if(featureLayer != null) {
                    setOnSingleTapConfirmedListener(featureLayer as FeatureLayer, agmlSelectedLayerArguments.maxResults)
                    result.success("success");
                } else {
                    result.success("failure");
                }
            }

            ///Map manage
            "/zoomIn" -> {
                mapView.apply {
                    val mapScale = mapScale.value
                    lifecycle.coroutineScope.launch() {
                        setViewpointScale(mapScale*0.5)
                    }
                }
            }
            "/zoomOut" -> {
                mapView.apply {
                    val mapScale = mapScale.value
                    lifecycle.coroutineScope.launch() {
                        setViewpointScale(mapScale*2.0)
                    }
                }
            }
            "/setViewPoint" -> {
                val arguments = call.arguments as Map<*, *>
                val aGMLViewPoint = Gson().fromJson(JSONObject(arguments).toString(), AGMLViewPoint::class.java)
                mapView.apply {
                    setViewpoint(
                        Viewpoint(
                            aGMLViewPoint.latitude,
                            aGMLViewPoint.longitude,
                            aGMLViewPoint.scale
                        )
                    )
                }
            }
            "/setViewPoint4326" -> {
                val arguments = call.arguments as Map<*, *>
                val aGMLViewPoint = Gson().fromJson(JSONObject(arguments).toString(), AGMLViewPoint::class.java)

                val point = Point(aGMLViewPoint.longitude, aGMLViewPoint.latitude, SpatialReference(4326))

                mapView.apply {
                    setViewpoint(
                        Viewpoint(
                            point.y,
                            point.x,
                            aGMLViewPoint.scale
                        )
                    )
                }
            }
            "/startLocation" -> {
                if(checkHavePermissions()) {
                    lifecycle.coroutineScope.launch {
                        locationDisplay.dataSource.start().onSuccess {
                            isStartedLocation = true
                        }
                    }
                } else {
                    result.error(
                        permissionErrorCode,
                        permissionErrorMessage,
                        permissionErrorDetail
                    )
                }
            }
            "/stopLocation" -> {
                lifecycle.coroutineScope.launch {
                    locationDisplay.dataSource.stop()
                }
            }
            "/autoPaneModeCenterLocation" -> {
                if(checkHavePermissions()) {
                    lifecycle.coroutineScope.launch {
                        locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
                    }
                } else {
                    result.error(
                        permissionErrorCode,
                        permissionErrorMessage,
                        permissionErrorDetail
                    )
                }
            }
            "/autoPaneModeInitNavigationMode" -> {
                if(checkHavePermissions()) {
                    locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Navigation)
                } else {
                    result.error(
                        permissionErrorCode,
                        permissionErrorMessage,
                        permissionErrorDetail
                    )
                }
            }
            "/autoPaneModeCompassNavigation" -> {
                if(checkHavePermissions()) {
                    locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.CompassNavigation)
                } else {
                    result.error(
                        permissionErrorCode,
                        permissionErrorMessage,
                        permissionErrorDetail
                    )
                }
            }
            "/getLocation" -> {
                if(!isStartedLocation) {
                  result.error("FAILED", "Error in isStartedLocation, may be is null", "")
                  return
                }

                lifecycle.coroutineScope.launch {
                    locationDisplay.dataSource.start().onSuccess {
                        val position = mapView.locationDisplay.location.value!!.position;

                        val location = AGMLViewPoint(
                            longitude = position.y,
                            latitude = position.x,
                            scale = position.z!!
                        )

                        result.success(Gson().toJson(location))
                    }.onFailure {
                        result.error("FAILED", "Error in /getLocation", "locationDisplay.dataSource.start()")
                    }
                }
            }
            "/setPoint4326" -> {
                val arguments = call.arguments as Map<*, *>
                val aGMLViewPoint = Gson().fromJson(JSONObject(arguments).toString(), AGMLViewPoint::class.java)

                val point = GeometryEngine.projectOrNull(
                    Point(aGMLViewPoint.longitude, aGMLViewPoint.latitude, SpatialReference(4326)),
                    SpatialReference(9377)
                ) as Point

                try {
                    val simpleMarkerSymbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, Color.cyan, 20f)
                    val blueOutlineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.fromRgba(5, 66, 96), 3f)
                    simpleMarkerSymbol.outline = blueOutlineSymbol

                    val pointGraphic = Graphic(point, simpleMarkerSymbol)

                    graphicsOverlay.graphics.add(pointGraphic)
                } catch (e: Exception) {
                    println(e)
                }
            }
            "/getLocation9377AndSetPoint" -> {
                if(!isStartedLocation) {
                  result.error("FAILED", "Error in isStartedLocation, may be is null", "")
                  return
                }

                lifecycle.coroutineScope.launch {
                    locationDisplay.dataSource.start().onSuccess {
                        val position = mapView.locationDisplay.location.value!!.position;

                        val point = GeometryEngine.projectOrNull(position, SpatialReference(9377)) as Point

                        val z = if(position.z != null) {
                            point.z
                        } else {
                            1000.0
                        }

                        val location = AGMLViewPoint(
                            longitude = point.y,
                            latitude = point.x,
                            scale = z!!
                        )

                        try {
                            val simpleMarkerSymbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, Color.red, 20f)

                            val blueOutlineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.fromRgba(0, 0, 0), 3f)
                            simpleMarkerSymbol.outline = blueOutlineSymbol

                            val pointGraphic = Graphic(point, simpleMarkerSymbol)

                            graphicsOverlay.graphics.add(pointGraphic)
                        } catch (e: Exception) {
                            println(e)
                        }

                        result.success(Gson().toJson(location))
                    }.onFailure {
                        result.error("FAILED", "Error in /getLocation", "locationDisplay.dataSource.start()")
                    }
                }
            }
            "/setPointCurrentLocation" -> {
                val arguments = call.arguments as Map<*, *>

                val attributes = HashMap<String, Any?>()
                for ((key, value) in arguments) {
                    if (key is String && value != null) {
                        attributes[key] = value
                    }
                }

                if(!isStartedLocation) {
                  result.error("FAILED", "Error in isStartedLocation, may be is null", "")
                  return
                }

                lifecycle.coroutineScope.launch {
                    locationDisplay.dataSource.start().onSuccess {
                        val position = mapView.locationDisplay.location.value!!.position;
                        val point = GeometryEngine.projectOrNull(position, SpatialReference(9377)) as Point

                        try {
                            val simpleMarkerSymbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, Color.black, 20f)

                            val blueOutlineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.fromRgba(0, 0, 0), 3f)
                            simpleMarkerSymbol.outline = blueOutlineSymbol

                            val pointGraphic = Graphic(point, simpleMarkerSymbol)
                            pointGraphic.attributes.putAll(attributes);

                            graphicsOverlay.graphics.add(pointGraphic)
                        } catch (e: Exception) {
                            println(e)
                        }
                    }.onFailure {
                        result.error("FAILED", "Error in /getLocation", "locationDisplay.dataSource.start()")
                    }
                }
            }

            else -> result.notImplemented()
        }
    }

}


