package com.ludycom.arcgis_maps

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.tasks.geodatabase.GeodatabaseSyncTask
import com.google.gson.Gson
import com.ludycom.arcgis_maps.entities.agml.AGMLDownloadPortalItem
import com.ludycom.arcgis_maps.entities.agml.AGMLPortalItem
import com.ludycom.arcgis_maps.entities.agml.AGMLServiceFeature
import com.ludycom.arcgis_maps.pigeons.AuthPigeonImpl
import com.ludycom.arcgis_maps.utils.AGMLDownloadStatusEnum

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.embedding.engine.plugins.lifecycle.FlutterLifecycleAdapter
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File



class AGMLPlugin: FlutterPlugin, MethodCallHandler, ActivityAware {

  private var context: Context? = null
  private var lifecycle: Lifecycle? = null
  private lateinit var channel : MethodChannel
  private val downloadArea: Graphic = Graphic()

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "arcgis_maps")
    channel.setMethodCallHandler { call, result -> onMethodCall(call, result) }

    context = flutterPluginBinding.applicationContext

    // ArcGIS Methods
    AuthPigeon.AuthApi.setUp(flutterPluginBinding.binaryMessenger, AuthPigeonImpl(context))

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
      "/downloadClipPortalItemGeoDatabase" -> {
        val arguments = call.arguments as Map<*, *>
        val arcGISMapFeatureService = Gson().fromJson(JSONObject(arguments).toString(), AGMLServiceFeature::class.java)

        val geoDatabasesSyncTask = GeodatabaseSyncTask(arcGISMapFeatureService.url)


        lifecycle!!.coroutineScope.launch {
          geoDatabasesSyncTask.load().onSuccess {
//            val fullExtent = geoDatabasesSyncTask.featureServiceInfo?.fullExtent;
            val minScreenPoint = ScreenCoordinate(200.0, 200.0)
            if(
              geoDatabasesSyncTask.featureServiceInfo?.fullExtent?.width != null &&
              geoDatabasesSyncTask.featureServiceInfo?.fullExtent?.height != null
            ) {
              val maxScreenPoint = ScreenCoordinate(
                geoDatabasesSyncTask.featureServiceInfo!!.fullExtent!!.width - 200.0,
                geoDatabasesSyncTask.featureServiceInfo!!.fullExtent!!.height - 200.0
              )

              // convert screen points to map points
              val minPoint = Point(minScreenPoint.x, minScreenPoint.y, SpatialReference.webMercator())
              val maxPoint = Point(maxScreenPoint.x, maxScreenPoint.y, SpatialReference.webMercator())
              // use the points to define and return an envelope
//              if (minPoint != null && maxPoint != null) {
              val envelope = Envelope(minPoint, maxPoint)
              downloadArea.geometry = envelope
//              }
              val defaultParameters = geoDatabasesSyncTask.createDefaultGenerateGeodatabaseParameters(
                downloadArea.geometry!!.extent
              ).getOrElse { err ->
                result.success(err.message)

                return@launch
              }.apply {
                layerOptions.removeIf { layerOptions ->
                  layerOptions.layerId != 0L
                }
              }
              defaultParameters.returnAttachments = false


              geoDatabasesSyncTask.createGenerateGeodatabaseJob(
                defaultParameters,
                context!!.getExternalFilesDir(null)!!.path+"test.geodatabase"
              ).run {
                start()
                val geodatabase = result().getOrElse { err ->
                  result.success(err.message)
                  return@launch
                }
                geoDatabasesSyncTask.unregisterGeodatabase(geodatabase)

              }
            }

//            if(fullExtent != null) {
//
//            }



          }.onFailure { err ->
            result.success(err.message)

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



//        val aGMLDownloadPortalItem = AGMLDownloadPortalItem(
//          portalItem = arcGISMapServicePortalItem,
//          pathLocation = ""
//        )

//        val portalItem = PortalItem(aGMLDownloadPortalItem.portalItem.url)
//        val offlineMapTask  = OfflineMapTask(portalItem)

//        val envelope = Envelope(
//          -13049000.0, 3861000.0, -13048000.0, 3861500.0,
//          spatialReference = SpatialReference.webMercator()
//        )



//          offlineMapTask.createDefaultGenerateOfflineMapParameters(
//            envelope,
//            5000.0,
//            10000.0
//          ).onSuccess { offlineParams ->
//            val folderPath = context!!.getExternalFilesDir(null)?.absolutePath.toString()+File.separator+"Portal Map Task"
//
//            val provisionFolder = File(folderPath)
//            if (!provisionFolder.exists()) {
//              provisionFolder.mkdirs()
//            }
//
//            offlineParams.includeBasemap = true
//            val offlineMapJob = offlineMapTask.createGenerateOfflineMapJob(
//              offlineParams,
//              provisionFolder.path
//            )
//
//            offlineMapJob.start()
//            offlineMapJob.result().onSuccess { offlineMapResult ->
//              if (offlineMapResult.offlineMap.utilityNetworks.size > 0) {
//                val utilityNetwork = offlineMapResult.offlineMap.utilityNetworks[0]
//                utilityNetwork.load().onSuccess {
//                  result.success("Todo bien")
//                }.onFailure { err ->
//                  println(err)
//                }
//              }
//            }.onFailure { err ->
//              println(err)
//            }
//          }.onFailure { err ->
//            println(err)
//          }