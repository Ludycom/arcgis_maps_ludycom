import 'package:pigeon/pigeon.dart';


class OAuthUserConfigurations {
  final String portalUrl;
  final String clientId;
  final String redirectUrl;

  OAuthUserConfigurations({
    required this.portalUrl,
    required this.clientId,
    required this.redirectUrl
  });
}

@ConfigurePigeon(PigeonOptions(
  dartOut: 'lib/pigeons/auth/auth_pigeon.g.dart',
  dartOptions: DartOptions(),
  dartPackageName: 'AuthPigeon',

  kotlinOut: 'android/src/main/kotlin/com/ludycom/arcgis_maps/pigeons/AuthPigeon.g.kt',
  kotlinOptions: KotlinOptions(package: 'AuthPigeon'),

  //swiftOut: 'ios/Runner/Messages.g.swift',
  //swiftOptions: SwiftOptions(),
))

@FlutterApi()
abstract class AGMLAuthApiHandler {
  void oAuthUserState(bool state);
}

@HostApi()
abstract class AGMLAuthApi {

  void oAuthUser(OAuthUserConfigurations portalConfig, String username, String password);

  void setApiKey(String apiKey);

}
