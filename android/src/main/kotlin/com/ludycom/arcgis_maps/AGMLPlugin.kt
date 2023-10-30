package com.ludycom.arcgis_maps

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.arcgismaps.data.Geodatabase
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.tasks.geodatabase.GeodatabaseSyncTask
import com.arcgismaps.tasks.geodatabase.SyncDirection
import com.arcgismaps.tasks.geodatabase.SyncGeodatabaseParameters
import com.arcgismaps.tasks.geodatabase.SyncLayerOption
import com.google.gson.Gson
import com.ludycom.arcgis_maps.entities.agml.AGMLDownloadPortalItem
import com.ludycom.arcgis_maps.entities.agml.AGMLGeodatabase
import com.ludycom.arcgis_maps.entities.agml.AGMLPortalItem
import com.ludycom.arcgis_maps.entities.agml.AGMLServiceFeature
import com.ludycom.arcgis_maps.entities.agml.AGMLViewPoint
import com.ludycom.arcgis_maps.pigeons.AuthPigeonImpl
import com.ludycom.arcgis_maps.utils.AGMLDownloadStatusEnum

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.embedding.engine.plugins.lifecycle.FlutterLifecycleAdapter
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.util.UUID


lateinit var flutterBinaryMessenger: BinaryMessenger


class AGMLPlugin: FlutterPlugin, MethodCallHandler, ActivityAware {

  private var context: Context? = null
  private var lifecycle: Lifecycle? = null
  private lateinit var channel : MethodChannel
  private val downloadArea: Graphic = Graphic()

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "arcgis_maps")
    channel.setMethodCallHandler { call, result -> onMethodCall(call, result) }

    context = flutterPluginBinding.applicationContext
    flutterBinaryMessenger = flutterPluginBinding.binaryMessenger

    AuthPigeon.AGMLAuthApi.setUp(flutterBinaryMessenger, AuthPigeonImpl(context))
    AuthPigeon.AGMLAuthApiHandler(flutterBinaryMessenger)

    flutterPluginBinding
      .platformViewRegistry
      .registerViewFactory(
        "plugins.flutter.io/arcgis_maps",
        AGMLViewFactory(
          flutterPluginBinding.binaryMessenger,
          object : LifecycleProvider {
            override fun getLifecycle(): Lifecycle? {
              return lifecycle
            }
          }
        )
      )
  }


  override fun onMethodCall(call: MethodCall, result: Result) {
    when(call.method) {
      "/downloadPortalItem" -> {
        val arguments = call.arguments as Map<*, *>
        val arcGISMapServicePortalItem = Gson().fromJson(JSONObject(arguments).toString(), AGMLPortalItem::class.java)

        val aGMLDownloadFromPortal = AGMLDownloadFromPortal(context!!)

        lifecycle!!.coroutineScope.launch {
          val downloadResponse = aGMLDownloadFromPortal.downloadPortalItem(arcGISMapServicePortalItem)
          result.success(Gson().toJson(downloadResponse))
        }
      }
      "/checkDownloadedPortalItems" -> {
        @Suppress("UNCHECKED_CAST")
        val arguments = call.arguments as List<Map<*, *>>
        val jsonCheckDownloadPortalItems = mutableListOf<String>()

        val gson = Gson()

        arguments.forEach {
          val aGMLPortalItem = gson.fromJson(JSONObject(it).toString(), AGMLPortalItem::class.java)
          val portalItem = PortalItem(aGMLPortalItem.url)
          val portalFolderPath = context!!.getExternalFilesDir(null)?.absolutePath.toString()+File.separator+"Portal Items"+File.separator+portalItem.itemId
          val filePortalFolder = File(portalFolderPath)

          lateinit var downloadPortalItem: AGMLDownloadPortalItem

          lifecycle!!.coroutineScope.launch {
            portalItem.load().onSuccess {
              downloadPortalItem = if (filePortalFolder.exists()) {
                AGMLDownloadPortalItem(
                  portalItem = aGMLPortalItem,
                  AGMLDownloadStatusEnum.FILE_EXISTS,
                  portalFolderPath+File.separator/*+portalItem.name*/
                )
              } else {
                AGMLDownloadPortalItem(
                  portalItem = aGMLPortalItem,
                  AGMLDownloadStatusEnum.FILE_NO_EXISTS,
                  ""
                )
              }
              jsonCheckDownloadPortalItems.add(gson.toJson(downloadPortalItem))
            }.onFailure {
              downloadPortalItem = AGMLDownloadPortalItem(
                portalItem = aGMLPortalItem,
                AGMLDownloadStatusEnum.FAILED,
                ""
              )
              jsonCheckDownloadPortalItems.add(gson.toJson(downloadPortalItem))
            }.also {
              if(jsonCheckDownloadPortalItems.size == arguments.size) {
                result.success(jsonCheckDownloadPortalItems)
              }
            }
          }
        }
      }
      "/generateGeodatabaseReplicaFromFeatureService" -> {
        val arguments = call.arguments as Map<*, *>
        val agmlFeatureService = Gson().fromJson(JSONObject(arguments).toString(), AGMLServiceFeature::class.java)

        val gson = Gson()

        val geoDatabasesSyncTask = GeodatabaseSyncTask(agmlFeatureService.url)

        lifecycle!!.coroutineScope.launch {
          geoDatabasesSyncTask.load().onSuccess {

            /*

            val minScreenPoint = ScreenCoordinate(200.0, 200.0)
            if(
              geoDatabasesSyncTask.featureServiceInfo?.fullExtent?.width == null ||
              geoDatabasesSyncTask.featureServiceInfo?.fullExtent?.height == null
            ) {
              result.error("LOAD_ERROR" ,"Error in .featureServiceInfo?.fullExtent?", "May be is null")
              return@launch
            }

            val maxScreenPoint = ScreenCoordinate(
              geoDatabasesSyncTask.featureServiceInfo!!.fullExtent!!.width - 200.0,
              geoDatabasesSyncTask.featureServiceInfo!!.fullExtent!!.height - 200.0
            )

            val mapView = MapView(context!!)

            val minPoint = mapView.screenToLocation(minScreenPoint)
            val maxPoint = mapView.screenToLocation(maxScreenPoint)

            if(minPoint == null || maxPoint == null) {
              result.error("POINT_ERROR" ,"Error in minPoint == null || maxPoint == null", "May be is null")
              return@launch
            }

            val envelope = Envelope(minPoint, maxPoint)
            downloadArea.geometry = envelope

             */

            val defaultParameters = geoDatabasesSyncTask.createDefaultGenerateGeodatabaseParameters(
              //downloadArea.geometry!!.extent
              geoDatabasesSyncTask.featureServiceInfo!!.fullExtent!!
            ).getOrElse { err ->
              result.error("LOAD_ERROR" ,"Error in geoDatabasesSyncTask.createDefaultGenerateGeodatabaseParameters()", err.message)
              return@launch
            }.apply {
              layerOptions.removeIf { layerOptions ->
                layerOptions.layerId != 0L
              }
            }
            defaultParameters.returnAttachments = false

            val provisionFolder = File(context!!.getExternalFilesDir(null)?.absolutePath.toString()+File.separator+"Sync")
            if (!provisionFolder.exists()) {
              provisionFolder.mkdirs()
            }

            val fileName = UUID.randomUUID().toString()

            geoDatabasesSyncTask.createGenerateGeodatabaseJob(
              defaultParameters,
              provisionFolder.path+File.separator+fileName+".geodatabase"
            ).run {
              start()
              val geodatabase = result().getOrElse { err ->
                result.error("LOAD_ERROR" ,"Error in geoDatabasesSyncTask.load()", err.message)
                return@launch
              }
              //geoDatabasesSyncTask.unregisterGeodatabase(geodatabase)

              result.success(gson.toJson(
                AGMLGeodatabase(
                  path = geodatabase.path,
                  url = agmlFeatureService.url,
                  viewPoint = null
                  //AGMLViewPoint(
                  //  latitude = geoDatabasesSyncTask.featureServiceInfo!!.fullExtent!!.center.y,
                  //  longitude = geoDatabasesSyncTask.featureServiceInfo!!.fullExtent!!.center.x,
                  //  scale = 4000.0
                  //)
              )))
            }
          }.onFailure { err ->
            result.error("LOAD_ERROR" ,"Error in geoDatabasesSyncTask.load()", err.message)
          }
        }
      }
      "/syncGeodatabaseReplicaToFeatureService" -> {
        val arguments = call.arguments as Map<*, *>
        val agmlGeodatabase = Gson().fromJson(JSONObject(arguments).toString(), AGMLGeodatabase::class.java)

        val geodatabase = Geodatabase(agmlGeodatabase.path!!)
        val geodatabaseSyncTask = GeodatabaseSyncTask(agmlGeodatabase.url!!)

        val syncParams = SyncGeodatabaseParameters()
        syncParams.geodatabaseSyncDirection = SyncDirection.Bidirectional
        syncParams.shouldRollbackOnFailure = false

        val syncDirection = SyncLayerOption()

        geodatabaseSyncTask.createSyncGeodatabaseJob(syncParams, geodatabase).run {
          start()
          lifecycle!!.coroutineScope.launch {
            val result = result().getOrElse { err ->
              result.error("SYNC_ERROR" ,"Error in geodatabaseSyncTask.createSyncGeodatabaseJob()", err.message)
              return@launch
            }

            println(result)
          }
        }
      }
    }
  }


  override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    lifecycle = FlutterLifecycleAdapter.getActivityLifecycle(binding)
  }

  override fun onDetachedFromActivityForConfigChanges() {
    lifecycle = null
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    lifecycle = FlutterLifecycleAdapter.getActivityLifecycle(binding)
  }

  override fun onDetachedFromActivity() {
    lifecycle = null
  }

  interface LifecycleProvider {
    fun getLifecycle(): Lifecycle?
  }
}