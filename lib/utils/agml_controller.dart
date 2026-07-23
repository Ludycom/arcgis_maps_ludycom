import 'dart:async';
import 'dart:convert';
import 'package:arcgis_maps/entities/agml_color.dart';
import 'package:arcgis_maps/entities/agml_geodatabase.dart';
import 'package:arcgis_maps/entities/agml_mobile_map_package.dart';
import 'package:arcgis_maps/entities/agml_selected_layer_arguments.dart';
import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';


import 'package:arcgis_maps/entities/features/agml_portal_item.dart';
import 'package:arcgis_maps/entities/features/agml_local_shapefile.dart';
import 'package:arcgis_maps/entities/features/agml_local_geopackage.dart';
import 'package:arcgis_maps/entities/features/agml_local_feature_layer.dart';
import 'package:arcgis_maps/entities/features/agml_feature_service_layer.dart';
import 'package:arcgis_maps/entities/features/agml_arcgis_online_portal_item.dart';
import 'package:arcgis_maps/entities/features/abstract_agml_feature_layer.dart';

import 'package:arcgis_maps/entities/agml_view_point.dart';

import '../entities/geometries/geometry.dart';
import 'enums/enums.dart';



class AGMLMapController {

  late MethodChannel _channel;

  final StreamController<List<dynamic>> _selectedLayerStreamController = StreamController();
  StreamController<List<dynamic>> get selectedLayerStreamController => _selectedLayerStreamController;

  final StreamController<Map<String, dynamic>> _selectedGraphicStreamController = StreamController.broadcast();
  StreamController<Map<String, dynamic>> get selectedGraphicStreamController => _selectedGraphicStreamController;

  late List<AGMLFeatureServiceLayer> _mapServiceLayers;
  List<AGMLFeatureServiceLayer> get mapServiceLayers => _mapServiceLayers;
  final StreamController<List<AGMLFeatureServiceLayer>> _onChangedMapServiceLayersStreamController = StreamController();
  StreamController<List<AGMLFeatureServiceLayer>> get onChangedMapServiceLayersStreamController => _onChangedMapServiceLayersStreamController;

  late List<AGMLLocalFeatureLayer> _mapLocalLayers;
  List<AGMLLocalFeatureLayer> get mapLocalLayers => _mapLocalLayers;
  final StreamController<List<AGMLLocalFeatureLayer>> _onChangedMapLocalLayersStreamController = StreamController();
  StreamController<List<AGMLLocalFeatureLayer>> get onChangedMapLocalLayersStreamController => _onChangedMapLocalLayersStreamController;

  Stream<bool>? canUndoStream;
  Stream<bool>? canRedoStream;
  Stream<bool>? canDeleteStream;

  AGMLMapController(int id) {
    _channel = MethodChannel('plugins.flutter.io/arcgis_maps:$id');
    final editorChannelName = 'plugins.flutter.io/arcgis_maps:$id:geometryEditor';
    final canUndoChannel = EventChannel('$editorChannelName/canUndo');
    final canRedoChannel = EventChannel('$editorChannelName/canRedo');
    final canDeleteChannel = EventChannel('$editorChannelName/canDelete');

    _mapServiceLayers = [];
    _mapLocalLayers = [];

    _channel.setMethodCallHandler((call) async {
      if (call.method == '/getSelectedFeatureInFeatureLayer') {
        final selectedLayers = call.arguments as List<dynamic>;
        _selectedLayerStreamController.add(selectedLayers);
      } else if (call.method == '/getSelectedGraphic') {
        final attributes = jsonDecode(call.arguments as String) as Map<String, dynamic>;
        _selectedGraphicStreamController.add(attributes);
      }
    });

    canUndoStream = canUndoChannel.receiveBroadcastStream().map((event) => event as bool);
    canRedoStream = canRedoChannel.receiveBroadcastStream().map((event) => event as bool);
    canDeleteStream = canDeleteChannel.receiveBroadcastStream().map((event) => event as bool);
  }


  void onLoadServiceFeatureChannelResponse(
      AGMLFeatureServiceLayer layer,
      String response
      ) {
    if(response != AGMLChannelStatusResponseEnum.failure.name) {
      _mapServiceLayers.add(AGMLFeatureServiceLayer(
          id: response,
          url: layer.url,
          viewPoint: layer.viewPoint
      ));
      _onChangedMapServiceLayersStreamController.add(_mapServiceLayers);
    }
  }

  void onLoadLocalFeatureChannelResponse(
      AGMLLocalFeatureLayer layer,
      String response
      ) {
    if(response != AGMLChannelStatusResponseEnum.failure.name) {
      _mapLocalLayers.add(AGMLLocalFeatureLayer(
        id: response,
        path: layer.path,
        viewPoint: layer.viewPoint
      ));
      _onChangedMapLocalLayersStreamController.add(_mapLocalLayers);
    }
  }

  void onRemoveAllFeatureChannelResponse() {
    _mapServiceLayers = [];
    _onChangedMapServiceLayersStreamController.add(_mapServiceLayers);
    _mapLocalLayers = [];
    _onChangedMapLocalLayersStreamController.add(_mapLocalLayers);
  }

  void onRemoveFeatureChannelResponse(
      AbstractAGMLFeatureLayer layer,
      String response
      ) {
    if(response == AGMLChannelStatusResponseEnum.success.name) {
      if(layer.runtimeType == AGMLLocalFeatureLayer) {
        _mapServiceLayers.remove(layer);
        _onChangedMapServiceLayersStreamController.add(_mapServiceLayers);
      } else {
        _mapLocalLayers.remove(layer);
        _onChangedMapLocalLayersStreamController.add(_mapLocalLayers);
      }
    }
  }


  //? -----------------------------
  //? --------Layer methods--------
  //? -----------------------------

  //? From Feature tablet service or Esri Portal service

  Future<void> loadServiceFeatureTable(AGMLFeatureServiceLayer arcGISMapServiceFeatureLayer) async {
    const method = '/loadServiceFeatureTable';

    try {
      final channelResponse = await _channel.invokeMethod(method, arcGISMapServiceFeatureLayer.toMap()) as String;
      onLoadServiceFeatureChannelResponse(arcGISMapServiceFeatureLayer, channelResponse);
    } on PlatformException catch(e) {
      if(kDebugMode) print(e);
    }
  }

  Future<void> loadPortalItemFeatureLayer(AGMLPortalItem agmlPortalItem) async {
    const method = '/loadPortalItemFeatureLayer';

    try {
      final channelResponse = await _channel.invokeMethod(method, agmlPortalItem.toJson()) as String;
      onLoadServiceFeatureChannelResponse(
          AGMLFeatureServiceLayer(
              url: agmlPortalItem.url,
              viewPoint: agmlPortalItem.viewPoint
          ),
          channelResponse
      );
    } on PlatformException catch(e) {
      if(kDebugMode) print(e);
    }
  }

  Future<void> loadArcGISOnlinePortalItemFeatureLayer(AGMLArcGISOnlinePortalItem agmlArcGISOnlinePortalItem) async {
    const method = '/loadArcGISOnlinePortalItemFeatureLayer';

    try {
      final channelResponse = await _channel.invokeMethod(method, agmlArcGISOnlinePortalItem.toMap()) as String;
      onLoadServiceFeatureChannelResponse(
          AGMLFeatureServiceLayer(
              url: 'https://www.arcgis.com/apps/mapviewer/index.html?layers=${agmlArcGISOnlinePortalItem.itemID}',
              viewPoint: agmlArcGISOnlinePortalItem.viewPoint
          ),
          channelResponse
      );
    } on PlatformException catch(e) {
      if(kDebugMode) print(e);
    }
  }

  //? From loca files type GeoDatabase, GeoPackage or Shapefile

  Future<void> loadGeoDatabase(AGMLGeodatabase agmlGeodatabase) async {
    const method = '/loadGeoDatabaseFeatureLayer';

    try {
      final channelResponse = await _channel.invokeMethod(method, agmlGeodatabase.toJson()) as String;
      onLoadLocalFeatureChannelResponse(AGMLLocalFeatureLayer(path: agmlGeodatabase.path!), channelResponse);
    } catch(e) {
      if(kDebugMode) print(e);
    }
  }

  Future<void> loadMobileMapPackage(AGMLMobileMapPackage agmlGeodatabase) async {
    const method = '/loadMobileMapPackage';

    try {
      final channelResponse = await _channel.invokeMethod(method, agmlGeodatabase.toJson()) as String;
      onLoadLocalFeatureChannelResponse(AGMLLocalFeatureLayer(path: agmlGeodatabase.path!), channelResponse);
    } catch(e) {
      if(kDebugMode) print(e);
    }
  }

  Future<void> loadSyncGeodatabase(AGMLGeodatabase agmlGeodatabase) async {
    const method = '/loadSyncGeodatabase';

    try {
      final channelResponse = await _channel.invokeMethod(method, agmlGeodatabase.toJson()) as String;
      onLoadLocalFeatureChannelResponse(AGMLLocalFeatureLayer(path: agmlGeodatabase.path!), channelResponse);
    } catch(e) {
      if(kDebugMode) print(e);
    }
  }

  Future<void> loadGeoPackageFeatureLayer(AGMLLocalGeopackage agmlLocalGeodatabase) async {
    const method = '/loadGeoPackageFeatureLayer';

    try {
      final channelResponse = await _channel.invokeMethod(method, agmlLocalGeodatabase.toJson()) as String;
      onLoadLocalFeatureChannelResponse(agmlLocalGeodatabase, channelResponse);
    } on PlatformException catch(e) {
      if(kDebugMode) print(e);
    }
  }

  Future<void> loadShapefileFeatureLayer(AGMLLocalShapefile agmlLocalShapefile) async {
    const method = '/loadShapefileFeatureLayer';

    try {
      final channelResponse = await _channel.invokeMethod(method, agmlLocalShapefile.toJson()) as String;
      onLoadLocalFeatureChannelResponse(agmlLocalShapefile, channelResponse);
    } on PlatformException catch(e) {
      if(kDebugMode) print(e);
    }
  }

  //? Remove Feature layer from map

  Future<void> removeAllFeatureLayer() async {
    const method = '/removeAllFeatureLayers';

    try {
      await _channel.invokeMethod(method);
      onRemoveAllFeatureChannelResponse();
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
    }
  }

  Future<void> removeFeatureLayer(AbstractAGMLFeatureLayer layer) async {
    const method = '/removeFeatureLayer';

    try {
      final channelResponse = await _channel.invokeMethod(method, layer.id) as String;
      onRemoveFeatureChannelResponse(layer, channelResponse);
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
    }
  }


  //? Selection Feature layer

  Future<void> setSelectedFeatureLayer(AGMLSelectedLayerArguments arguments) async {
    const method = '/setSelectedFeatureLayer';

    try {
      final channelResponse = await _channel.invokeMethod(method, arguments.toJson());
      if (kDebugMode) {
        print(channelResponse);
      }
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
    }
  }


  //? Manage Map

  void zoomIn() {
    const method = '/zoomIn';
    try {
      _channel.invokeMethod(method);
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
    }
  }

  void zoomOut() {
    const method = '/zoomOut';
    try {
      _channel.invokeMethod(method);
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
    }
  }

  void setViewPoint(AGMLViewPoint viewPoint) {
    const method = '/setViewPoint';
    try {
      _channel.invokeMethod(method, viewPoint.toJson());
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
    }
  }

  void setViewPoint4326(AGMLViewPoint viewPoint) {
    const method = '/setViewPoint4326';
    try {
      _channel.invokeMethod(method, viewPoint.toJson());
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
    }
  }

  void startLocation() {
    const method = '/startLocation';
    try {
      _channel.invokeMethod(method);
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
    }
  }

  void stopLocation() {
    const method = '/stopLocation';
    try {
      _channel.invokeMethod(method);
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
    }
  }

  void autoPaneModeCenterLocation() {
    const method = '/autoPaneModeCenterLocation';
    try {
      _channel.invokeMethod(method);
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
    }
  }

  void autoPaneModeInitNavigationMode() {
    const method = '/autoPaneModeInitNavigationMode';
    try {
      _channel.invokeMethod(method);
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
    }
  }

  void autoPaneModeCompassNavigation() {
    const method = '/autoPaneModeCompassNavigation';
    try {
      _channel.invokeMethod(method);
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
    }
  }

  Future<AGMLViewPoint?> getLocation4326() async {
    const method = '/getLocation4326';
    try {
      final response = await _channel.invokeMethod(method) as String;
      final location = AGMLViewPoint.fromJson(jsonDecode(response));
      return location;
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
      return null;
    }
  }

  Future<AGMLViewPoint?> getLocation9377() async {
    const method = '/getLocation9377';
    try {
      final response = await _channel.invokeMethod(method) as String;
      final location = AGMLViewPoint.fromJson(jsonDecode(response));
      return location;
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
      return null;
    }
  }

  Future<AGMLViewPoint?> getLocation9377AndSetPoint() async {
    const method = '/getLocation9377AndSetPoint';
    try {
      final response = await _channel.invokeMethod(method) as String;
      final location = AGMLViewPoint.fromJson(jsonDecode(response));
      return location;
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
      return null;
    }
  }

  void setPoint4326(AGMLViewPoint viewPoint, {required Color color}) async {
    const method = '/setPoint4326';
    try {
      final Map<String, dynamic> arguments = {
        ...viewPoint.toJson(),
        'colorR': color.red,
        'colorG': color.green,
        'colorB': color.blue,
        'colorA': color.alpha,
      };
      _channel.invokeMethod(method, viewPoint.toJson());
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
      return null;
    }
  }

  void setPoint9377(AGMLViewPoint viewPoint, {required Color color}) async {  
    const method = '/setPoint9377';
    try {
      final Map<String, dynamic> arguments = {
        ...viewPoint.toJson(),
        'colorR': color.red,
        'colorG': color.green,
        'colorB': color.blue,
        'colorA': color.alpha,
      };
      //TODO: Validar el envio de parámetros, el color seleccionado no está funcionando y siempre queda cyan
      _channel.invokeMethod(method, arguments);
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
      return null;
    }
  }

  void setPointCurrentLocation({Map<String, dynamic>? attributes}) async {
    const method = '/setPointCurrentLocation';
    try {
      _channel.invokeMethod(method, attributes);
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
      return null;
    }
  }

  Future<Map<String, dynamic>> queryData(String queryString) async {
    const method = '/queryData';
    try {
      final Map<dynamic, dynamic>? response =
          await _channel.invokeMethod(method, queryString);

      return response?.cast<String, dynamic>() ?? {};
    } on PlatformException catch (e) {
      if (kDebugMode) print(e);
      return {};
    }
  }

  void startEditing(AGMLGeometryTypeEnum editType, {AGMLColor? color}) {
    const method = '/startEditing';
    try {
      final payload = <String, dynamic>{
        'editType': editType.getString(),
        ...?color?.toMap(),
      };
      _channel.invokeMethod(method, payload);
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
    }
  }

  /// Inicia la edición cargando una geometría existente en el editor, de forma
  /// que el usuario pueda ajustar sus vértices (en vez de dibujar desde cero).
  void startEditingWithGeometry(AGMLGeometryInterface geometry, {AGMLColor? color}) {
    startEditingWithRawGeometry(geometry.toJson(), color: color);
  }

  /// Igual que [startEditingWithGeometry] pero recibe el JSON de la geometría
  /// tal cual (formato ArcGIS). Útil cuando la geometría proviene del nativo
  /// (p. ej. un feature del mapa) y no conviene reconstruirla en Dart, para
  /// evitar incompatibilidades de tipos (p. ej. `wkid` entero vs double).
  void startEditingWithRawGeometry(Map<String, dynamic> geometryJson, {AGMLColor? color}) {
    const method = '/startEditingWithGeometry';
    try {
      final payload = Map<String, dynamic>.from(geometryJson);
      if (color != null) {
        payload.addAll(color.toMap());
      }
      _channel.invokeMethod(method, payload);
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
    }
  }

  Future<AGMLGeometryInterface> completeEditing({spatialReferenceCode = 4326}) async {
    const method = '/completeEditing';
    try {
      final result = await _channel.invokeMapMethod(method, spatialReferenceCode);
      if(result == null) {
        throw Exception('No geometry data on completeEditing');
      }

      final completeGeometry = AGMLCompleteGeometry.fromJson(result);

      switch(completeGeometry.geometryType) {
        case AGMLGeometryTypeEnum.point:
          return AGMLPoint.fromJson({...completeGeometry.data, 'featuresAttributes': completeGeometry.featuresAttributes});
        case AGMLGeometryTypeEnum.polyline:
          return AGMLPolyline.fromJson({...completeGeometry.data, 'featuresAttributes': completeGeometry.featuresAttributes});
        case AGMLGeometryTypeEnum.polygon:
          return AGMLPolygon.fromJson({...completeGeometry.data, 'featuresAttributes': completeGeometry.featuresAttributes});
        case AGMLGeometryTypeEnum.multipoint:
          return AGMLMultipoint.fromJson({...completeGeometry.data, 'featuresAttributes': completeGeometry.featuresAttributes});
        default:
          throw Exception('Invalid geometry type: ${completeGeometry.geometryType}');
      }
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
      throw Exception(e);
    }
  }

  void cancelEditing() {
    const method = '/cancelEditing';
    try {
      _channel.invokeMethod(method);
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
    }
  }

  void addGeometry(AGMLGeometryInterface geometry, {Map<String, dynamic>? attributes}) {
    const method = '/addGeometry';
    try {
      final payload = Map<String, dynamic>.from(geometry.toJson());
      if (attributes != null) {
        payload['_graphicAttributes'] = attributes;
      }
      _channel.invokeMethod(method, payload);
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
      throw Exception(e);
    }
  }

  void removeGeometry(AGMLGeometryInterface geometry) {
    const method = '/removeGeometry';
    try {
      _channel.invokeMethod(method, geometry.toJson());
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
      throw Exception(e);
    }
  }

  void removeAllGeometries(){
    const method = '/removeAllGeometries';
    try {
      _channel.invokeMethod(method);
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
      throw Exception(e);
    }
  }

  /// Selecciona (resalta) el gráfico cuyo atributo `cadastralCode` coincide con
  /// [code], deselecciona los demás y centra/encaja la vista del mapa en su
  /// geometría. No hace nada si no existe un gráfico con ese código.
  void selectGraphicByCode(String code) {
    const method = '/selectGraphicByCode';
    try {
      _channel.invokeMethod(method, code);
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
    }
  }

  /// Selecciona (resalta) en la capa activa el feature cuyo campo [field] es
  /// igual a [code], deselecciona lo anterior y centra/encaja la vista en su
  /// geometría. Para canales existentes del mapa (que no tienen gráfico propio).
  void selectFeatureByCode(String code, {String field = 'DIDENTIF'}) {
    const method = '/selectFeatureByCode';
    try {
      _channel.invokeMethod(method, {'code': code, 'field': field});
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
    }
  }

  /// Oculta de forma persistente (definitionExpression) los features de la capa
  /// activa cuyo campo [field] esté en [codes]. Útil para ocultar los canales
  /// cuya geometría se clonó/editó. Lista vacía → muestra todos los features.
  void hideFeaturesByCodes(List<String> codes, {String field = 'DIDENTIF'}) {
    const method = '/hideFeaturesByCodes';
    try {
      _channel.invokeMethod(method, {'codes': codes, 'field': field});
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
    }
  }

  void undoGeometry() {
    const method = '/undoGeometry';
    try {
      _channel.invokeMethod(method);
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
      throw Exception(e);
    }
  }

  void redoGeometry() {
    const method = '/redoGeometry';
    try {
      _channel.invokeMethod(method);
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
      throw Exception(e);
    }
  }

  void deleteSelectedGeometryElement() {
    const method = '/deleteSelectedGeometryElement';
    try {
      _channel.invokeMethod(method);
    } on PlatformException catch (e) {
      if(kDebugMode) print(e);
      throw Exception(e);
    }
  }
}