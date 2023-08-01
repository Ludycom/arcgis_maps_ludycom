import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'arcgis_maps_method_channel.dart';

abstract class ArcgisMapsPlatform extends PlatformInterface {
  /// Constructs a ArcgisMapsPlatform.
  ArcgisMapsPlatform() : super(token: _token);

  static final Object _token = Object();

  static ArcgisMapsPlatform _instance = MethodChannelArcgisMaps();

  /// The default instance of [ArcgisMapsPlatform] to use.
  ///
  /// Defaults to [MethodChannelArcgisMaps].
  static ArcgisMapsPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [ArcgisMapsPlatform] when
  /// they register themselves.
  static set instance(ArcgisMapsPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
