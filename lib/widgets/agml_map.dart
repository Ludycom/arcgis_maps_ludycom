import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/services.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/foundation.dart';


import 'package:arcgis_maps/entities/agml_params.dart';
import 'package:arcgis_maps/utils/agml_controller.dart';

import 'package:arcgis_maps/entities/features/agml_local_feature_layer.dart';
import 'package:arcgis_maps/entities/features/agml_feature_service_layer.dart';



class AGMLMap extends StatefulWidget {

  final AGMLCreationParams creationParams;
  final Function(AGMLMapController controller)? onMapCreated;
  final Function(List<dynamic> attributesList)? onLayerSelected;
  final Function(List<AGMLFeatureServiceLayer> layers)? onChangeMapServiceLayers;
  final Function(List<AGMLLocalFeatureLayer> layers)? onChangeMapLocalLayers;

  const AGMLMap({
    super.key, 
    required this.creationParams, 
    this.onMapCreated, 
    this.onLayerSelected, 
    this.onChangeMapServiceLayers, 
    this.onChangeMapLocalLayers
  });

  @override
  State<AGMLMap> createState() => _AGMLMapState();
}

class _AGMLMapState extends State<AGMLMap> {

  late AGMLMapController _controller;
  late MethodChannel _methodChannel;

  static const String _viewType = 'plugins.flutter.io/arcgis_maps';


  void initMethodChannel(int id) {
    _methodChannel = MethodChannel('$_viewType:$id:mapStatus');
    _methodChannel.setMethodCallHandler(
      (call) async {
        if(call.method == '/mapIsReady') {
          _configureController();
        }
      }
    );
  }

  @override
  void dispose() {
    _methodChannel.setMethodCallHandler(null);
    _controller.onChangedMapLocalLayersStreamController.close();
    _controller.onChangedMapServiceLayersStreamController.close();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {

    if(Platform.isAndroid) {
      return PlatformViewLink(
        surfaceFactory: (context, controller) {
          return AndroidViewSurface(
            controller: controller as AndroidViewController,
            hitTestBehavior: PlatformViewHitTestBehavior.opaque, 
            gestureRecognizers: const <Factory<OneSequenceGestureRecognizer>>{}
          );
        },
        onCreatePlatformView: (params) {
          _controller = AGMLMapController(params.id);
          initMethodChannel(params.id);

          return PlatformViewsService.initExpensiveAndroidView(
            id: params.id,
            viewType: _viewType, 
            layoutDirection: TextDirection.ltr,
            creationParams: widget.creationParams.toMap(),
            creationParamsCodec: const StandardMessageCodec(),
            onFocus: () {
              params.onFocusChanged(true);
            },
          )..addOnPlatformViewCreatedListener(params.onPlatformViewCreated)..create();
        },
        viewType: _viewType
      );
    }
    
    
    return const Center(
      child: Text('Not support platform 2.')
    );

  }

  void _configureController() {
    if(widget.onMapCreated != null) widget.onMapCreated!(_controller);
    
    _controller.selectedLayerStreamController.stream.listen((event) {
      if(widget.onLayerSelected != null) widget.onLayerSelected!(event);
    });

    _controller.onChangedMapServiceLayersStreamController.stream.listen((event) {
      if(widget.onChangeMapServiceLayers != null) widget.onChangeMapServiceLayers!(event);
    });

    _controller.onChangedMapLocalLayersStreamController.stream.listen((event) {
      if(widget.onChangeMapLocalLayers != null) widget.onChangeMapLocalLayers!(event);
    });

  }

}