package com.ludycom.arcgis_maps

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.arcgismaps.data.Geodatabase
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.tasks.geodatabase.GenerateLayerOption
import com.arcgismaps.tasks.geodatabase.GeodatabaseSyncTask
import com.arcgismaps.tasks.geodatabase.SyncDirection
import com.arcgismaps.tasks.geodatabase.SyncGeodatabaseParameters
import com.arcgismaps.tasks.geodatabase.SyncLayerOption
import com.arcgismaps.tasks.geodatabase.SyncModel
import com.google.gson.Gson
import com.ludycom.arcgis_maps.entities.agml.AGMLChangeSpacialReferenceParams
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
        try {
          val arguments = call.arguments as Map<*, *>
          val arcGISMapServicePortalItem = Gson().fromJson(JSONObject(arguments).toString(), AGMLPortalItem::class.java)

          val aGMLDownloadFromPortal = AGMLDownloadFromPortal(context!!)

          lifecycle!!.coroutineScope.launch {
            val downloadResponse = aGMLDownloadFromPortal.downloadPortalItem(arcGISMapServicePortalItem)
            result.success(Gson().toJson(downloadResponse))
          }
        } catch (err: Exception) {
          result.error("LOAD_ERROR", "Error in downloadPortalItem", err.message)
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
        val agmlFeatureService = Gson().fromJson(
          JSONObject(arguments).toString(),
          AGMLServiceFeature::class.java
        )

        val gson = Gson()
        val contextRef = context ?: run {
          result.error("CONTEXT_ERROR", "Context is null", null)
          return@onMethodCall
        }

        val provisionFolder = File(contextRef.getExternalFilesDir(null), "Sync").apply {
          if (!exists()) mkdirs()
        }

        val geoDatabasesSyncTask = GeodatabaseSyncTask(agmlFeatureService.url)

        lifecycle?.coroutineScope?.launch {
          geoDatabasesSyncTask.load().onSuccess {
            val serviceInfo = geoDatabasesSyncTask.featureServiceInfo ?: run {
              result.error("SERVICE_INFO_ERROR", "FeatureServiceInfo is null", null)
              return@launch
            }

            // ===== DIAGNÓSTICO COMPLETO DEL SERVICIO =====
            Log.d("ServiceDiag", "==========================================")
            Log.d("ServiceDiag", "URL: ${agmlFeatureService.url}")
            Log.d("ServiceDiag", "Description: ${serviceInfo.serviceDescription}")

            // ✅ Validar capacidades usando serviceCapabilities
            val capabilities = serviceInfo.featureServiceCapabilities
            Log.d("ServiceDiag", "=== FEATURE SERVICE CAPABILITIES ===")
            Log.d("ServiceDiag", "Supports Sync: ${capabilities?.supportsSync}")
            Log.d("ServiceDiag", "Supports Query: ${capabilities?.supportsQuery}")
            Log.d("ServiceDiag", "Supports Create: ${capabilities?.supportsCreate}")
            Log.d("ServiceDiag", "Supports Update: ${capabilities?.supportsUpdate}")
            Log.d("ServiceDiag", "Supports Delete: ${capabilities?.supportsDelete}")
            Log.d("ServiceDiag", "Supports Editing: ${capabilities?.supportsEditing}")

            // Verificar si soporta sincronización
            if (capabilities?.supportsSync != true) {
              result.error(
                "SYNC_NOT_SUPPORTED",
                "El servicio no tiene habilitada la sincronización. Capabilities: $capabilities",
                null
              )
              return@launch
            }

            // ===== SYNC CAPABILITIES DETALLADAS =====
            val syncCaps = serviceInfo.syncCapabilities
            Log.d("ServiceDiag", "=== SYNC CAPABILITIES ===")
            Log.d("ServiceDiag", "supportsSyncModelLayer Sync: ${syncCaps?.supportsSyncModelLayer}")
            Log.d("ServiceDiag", "Async: ${syncCaps?.supportsAsync}")
            Log.d("ServiceDiag", "Attachments Sync Direction: ${syncCaps?.supportsAttachmentsSyncDirection}")
            Log.d("ServiceDiag", "Sync Direction Control: ${syncCaps?.supportsSyncDirectionControl}")
            Log.d("ServiceDiag", "Register Existing Data: ${syncCaps?.supportsRegisteringExistingData}")

            val fullExtent = serviceInfo.fullExtent ?: run {
              result.error("EXTENT_ERROR", "Full extent is null", null)
              return@launch
            }

            // ✅ Validar y diagnosticar extent
            Log.d("ServiceDiag", "=== EXTENT ===")
            Log.d("ServiceDiag", "xMin: ${fullExtent.xMin}, yMin: ${fullExtent.yMin}")
            Log.d("ServiceDiag", "xMax: ${fullExtent.xMax}, yMax: ${fullExtent.yMax}")
            Log.d("ServiceDiag", "Width: ${fullExtent.width}, Height: ${fullExtent.height}")
            Log.d("ServiceDiag", "Spatial Reference WKID: ${fullExtent.spatialReference?.wkid}")
            Log.d("ServiceDiag", "Spatial Reference WKText: ${fullExtent.spatialReference?.wkText}")
            Log.d("ServiceDiag", "Is Empty: ${fullExtent.isEmpty}")

            if (fullExtent.isEmpty) {
              result.error("INVALID_EXTENT", "Extent está vacío", null)
              return@launch
            }

            // Validar valores del extent
            if (fullExtent.xMin.isNaN() || fullExtent.xMin.isInfinite() ||
              fullExtent.yMin.isNaN() || fullExtent.yMin.isInfinite() ||
              fullExtent.xMax.isNaN() || fullExtent.xMax.isInfinite() ||
              fullExtent.yMax.isNaN() || fullExtent.yMax.isInfinite()) {
              result.error("INVALID_EXTENT", "Extent contiene valores inválidos (NaN o Infinito)", null)
              return@launch
            }

            // ===== INFORMACIÓN DE CAPAS =====
            Log.d("ServiceDiag", "=== LAYERS (Total: ${serviceInfo.layerInfos.size}) ===")
            serviceInfo.layerInfos.forEach { layerInfo ->
              Log.d("LayerDiag", "Layer ID: ${layerInfo.id}")
              Log.d("LayerDiag", "  Name: ${layerInfo.name}")
            }
            Log.d("ServiceDiag", "==========================================")

            val defaultParameters = geoDatabasesSyncTask
              .createDefaultGenerateGeodatabaseParameters(fullExtent)
              .getOrElse { err ->
                Log.e("ParamsError", "Error creando parámetros: ${err.message}", err)
                result.error("PARAMS_ERROR", "Error creando parámetros: ${err.message}", err.message)
                return@launch
              }

            // ===== DIAGNÓSTICO DE PARÁMETROS POR DEFECTO =====
            Log.d("ParamsDiag", "=== PARÁMETROS INICIALES (POR DEFECTO) ===")
            Log.d("ParamsDiag", "Layer Options Count: ${defaultParameters.layerOptions.size}")
            defaultParameters.layerOptions.forEach { option ->
              Log.d("ParamsDiag", "  Layer ID en options: ${option.layerId}")
            }
            Log.d("ParamsDiag", "Sync Model: ${defaultParameters.syncModel}")
            Log.d("ParamsDiag", "Return Attachments: ${defaultParameters.returnAttachments}")
            Log.d("ParamsDiag", "Out Spatial Reference: ${defaultParameters.outSpatialReference?.wkid}")

            // ✅ Configurar capas
            defaultParameters.layerOptions.clear()

            val validLayers = serviceInfo.layerInfos.mapNotNull { it }

            if (validLayers.isEmpty()) {
              result.error("NO_VALID_LAYERS", "No hay capas válidas para sincronizar", null)
              return@launch
            }

            Log.d("ParamsDiag", "=== CONFIGURANDO CAPAS ===")
            validLayers.forEach { layer ->
              Log.d("ParamsDiag", "Agregando Layer ID: ${layer.id}, Name: ${layer.name}")
              defaultParameters.layerOptions.add(GenerateLayerOption(layer.id!!))
            }

            // Configuración
            defaultParameters.returnAttachments = false

            // ✅ Especificar spatial reference de salida
            defaultParameters.outSpatialReference = fullExtent.spatialReference

            // ===== DIAGNÓSTICO DE PARÁMETROS FINALES =====
            Log.d("ParamsDiag", "=== PARÁMETROS FINALES ===")
            Log.d("ParamsDiag", "Layer Options Count: ${defaultParameters.layerOptions.size}")
            Log.d("ParamsDiag", "Sync Model: ${defaultParameters.syncModel}")
            Log.d("ParamsDiag", "Return Attachments: ${defaultParameters.returnAttachments}")
            Log.d("ParamsDiag", "Out Spatial Reference: ${defaultParameters.outSpatialReference?.wkid}")

            val fileName = UUID.randomUUID().toString()
            val geodatabasePath = File(provisionFolder, "$fileName.geodatabase").path

            Log.d("JobDiag", "=== CREANDO JOB ===")
            Log.d("JobDiag", "Ruta geodatabase: $geodatabasePath")
            Log.d("JobDiag", "Nombre archivo: $fileName")

            val job = geoDatabasesSyncTask.createGenerateGeodatabaseJob(
              defaultParameters,
              geodatabasePath
            )

            // ✅ Colector de mensajes en paralelo
            launch {
              try {
                job.messages.collect { message ->
                  Log.d("JobMessage", "[${message.severity}] ${message.message}")
                }
              } catch (e: Exception) {
                Log.e("JobMessage", "Error colectando mensajes: ${e.message}", e)
              }
            }

            // ✅ Colector de progreso en paralelo
            launch {
              try {
                job.progress.collect { progress ->
                  Log.d("JobProgress", "Progreso: $progress%")
                }
              } catch (e: Exception) {
                Log.e("JobProgress", "Error colectando progreso: ${e.message}", e)
              }
            }

            // ✅ Colector de estado en paralelo
            launch {
              try {
                job.status.collect { status ->
                  Log.d("JobStatus", "Estado del job: $status")
                }
              } catch (e: Exception) {
                Log.e("JobStatus", "Error colectando estado: ${e.message}", e)
              }
            }

            Log.d("JobDiag", "Iniciando job...")
            job.start()
            Log.d("JobDiag", "Job iniciado, esperando resultado...")

            val geodatabase = job.result().getOrElse { err ->
              Log.e("JobError", "==========================================")
              Log.e("JobError", "===== ERROR EN LA GENERACIÓN =====")
              Log.e("JobError", "Tipo de excepción: ${err::class.simpleName}")
              Log.e("JobError", "Mensaje: ${err.message}")
              Log.e("JobError", "Causa: ${err.cause}")
              Log.e("JobError", "Causa mensaje: ${err.cause?.message}")
              Log.e("JobError", "Stack trace:", err)

              // Si es ServiceException, mostrar más detalles
              if (err is com.arcgismaps.exceptions.ServiceException) {
                Log.e("JobError", "ServiceException detectada")
                Log.e("JobError", "Detalles del error de servicio: ${err.message}")
              }

              Log.e("JobError", "==========================================")

              result.error(
                "GENERATION_ERROR",
                "Error generando geodatabase: ${err.message}",
                err.cause?.message
              )
              return@launch
            }

            Log.d("JobDiag", "==========================================")
            Log.d("JobDiag", "===== GENERACIÓN EXITOSA =====")
            Log.d("JobDiag", "Ruta: ${geodatabase.path}")
            Log.d("JobDiag", "Feature Tables: ${geodatabase.featureTables.size}")
            geodatabase.featureTables.forEach { table ->
              Log.d("JobDiag", "  Table: ${table.tableName}, Features: ${table.numberOfFeatures}")
            }
            Log.d("JobDiag", "==========================================")

            geoDatabasesSyncTask.unregisterGeodatabase(geodatabase)
              .onSuccess {
                Log.d("Unregister", "Geodatabase desregistrada exitosamente")
              }
              .onFailure { err ->
                Log.w("Unregister", "No se pudo desregistrar: ${err.message}")
              }

            val response = AGMLGeodatabase(
              path = geodatabase.path,
              url = agmlFeatureService.url,
              viewPoint = null
            )

            result.success(gson.toJson(response))

          }.onFailure { err ->
            Log.e("LoadError", "==========================================")
            Log.e("LoadError", "===== ERROR CARGANDO SERVICIO =====")
            Log.e("LoadError", "Tipo: ${err::class.simpleName}")
            Log.e("LoadError", "Mensaje: ${err.message}")
            Log.e("LoadError", "Causa: ${err.cause?.message}")
            Log.e("LoadError", "Stack trace:", err)
            Log.e("LoadError", "==========================================")

            result.error(
              "LOAD_ERROR",
              "Error cargando servicio: ${err.message}",
              err.cause?.message
            )
          }
        } ?: run {
          result.error("LIFECYCLE_ERROR", "Lifecycle is null", null)
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
      "/changeSpacialReference" -> {
        val arguments = call.arguments as Map<*, *>
        val changeSpacialReferenceParams = Gson().fromJson(JSONObject(arguments).toString(), AGMLChangeSpacialReferenceParams::class.java)


        val point = GeometryEngine.projectOrNull(
          Point(
            changeSpacialReferenceParams.point.longitude, changeSpacialReferenceParams.point.latitude, SpatialReference(changeSpacialReferenceParams.fromSpacialReference)),
            SpatialReference(changeSpacialReferenceParams.toSpacialReference)
          ) as Point

        val gson = Gson()

        result.success(gson.toJson(
          AGMLViewPoint(
            latitude = point.y,
            longitude = point.x,
            scale = 3000.0
          )
        ))
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