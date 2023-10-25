
import 'package:arcgis_maps/pigeons/auth/auth_pigeon.g.dart';

class AGMLAuthManagerHandler implements AGMLAuthApiHandler {

  final Function(bool state) authStateListener;

  AGMLAuthManagerHandler(this.authStateListener);

  @override
  void oAuthUserState(bool state) {
    authStateListener(state);
  }

}