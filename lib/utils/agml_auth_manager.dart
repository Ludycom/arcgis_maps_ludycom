
import 'package:arcgis_maps/pigeons/auth/auth_pigeon.g.dart';

class AGMLAuthManager {

  final authApi = AGMLAuthApi();

  void setApiKey(String apiKey) {
    authApi.setApiKey(apiKey);
  }

}