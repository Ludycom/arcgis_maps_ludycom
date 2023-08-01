package com.ludycom.arcgis_maps

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.arcgismaps.mapping.PortalItem
import com.google.gson.Gson
import com.ludycom.arcgis_maps.entities.AGMLDownloadPortalItem
import com.ludycom.arcgis_maps.entities.AGMLPortalItem
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

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "arcgis_maps")
    channel.setMethodCallHandler { call, result -> onMethodCall(call, result) }

    context = flutterPluginBinding.applicationContext

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
