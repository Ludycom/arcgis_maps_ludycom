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
import com.arcgismaps.data.FeatureQueryResult
import com.arcgismaps.data.GeoPackage
import com.arcgismaps.data.Geodatabase
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.data.ShapefileFeatureTable
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryBuilder
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Multipoint
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.mapping.MobileMapPackage
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditor
import com.arcgismaps.portal.Portal
import com.google.gson.Gson
import com.ludycom.arcgis_maps.entities.agml.AGMLArcGISOnlinePortalItem
import com.ludycom.arcgis_maps.entities.agml.AGMLFeatureServiceLayer
import com.ludycom.arcgis_maps.entities.agml.AGMLGeodatabase
import com.ludycom.arcgis_maps.entities.agml.AGMLLocalFeatureLayer
import com.ludycom.arcgis_maps.entities.agml.AGMLMobileMapPackage
import com.ludycom.arcgis_maps.entities.agml.AGMLPortalItem
import com.ludycom.arcgis_maps.entities.agml.AGMLSelectedLayerArguments
import com.ludycom.arcgis_maps.entities.agml.AGMLViewPoint
import com.ludycom.arcgis_maps.utils.AGMLGeometryTypeEnum
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.File
import kotlin.time.Duration.Companion.milliseconds


class AGMLViewMethodCall(
    private val context: Context,
    messenger: BinaryMessenger,
    channel: String,
    private val lifecycle: Lifecycle,
    private val mapView: MapView,
    private val graphicsOverlay: GraphicsOverlay,
    private val geometryEditor: GeometryEditor
) {
    private val methodChannel = MethodChannel(messenger, channel)

    private val pointSymbol: SimpleMarkerSymbol by lazy {
        SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle,
            Color(ContextCompat.getColor(context, R.color.point_symbol_color)),
            20f)
    }
    private val lineSymbol: SimpleLineSymbol by lazy {
        SimpleLineSymbol(
            SimpleLineSymbolStyle.Solid,
            Color(ContextCompat.getColor(context, R.color.line_symbol_color)),
            4f
        )
    }
    private val fillSymbol: SimpleFillSymbol by lazy {
        SimpleFillSymbol(
            SimpleFillSymbolStyle.Cross,
            Color(ContextCompat.getColor(context, R.color.fill_symbol_color)),
            lineSymbol
        )
    }

    private suspend fun getSelectedFeatureLayer(featureLayer: FeatureLayer, screenCoordinate: ScreenCoordinate, maxResults: Int) {
        featureLayer.clearSelection()

        val tolerance = 25.0
        val identifyLayerResult = mapView.identifyLayer(featureLayer, screenCoordinate, tolerance, false, maxResults).onFailure {
            Log.e("identifyLayerResult", "Select feature failed: " + it.message)
        }

        identifyLayerResult.onSuccess { identifyLayerResult ->
            val features = identifyLayerResult.geoElements.filterIsInstance<Feature>()
            featureLayer.selectFeatures(features)

            val jsonSelectedLayers = mutableListOf<String>()
            val gson = Gson()

            features.forEach { feature ->
                val attributes = feature.attributes
                val attributeMap = mutableMapOf<String, Any?>()

                attributes.keys.forEach { key ->
                    attributeMap[key] = attributes[key]
                }

                val json = gson.toJson(attributeMap)

                jsonSelectedLayers.add(json)
            }

            methodChannel.invokeMethod("/getSelectedFeatureInFeatureLayer", jsonSelectedLayers)
        }

        identifyLayerResult.onFailure {
            Log.e("getSelectedFeatureLayer", "Select feature failed: " + it.message)
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
                graphicsOverlay.graphics.clear()
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
            "/getLocation4326" -> {
                if(!isStartedLocation) {
                    result.error("FAILED", "Error in isStartedLocation, may be is null", "")
                    return
                }

                lifecycle.coroutineScope.launch {
                    locationDisplay.dataSource.start().onSuccess {
                        try{
                            val position = mapView.locationDisplay.location.value!!.position;
                            val location = AGMLViewPoint(
                                longitude = position.x,
                                latitude = position.y,
                                scale = position.z!!
                            )
                            result.success(Gson().toJson(location))
                        }catch(e: Exception){
                            result.error("FAILED", "catched Error in /getLocation: $e", "locationDisplay.dataSource.start()")
                        }
                    }.onFailure {
                        result.error("FAILED", "onFailure Error in /getLocation", "locationDisplay.dataSource.start()")
                    }
                }
            }
            "/getLocation9377" -> {
                if(!isStartedLocation) {
                    result.error("FAILED", "Error in isStartedLocation, may be is null", "")
                    return
                }

                lifecycle.coroutineScope.launch {
                    locationDisplay.dataSource.start().onSuccess {
                        try{

                            val position = mapView.locationDisplay.location.value!!.position;

                            val point = GeometryEngine.projectOrNull(
                                Point(position.x, position.y, SpatialReference(4326)),
                                SpatialReference(9377)
                            ) as Point

                            val location = AGMLViewPoint(
                                longitude = point.x,
                                latitude = point.y,
                                scale = 3000.0
                            )

                            result.success(Gson().toJson(location))
                        }catch(e: Exception){
                            result.error("FAILED", "catched Error in /getLocation: $e", "locationDisplay.dataSource.start()")
                        }
                    }.onFailure {
                        result.error("FAILED", "onFailure Error in /getLocation", "locationDisplay.dataSource.start()")
                    }
                }
            }
            "/setPoint4326" -> {
                val arguments = call.arguments as Map<*, *>

                // Extraer valores directamente del mapa
                val latitude = (arguments["latitude"] as? Number)?.toDouble() ?: 0.0
                val longitude = (arguments["longitude"] as? Number)?.toDouble() ?: 0.0

                // Extraer los componentes de color
                val red = (arguments["colorR"] as? Number)?.toInt() ?: 0
                val green = (arguments["colorG"] as? Number)?.toInt() ?: 255
                val blue = (arguments["colorB"] as? Number)?.toInt() ?: 255
                val alpha = (arguments["colorA"] as? Number)?.toInt() ?: 255

                val point = Point(longitude, latitude, SpatialReference(4326))

                try {
                    val markerColor = Color.fromRgba(red, green, blue, alpha)
                    val simpleMarkerSymbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, markerColor, 20f)
                    val blueOutlineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.fromRgba(5, 66, 96), 3f)
                    simpleMarkerSymbol.outline = blueOutlineSymbol

                    val pointGraphic = Graphic(point, simpleMarkerSymbol)

                    graphicsOverlay.graphics.add(pointGraphic)
                } catch (e: Exception) {
                    println(e)
                }
            }
            "/setPoint9377" -> {
                val arguments = call.arguments as Map<*, *>

                // Extraer valores directamente del mapa
                val latitude = (arguments["latitude"] as? Number)?.toDouble() ?: 0.0
                val longitude = (arguments["longitude"] as? Number)?.toDouble() ?: 0.0

                // Extraer los componentes de color
                val red = (arguments["colorR"] as? Number)?.toInt() ?: 0
                val green = (arguments["colorG"] as? Number)?.toInt() ?: 255
                val blue = (arguments["colorB"] as? Number)?.toInt() ?: 255
                val alpha = (arguments["colorA"] as? Number)?.toInt() ?: 255

                val point = Point(longitude, latitude, SpatialReference(9377))

                try {
                    val markerColor = Color.fromRgba(red, green, blue, alpha)
                    val simpleMarkerSymbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, markerColor, 20f)
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
                var arguments: Map<*, *>? = null

                if(call.arguments != null) {
                    arguments = call.arguments as Map<*, *>
                }

                val attributes = HashMap<String, Any?>()

                if(arguments != null) {
                    for (argument in arguments) {
                        attributes[argument.key.toString()] = argument.value
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
                            val simpleMarkerSymbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, Color.cyan, 20f)
                            val blueOutlineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.fromRgba(5, 66, 96), 3f)
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
            "/queryData" -> {
                val queryString = call.arguments as String

                val mapLayers = mapView.map?.operationalLayers;
                if(mapLayers.isNullOrEmpty()) return result.error("FAILED", "Error in /queryData", "operationalLayers is empty");

                val featureLayer = mapLayers.last() as FeatureLayer

                featureLayer.clearSelection()
                val queryParameters = QueryParameters().apply {
                    whereClause = queryString
                }

                lifecycle.coroutineScope.launch {
                    val featureQueryResult = featureLayer.featureTable?.queryFeatures(queryParameters)?.getOrElse {
                        result.error("FAILED", "Error in /queryData", "featureLayer.featureTable?.queryFeatures(queryParameters).getOrElse")
                    } as FeatureQueryResult?

                    val feature = featureQueryResult?.firstOrNull()
                    if (feature != null) {
                        featureLayer.selectFeature(feature)
                        val envelope = feature.geometry?.extent
                            ?: return@launch result.error("FAILED", "Error in /queryData", "Error retrieving geometry extent")

                        mapView.setViewpoint(Viewpoint(envelope))
                        result.success(Gson().toJson(feature.attributes))
                    } else {
                        result.success("No data")
                    }
                }
            }
            "/startEditing" -> {
                val editType = Gson().fromJson(call.arguments.toString(), AGMLGeometryTypeEnum::class.java)
                geometryEditor.start(editType.getValue())
            }
            "/completeEditing" -> {
                val spatialReferenceCode = call.arguments as Int

                val geometry = geometryEditor.geometry.value
                if(geometry == null) {
                    result.error("FAILED", "Error in /completeEditing", "Geometry is null")
                    return
                }

                val isValid = GeometryBuilder.builder(geometry).isSketchValid
                if(!isValid) {
                    result.error("FAILED", "Error in /completeEditing", "Geometry is not valid")
                    return
                }

                val geometryType = when (geometry) {
                    is Polygon -> AGMLGeometryTypeEnum.POLYGON
                    is Polyline -> AGMLGeometryTypeEnum.POLYLINE
                    is Point -> AGMLGeometryTypeEnum.POINT
                    is Multipoint -> AGMLGeometryTypeEnum.MULTIPOINT
                    else -> null
                }

                val projectedGeometry = GeometryEngine.projectOrNull(geometry, SpatialReference(spatialReferenceCode))
                val dataResult = mutableMapOf(
                    "DATA" to (projectedGeometry?.toJson() ?: ""),
                    "GEOMETRY_TYPE" to geometryType.toString()
                )

                result.success(dataResult)
                geometryEditor.stop()
            }
            "/cancelEditing" -> {
                geometryEditor.stop()
            }
            "/addGeometry" -> {
                val arguments = call.arguments as Map<*, *>
                val graphic = graphicFromParams(arguments, result)
                if(graphic == null) {
                    result.error("FAILED", "Error in /addGeometry", "graphic is null")
                    return
                }
                graphicsOverlay.graphics.add(graphic)
            }
            "/removeGeometry" -> {
                val arguments = call.arguments as Map<*, *>
                val graphic = graphicFromParams(arguments, result)
                if(graphic == null) {
                    result.error("FAILED", "Error in /removeGeometry", "graphic is null")
                    return
                }

                val graphicToRemove = graphicsOverlay.graphics.find { existingGraphic ->
                    areGraphicsEqual(existingGraphic, graphic)
                }

                graphicsOverlay.graphics.remove(graphicToRemove)
            }
            "/removeAllGeometries" -> {
                graphicsOverlay.graphics.clear()
            }
            "/undoGeometry" -> {
                geometryEditor.undo()
            }
            "/redoGeometry" -> {
                geometryEditor.redo()
            }
            "/deleteSelectedGeometryElement" -> {
                geometryEditor.deleteSelectedElement()
            }
            else -> result.notImplemented()
        }
    }

    private fun graphicFromParams(params: Map<*, *>, result: MethodChannel.Result): Graphic? {
        val geometryString = JSONObject(params).toString()
        val geometry = Geometry.fromJsonOrNull(geometryString)

        if(geometry == null) {
            result.error("FAILED", "Error in /addGeometry or /removeGeometry", "Geometry is null")
            return null
        }

        return Graphic(geometry).apply {
            symbol = when (geometry) {
                is Polygon -> fillSymbol
                is Polyline -> lineSymbol
                is Point, is Multipoint -> pointSymbol
                else -> null
            }
        }
    }

    private fun areGraphicsEqual(existingGraphic: Graphic, graphic: Graphic): Boolean {
        val existingGeometry = existingGraphic.geometry
        val geometry = graphic.geometry

        if (existingGeometry != geometry) return false

        return existingGraphic.attributes == graphic.attributes
    }
}