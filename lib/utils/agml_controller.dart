import 'dart:async';
import 'dart:convert';
import 'package:arcgis_maps/entities/agml_geodatabase.dart';
import 'package:arcgis_maps/entities/agml_mobile_map_package.dart';
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

import 'package:arcgis_maps/utils/enums/agml_channel_status_response_enum.dart';



class AGMLMapController {

  late MethodChannel _channel;

  final StreamController<List<dynamic>> _selectedLayerStreamController = StreamController();
  StreamController<List<dynamic>> get selectedLayerStreamController => _selectedLayerStreamController;

  late List<AGMLFeatureServiceLayer> _mapServiceLayers;
  List<AGMLFeatureServiceLayer> get mapServiceLayers => _mapServiceLayers;
  final StreamController<List<AGMLFeatureServiceLayer>> _onChangedMapServiceLayersStreamController = StreamController();
  StreamController<List<AGMLFeatureServiceLayer>> get onChangedMapServiceLayersStreamController => _onChangedMapServiceLayersStreamController;

  late List<AGMLLocalFeatureLayer> _mapLocalLayers;
  List<AGMLLocalFeatureLayer> get mapLocalLayers => _mapLocalLayers;
  final StreamController<List<AGMLLocalFeatureLayer>> _onChangedMapLocalLayersStreamController = StreamController();
  StreamController<List<AGMLLocalFeatureLayer>> get onChangedMapLocalLayersStreamController => _onChangedMapLocalLayersStreamController;



  AGMLMapController(int id) {
    _channel = MethodChannel('plugins.flutter.io/arcgis_maps:$id');

    _mapServiceLayers = [];
    _mapLocalLayers = [];

    _channel.setMethodCallHandler((call) async {
      if (call.method == '/getSelectedFeatureInFeatureLayer') {
        final selectedLayers = call.arguments as List<dynamic>;
        _selectedLayerStreamController.add(selectedLayers);
      }
    });
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
      final channelResponse = await _channel.invokeMethod(method, agmlLocalGeodatabase.toMap()) as String;
      onLoadLocalFeatureChannelResponse(agmlLocalGeodatabase, channelResponse);
    } on PlatformException catch(e) {
      if(kDebugMode) print(e);
    }
  }

  Future<void> loadShapefileFeatureLayer(AGMLLocalShapefile agmlLocalShapefile) async {
    const method = '/loadShapefileFeatureLayer';

    try {
      final channelResponse = await _channel.invokeMethod(method, agmlLocalShapefile.toMap()) as String;
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

  Future<void> setSelectedFeatureLayer(String layerId) async {
    const method = '/setSelectedFeatureLayer';

    try {
      final channelResponse = await _channel.invokeMethod(method, layerId);
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

  Future<AGMLViewPoint?> getLocation() async {
    const method = '/getLocation';
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


}