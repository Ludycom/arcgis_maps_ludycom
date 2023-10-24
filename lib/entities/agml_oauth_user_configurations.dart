
import '../pigeons/auth/auth_pigeon.g.dart';

class AGMLOAuthUserConfigurations {
  final String portalUrl;
  final String clientId;
  final String redirectUrl;

  AGMLOAuthUserConfigurations({
    required this.portalUrl,
    required this.clientId,
    required this.redirectUrl
  });

  OAuthUserConfigurations toPigeon() => OAuthUserConfigurations(
    portalUrl: portalUrl,
    clientId: clientId,
    redirectUrl:redirectUrl
  );

}