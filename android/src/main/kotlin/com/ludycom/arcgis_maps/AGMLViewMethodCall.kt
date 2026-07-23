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
import com.arcgismaps.data.FeatureTable
import com.arcgismaps.data.GeoPackage
import com.arcgismaps.data.Geodatabase
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.data.ShapefileFeatureTable
import com.arcgismaps.data.SpatialRelationship
import com.arcgismaps.geometry.AreaUnit
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.GeodeticCurveType
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryBuilder
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.LinearUnit
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
import com.arcgismaps.mapping.symbology.Symbol
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
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
    // Símbolo resaltado para el gráfico seleccionado (amarillo-ámbar, más grueso)
    private val lineSymbolSelected: SimpleLineSymbol by lazy {
        SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.fromRgba(229, 57, 53, 255), 6f)
    }
    private val pointSymbolSelected: SimpleMarkerSymbol by lazy {
        SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, Color.fromRgba(229, 57, 53, 255), 24f)
    }

    private var activeFeatureLayer: FeatureLayer? = null
    private var activeFeatureLayerMaxResults: Int = 1
    private var tapListenerJob: Job? = null

    init {
        startGlobalTapListener()
    }

    // Color activo para nuevas geometrías (naranja por defecto)
    private var drawColorR: Int = 0xFE
    private var drawColorG: Int = 0x87
    private var drawColorB: Int = 0x00
    private var drawColorA: Int = 0xFF

    private fun currentLineSymbol() = SimpleLineSymbol(
        SimpleLineSymbolStyle.Solid,
        Color.fromRgba(drawColorR, drawColorG, drawColorB, drawColorA),
        4f
    )
    private fun currentPointSymbol() = SimpleMarkerSymbol(
        SimpleMarkerSymbolStyle.Circle,
        Color.fromRgba(drawColorR, drawColorG, drawColorB, drawColorA),
        20f
    )
    private fun currentFillSymbol() = SimpleFillSymbol(
        SimpleFillSymbolStyle.Cross,
        Color.fromRgba(drawColorR, drawColorG, drawColorB, (drawColorA * 0.43).toInt()),
        currentLineSymbol()
    )

    private fun normalSymbolFor(graphic: Graphic): Symbol? {
        // Recuperar el color almacenado en los atributos del gráfico si existe
        val attrs = graphic.attributes
        val r = (attrs["_colorR"] as? Number)?.toInt()
        val g = (attrs["_colorG"] as? Number)?.toInt()
        val b = (attrs["_colorB"] as? Number)?.toInt()
        val a = (attrs["_colorA"] as? Number)?.toInt() ?: 0xFF
        return if (r != null && g != null && b != null) {
            val c = Color.fromRgba(r, g, b, a)
            when (graphic.geometry) {
                is Polygon -> SimpleFillSymbol(SimpleFillSymbolStyle.Cross, Color.fromRgba(r, g, b, (a * 0.43).toInt()), SimpleLineSymbol(SimpleLineSymbolStyle.Solid, c, 4f))
                is Polyline -> SimpleLineSymbol(SimpleLineSymbolStyle.Solid, c, 4f)
                is Point, is Multipoint -> SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, c, 20f)
                else -> null
            }
        } else {
            when (graphic.geometry) {
                is Polygon -> fillSymbol
                is Polyline -> lineSymbol
                is Point, is Multipoint -> pointSymbol
                else -> null
            }
        }
    }

    private fun resetAllGraphicSymbols() {
        graphicsOverlay.graphics.forEach { it.symbol = normalSymbolFor(it) }
    }

    private fun highlightGraphic(graphic: Graphic) {
        resetAllGraphicSymbols()
        graphic.symbol = when (graphic.geometry) {
            is Polyline -> lineSymbolSelected
            is Point, is Multipoint -> pointSymbolSelected
            else -> normalSymbolFor(graphic)
        }
    }

    private suspend fun identifyGraphicsOnTap(screenCoordinate: ScreenCoordinate): Boolean {
        val identifyResult = mapView.identifyGraphicsOverlay(graphicsOverlay, screenCoordinate, 22.0, false)
        var graphicFound = false
        identifyResult.onSuccess { result ->
            Log.d("identifyGraphicsOnTap", "Graphics found: ${result.graphics.size}")
            if (result.graphics.isNotEmpty()) {
                val graphic = result.graphics.first()
                activeFeatureLayer?.clearSelection()
                highlightGraphic(graphic)
                val gson = Gson()
                val attributeMap = mutableMapOf<String, Any?>()
                graphic.attributes.keys.forEach { key -> attributeMap[key] = graphic.attributes[key] }
                methodChannel.invokeMethod("/getSelectedGraphic", gson.toJson(attributeMap))
                graphicFound = true
            }
        }
        identifyResult.onFailure {
            Log.e("identifyGraphicsOnTap", "identifyGraphicsOverlay failed: ${it.message}")
        }
        return graphicFound
    }

    private suspend fun getSelectedFeatureLayer(featureLayer: FeatureLayer, screenCoordinate: ScreenCoordinate, maxResults: Int) {
        featureLayer.clearSelection()
        // Quitar el resaltado de cualquier gráfico dibujado previamente seleccionado
        resetAllGraphicSymbols()

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

                // Adjunta la geometría del feature (como JSON string) para permitir
                // editarla/clonarla desde Flutter (caso "modificado").
                feature.geometry?.let { attributeMap["_geometryJson"] = it.toJson() }

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

    private fun startGlobalTapListener() {
        tapListenerJob?.cancel()
        tapListenerJob = lifecycle.coroutineScope.launch {
            mapView.onSingleTapConfirmed.flatMapConcat { tapEvent -> flow {
                emit(getTapEventCoordinate(tapEvent))
            }}.collect { coordinate ->
                Log.d("tapListener", "Tap at $coordinate — graphics: ${graphicsOverlay.graphics.size}")
                val graphicSelected = identifyGraphicsOnTap(coordinate)
                if (!graphicSelected) {
                    val layer = activeFeatureLayer
                    if (layer != null) {
                        getSelectedFeatureLayer(layer, coordinate, activeFeatureLayerMaxResults)
                    }
                }
            }
        }
    }

    private fun setOnSingleTapConfirmedListener(layer: FeatureLayer, maxResults: Int) {
        activeFeatureLayer = layer
        activeFeatureLayerMaxResults = maxResults
        if (layer.item?.extent?.center != null) {
            mapView.setViewpoint(Viewpoint(layer.item?.extent?.center!!))
        }
    }

    private suspend fun setFeatureLayer(layer: FeatureLayer?, viewPoint: AGMLViewPoint?) {
        val map = try {
            mapView.map
        } catch (e: Exception) {
            Log.e("setFeatureLayer", "mapView.map not ready: ${e.message}")
            return
        } ?: return

        // Esperar a que el mapa esté cargado antes de añadir capas
        map.load().onFailure {
            Log.e("setFeatureLayer", "map.load() failed: ${it.message}")
            return
        }

        layer?.let {
            map.operationalLayers.add(it)

            if (viewPoint != null) {
                mapView.setViewpoint(Viewpoint(viewPoint.latitude, viewPoint.longitude, viewPoint.scale))
            } else {
                it.fullExtent?.center?.let { center ->
                    mapView.setViewpoint(Viewpoint(center))
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

                val mapLayers = mapView.map?.operationalLayers
                if (mapLayers.isNullOrEmpty()) {
                    result.error("FAILED", "operationalLayers is empty", null)
                    return
                }

                val featureLayer = mapLayers.last() as? FeatureLayer
                if (featureLayer == null) {
                    result.error("FAILED", "Last layer is not FeatureLayer", null)
                    return
                }

                val featureTable = featureLayer.featureTable
                if (featureTable == null) {
                    result.error("FAILED", "FeatureTable is null", null)
                    return
                }

                val queryParameters = QueryParameters().apply {
                    whereClause = queryString
                    maxFeatures = 1
                }

                lifecycle.coroutineScope.launch {
                    try {
                        featureLayer.clearSelection()

                        // 🔥 AQUÍ ESTÁ LA CLAVE
                        val resultQuery: Result<FeatureQueryResult> =
                            featureTable.queryFeatures(queryParameters)

                        if (resultQuery.isFailure) {
                            result.error("FAILED", "Query failed", null)
                            return@launch
                        }

                        val featureQueryResult = resultQuery.getOrNull()
                        val feature = featureQueryResult?.firstOrNull()

                        if (feature != null) {
                            featureLayer.selectFeature(feature)

                            feature.geometry?.extent?.let {
                                mapView.setViewpoint(Viewpoint(it))
                            }

                            result.success(feature.attributes)
                        } else {
                            result.success(emptyMap<String, Any>())
                        }

                    } catch (e: Exception) {
                        result.error("QUERY_ERROR", e.message, null)
                    }
                }
            }
            "/startEditing" -> {
                val args = call.arguments
                val editTypeStr: String
                if (args is Map<*, *>) {
                    editTypeStr = args["editType"]?.toString() ?: "POLYLINE"
                    drawColorR = (args["colorR"] as? Number)?.toInt() ?: 0xFE
                    drawColorG = (args["colorG"] as? Number)?.toInt() ?: 0x87
                    drawColorB = (args["colorB"] as? Number)?.toInt() ?: 0x00
                    drawColorA = (args["colorA"] as? Number)?.toInt() ?: 0xFF
                } else {
                    editTypeStr = args.toString()
                    drawColorR = 0xFE; drawColorG = 0x87; drawColorB = 0x00; drawColorA = 0xFF
                }
                val editType = Gson().fromJson("\"$editTypeStr\"", AGMLGeometryTypeEnum::class.java)
                geometryEditor.start(editType.getValue())
            }
            "/startEditingWithGeometry" -> {
                val arguments = call.arguments as Map<*, *>
                drawColorR = (arguments["colorR"] as? Number)?.toInt() ?: 0x19
                drawColorG = (arguments["colorG"] as? Number)?.toInt() ?: 0x76
                drawColorB = (arguments["colorB"] as? Number)?.toInt() ?: 0xD2
                drawColorA = (arguments["colorA"] as? Number)?.toInt() ?: 0xFF

                // Reconstruye la geometría a editar (mismo formato que /addGeometry).
                val geometryParams = arguments.toMutableMap().apply {
                    remove("colorR"); remove("colorG"); remove("colorB"); remove("colorA")
                }
                val geometryString = JSONObject(geometryParams as Map<*, *>).toString()
                val geometry = Geometry.fromJsonOrNull(geometryString)

                if (geometry == null) {
                    Log.e("startEditingWithGeometry", "Geometry is null")
                } else {
                    geometryEditor.start(geometry)
                }
            }
            "/completeEditing" -> {
                val spatialReferenceCode = call.arguments as Int
                val geometry = geometryEditor.geometry.value

                if (geometry == null) {
                    result.error("FAILED", "Error in /completeGeometry", "Geometry is null")
                    return
                }

                val isValid = GeometryBuilder.builder(geometry).isSketchValid
                if (!isValid) {
                    result.error("FAILED", "Error in /completeGeometry", "Geometry is not valid")
                    return
                }

                val geometryType = when (geometry) {
                    is Polygon -> AGMLGeometryTypeEnum.POLYGON
                    is Polyline -> AGMLGeometryTypeEnum.POLYLINE
                    is Point -> AGMLGeometryTypeEnum.POINT
                    is Multipoint -> AGMLGeometryTypeEnum.MULTIPOINT
                    else -> null
                }

                val projectedGeometry =
                    GeometryEngine.projectOrNull(geometry, SpatialReference(spatialReferenceCode))

                val gson = Gson()
                val geometryData: MutableMap<String, Any?> =
                    if (projectedGeometry != null) {
                        gson.fromJson(projectedGeometry.toJson(), MutableMap::class.java) as MutableMap<String, Any?>
                    } else {
                        mutableMapOf()
                    }

                // Cálculo de métricas
                when (geometry) {
                    is Polyline -> {
                        val length = GeometryEngine.lengthGeodetic(
                            geometry,
                            LinearUnit.meters,
                            GeodeticCurveType.Geodesic
                        )
                        geometryData["LENGTH_METERS"] = length
                    }
                    is Polygon -> {
                        val area = GeometryEngine.areaGeodetic(
                            geometry,
                            AreaUnit.squareMeters,
                            GeodeticCurveType.Geodesic
                        )
                        geometryData["AREA_SQ_METERS"] = area
                    }
                    else -> null
                }

                val dataResult = mutableMapOf<String, Any?>(
                    "DATA" to geometryData,
                    "GEOMETRY_TYPE" to geometryType.toString(),
                )

                val allFeaturesAttrs = mutableMapOf<String, Any?>()

                lifecycle.coroutineScope.launch {
                    mapView.map?.operationalLayers
                        ?.filterIsInstance<FeatureLayer>()
                        ?.forEach { layer ->

                            val queryParams = QueryParameters().apply {
                                this.geometry = geometry
                                this.spatialRelationship = SpatialRelationship.Intersects
                            }

                            val table = layer.featureTable
                            val resultFeatures = table?.queryFeatures(queryParams)?.getOrNull()

                            resultFeatures?.forEach { feature ->
                                val attrs = mutableMapOf<String, Any?>()
                                feature.attributes.forEach { (k, v) ->
                                    attrs[k] = v
                                }
                                allFeaturesAttrs.putAll(attrs)
                            }
                        }

                    dataResult["FEATURES_ATTRIBUTES"] = gson.toJson(allFeaturesAttrs)

                    result.success(dataResult)
                    geometryEditor.stop()
                }
            }
            "/cancelEditing" -> {
                geometryEditor.stop()
            }
            "/addGeometry" -> {
                val arguments = call.arguments as Map<*, *>
                val graphicAttributes = arguments["_graphicAttributes"] as? Map<*, *>
                val graphic = graphicFromParams(arguments, result)
                if(graphic == null) {
                    result.error("FAILED", "Error in /addGeometry", "graphic is null")
                    return
                }
                graphicAttributes?.forEach { (k, v) -> graphic.attributes[k.toString()] = v }
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
            "/hideFeaturesByCodes" -> {
                // Oculta persistentemente los features cuyo campo identificador esté en
                // la lista de códigos (canales "modified" cuya geometría se clonó/editó),
                // mediante un definitionExpression. Lista vacía → muestra todos.
                val arguments = call.arguments as? Map<*, *>
                val codes = (arguments?.get("codes") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val field = arguments?.get("field")?.toString() ?: "DIDENTIF"
                val layer = activeFeatureLayer
                if (layer != null) {
                    layer.definitionExpression = if (codes.isEmpty()) {
                        ""
                    } else {
                        val inList = codes.joinToString(",") { "'${it.replace("'", "''")}'" }
                        "$field NOT IN ($inList)"
                    }
                    layer.clearSelection()
                }
            }
            "/selectGraphicByCode" -> {
                // Selecciona (resalta) el gráfico cuyo atributo cadastralCode coincide,
                // deselecciona los demás y centra/encaja la vista en su geometría.
                val code = call.arguments?.toString()
                val graphic = graphicsOverlay.graphics.firstOrNull {
                    it.attributes["cadastralCode"]?.toString() == code
                }
                if (graphic != null) {
                    activeFeatureLayer?.clearSelection()
                    highlightGraphic(graphic)
                    graphic.geometry?.let { geom ->
                        lifecycle.coroutineScope.launch {
                            mapView.setViewpointGeometry(geom, 60.0)
                        }
                    }
                }
            }
            "/selectFeatureByCode" -> {
                // Selecciona (resalta) en la capa el feature cuyo campo identificador
                // coincide, deselecciona lo anterior y centra la vista en su geometría.
                val arguments = call.arguments as? Map<*, *>
                val code = arguments?.get("code")?.toString()
                val field = arguments?.get("field")?.toString() ?: "DIDENTIF"
                val layer = activeFeatureLayer
                val featureTable = layer?.featureTable
                if (layer != null && featureTable != null && code != null) {
                    val queryParameters = QueryParameters().apply {
                        whereClause = "$field = '${code.replace("'", "''")}'"
                        maxFeatures = 1
                    }
                    lifecycle.coroutineScope.launch {
                        layer.clearSelection()
                        resetAllGraphicSymbols()
                        val feature = featureTable.queryFeatures(queryParameters)
                            .getOrNull()?.firstOrNull()
                        if (feature != null) {
                            layer.selectFeature(feature)
                            feature.geometry?.let { geom ->
                                mapView.setViewpointGeometry(geom, 60.0)
                            }
                        }
                    }
                }
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
        val graphicAttributes = params["_graphicAttributes"] as? Map<*, *>
        val geometryParams = params.toMutableMap().apply { remove("_graphicAttributes") }
        val geometryString = JSONObject(geometryParams as Map<*, *>).toString()
        val geometry = Geometry.fromJsonOrNull(geometryString)

        if(geometry == null) {
            result.error("FAILED", "Error in /addGeometry or /removeGeometry", "Geometry is null")
            return null
        }

        return Graphic(geometry).apply {
            attributes["_colorR"] = drawColorR
            attributes["_colorG"] = drawColorG
            attributes["_colorB"] = drawColorB
            attributes["_colorA"] = drawColorA
            // Los atributos pasados pueden sobreescribir _colorR/G/B/A
            graphicAttributes?.forEach { (k, v) -> attributes[k.toString()] = v }
            // Construir símbolo desde el color final en atributos
            val r = (attributes["_colorR"] as? Number)?.toInt() ?: drawColorR
            val g = (attributes["_colorG"] as? Number)?.toInt() ?: drawColorG
            val b = (attributes["_colorB"] as? Number)?.toInt() ?: drawColorB
            val a = (attributes["_colorA"] as? Number)?.toInt() ?: drawColorA
            val c = Color.fromRgba(r, g, b, a)
            symbol = when (geometry) {
                is Polygon -> SimpleFillSymbol(SimpleFillSymbolStyle.Cross, Color.fromRgba(r, g, b, (a * 0.43).toInt()), SimpleLineSymbol(SimpleLineSymbolStyle.Solid, c, 4f))
                is Polyline -> SimpleLineSymbol(SimpleLineSymbolStyle.Solid, c, 4f)
                is Point, is Multipoint -> SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, c, 20f)
                else -> null
            }
        }
    }

    private fun areGraphicsEqual(existingGraphic: Graphic, graphic: Graphic): Boolean {
        return existingGraphic.geometry == graphic.geometry
    }
}