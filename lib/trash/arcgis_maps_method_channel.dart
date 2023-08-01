import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'arcgis_maps_platform_interface.dart';

/// An implementation of [ArcgisMapsPlatform] that uses method channels.
class MethodChannelArcgisMaps extends ArcgisMapsPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('arcgis_maps');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
