package com.ludycom.arcgis_maps

import android.content.Context
import android.util.Log
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditor
import com.google.gson.Gson
import com.ludycom.arcgis_maps.entities.agml.AGMLParams

import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory
import org.json.JSONObject

class AGMLViewFactory(
    private val messenger: BinaryMessenger,
    private val lifecycleProvider: AGMLPlugin.LifecycleProvider
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {

    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        val creationParams = args as Map<*, *>

        return AGMLMapView(
            context,
            messenger,
            lifecycleProvider,
            viewId,
            creationParams
        )
    }
}


internal class AGMLMapView(
    private val context: Context,
    messenger: BinaryMessenger,
    lifecycleProvider: AGMLPlugin.LifecycleProvider,
    id: Int,
    creationParams: Map<*, *>) :
    PlatformView {

    private var methodChannel: MethodChannel
    private var mapView = MapView(context)
    private var params: AGMLParams

    private var graphicsOverlay: GraphicsOverlay = GraphicsOverlay()


    private var lifecycle: Lifecycle = lifecycleProvider.getLifecycle()
        ?: throw RuntimeException("Context is null, can't create MapView!")

    init {
        params = Gson().fromJson(JSONObject(creationParams).toString(), AGMLParams::class.java)

        lifecycle.addObserver(mapView)

        val channel = "plugins.flutter.io/arcgis_maps:${id}"


        if(params.basemapStyle.getBasemapStyle() != null) {
            mapView.map = ArcGISMap(params.basemapStyle.getBasemapStyle()!!)
        } else {
            mapView.map = ArcGISMap()
        }

        mapView.graphicsOverlays.add(graphicsOverlay)

        mapView.selectionProperties.color = Color.red

        val aGMLViewMethodCall = AGMLViewMethodCall(
            context,
            messenger,
            channel,
            lifecycle,
            mapView,
            graphicsOverlay
        )

        methodChannel = MethodChannel(messenger, channel)
        methodChannel.setMethodCallHandler { call, result -> aGMLViewMethodCall.onMethodCall(
            call,
            result
        ) }

        MethodChannel(messenger, "$channel:mapStatus").invokeMethod("/mapIsReady", "")
    }


    override fun getView(): View {
        return mapView
    }

    override fun dispose() {
        lifecycle.removeObserver(mapView)
        methodChannel.setMethodCallHandler(null)

        try {
            mapView.onStop(context as LifecycleOwner)
            mapView.onDestroy(context as LifecycleOwner)
        } catch (e: Exception) {
            Log.e("context as LifecycleOwner:", e.toString())
        }
    }

}

