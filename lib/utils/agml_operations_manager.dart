
import 'dart:convert';

import 'package:arcgis_maps/entities/agml_view_point.dart';
import 'package:arcgis_maps/entities/features/agml_change_spacial_reference_params.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

class AGMLOperationsManager {

  late MethodChannel _channel;

  AGMLOperationsManager() {
    _channel = const MethodChannel('arcgis_maps');
  }

  Future<AGMLViewPoint?> changeSpacialReference(AGMLChangeSpacialReferenceParams changeSpacialReferenceParams) async {
    const method = '/changeSpacialReference';

    try {
      final channelResponse = await _channel.invokeMethod(method, changeSpacialReferenceParams.toJson()) as String;
      final newViewPoint = AGMLViewPoint.fromJson(json.decode(channelResponse));
      return newViewPoint;
    } catch (e) {
      if(kDebugMode) print(e);
      return null;
    }
  }

}