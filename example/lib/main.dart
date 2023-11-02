import 'package:arcgis_maps_example/pages/generate_geodatabase_replica_from_feature.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

import 'package:flutter_dotenv/flutter_dotenv.dart';


import 'package:arcgis_maps/entities/agml_oauth_user_configurations.dart';
import 'package:arcgis_maps/pigeons/auth/auth_pigeon.g.dart';
import 'package:arcgis_maps/utils/agml_auth_manager.dart';
import 'package:arcgis_maps/utils/agml_auth_manager_handler.dart';

import 'package:arcgis_maps_example/pages/basic_map.dart';
import 'package:arcgis_maps_example/pages/manage_map.dart';
import 'package:arcgis_maps_example/pages/download_from_portal.dart';

import 'package:arcgis_maps_example/pages/load_feature_tablet_service.dart';
import 'package:arcgis_maps_example/pages/load_local_files.dart';
import 'package:arcgis_maps_example/pages/load_portal_feature_layer.dart';

import 'package:arcgis_maps_example/pages/select_features_in_feature_layer.dart';
import 'package:arcgis_maps_example/utils/page_routes_enum.dart';



Future<void> main() async {
  await dotenv.load(fileName: '.env');
  runApp(const MyApp());
}  

class MyApp extends StatelessWidget {

  const MyApp({super.key});

  void navigateToBasicMap(BuildContext context) {
    Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => const BasicMapPage()),
    );
  }

  @override
  Widget build(BuildContext context) {

    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorSchemeSeed: const Color(0xff243edd),
        appBarTheme: const AppBarTheme(
          backgroundColor: Color(0xff243edd),
        ),
      ),
      title: 'ArcGIS Maps',
      initialRoute: PageRoutesEnum.home.path,
      routes: {
        PageRoutesEnum.home.path: (context) => _HomePage(),
        PageRoutesEnum.basic_map.path: (context) => const BasicMapPage(),
        PageRoutesEnum.load_feature_tablet_service.path: (context) => const LoadFeatureTabletServicePage(),
        PageRoutesEnum.load_portal_feature_layer.path: (context) => const LoadPortalFeatureLayerPage(),
        PageRoutesEnum.download_form_portal.path: (context) => const DownloadFormPortalPage(),
        PageRoutesEnum.load_local_files.path: (context) => const LoadLocalFilesPage(),
        PageRoutesEnum.select_features_in_feature_layer.path: (context) => const SelectFeaturesInFeatureLayerPage(),
        PageRoutesEnum.manage_map.path: (context) => const ManageMapPage(),
        PageRoutesEnum.generate_geodatabase_replica_from_feature_service.path: (context) => const GenerateGeodatabaseReplicaFromFeaturePage()
      },
    );
  }
}


class _HomePage extends StatefulWidget {

  @override
  State<_HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<_HomePage> {

  bool authState = false;

  @override
  void initState() {
    super.initState();

    AGMLAuthApiHandler.setup(AGMLAuthManagerHandler((state) {
      setState(() { authState = state; });
    }));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const FittedBox(
          child: Text(
            'ArcGIS Maps by Ludycom S.A.S., Jonathan Poveda',
            style: TextStyle(color: Colors.white),
          )
        ),
        actions: [
          if(authState) const Icon(Icons.check_box_rounded, color: Colors.green)
          else const Icon(Icons.warning_rounded, color: Colors.yellow),
          TextButton(
            onPressed: () {
              final authManager = AGMLAuthManager();
              final configs = AGMLOAuthUserConfigurations(
                  clientId: dotenv.env['OAUTH_CLIENT_ID'] ?? '',
                  portalUrl: dotenv.env['PORTAL_URL'] ?? '',
                  redirectUrl: dotenv.env['OAUTH_REDIRECT_URI'] ?? ''
              );

              try {
                authManager.authApi.oAuthUser(
                  configs.toPigeon(),
                  dotenv.env['ARCGIS_USER'] ?? '',
                  dotenv.env['ARCGIS_PASSWORD'] ?? ''
                );
              } catch(e) {
                if(kDebugMode) print(e);
              }
            },
            child: const Text(
              'OAuth 2.0',
              style: TextStyle(color: Colors.white),
            )
          )
        ],
      ),
      body: ListView.custom(
        childrenDelegate: SliverChildListDelegate.fixed(
          [
            Card(
              child: ListTile(
                leading: const Icon(Icons.map),
                title: const Text('Basic map'),
                onTap: () => Navigator.of(context).pushNamed(PageRoutesEnum.basic_map.path),
              ),
            ),
            Card(
              child: ListTile(
                leading: const Icon(Icons.map),
                title: const Text('Load feature tablet service'),
                onTap: () => Navigator.of(context).pushNamed(PageRoutesEnum.load_feature_tablet_service.path),
              ),
            ),
            Card(
              child: ListTile(
                leading: const Icon(Icons.map),
                title: const Text('Load Portal feature layer'),
                onTap: () => Navigator.of(context).pushNamed(PageRoutesEnum.load_portal_feature_layer.path),
              ),
            ),
            Card(
              child: ListTile(
                leading: const Icon(Icons.map),
                title: const Text('Download from Portal'),
                onTap: () => Navigator.of(context).pushNamed(PageRoutesEnum.download_form_portal.path),
              ),
            ),
            Card(
              child: ListTile(
                leading: const Icon(Icons.map),
                title: const Text('Load GeoDatabase, Geopackage, ShapeFile and Mobile Map Package'),
                onTap: () => Navigator.of(context).pushNamed(PageRoutesEnum.load_local_files.path),
              ),
            ),
            Card(
              child: ListTile(
                leading: const Icon(Icons.cloud_sync_rounded),
                title: const Text('Generate FeatureService replica, load geodatabase and sync'),
                onTap: () => Navigator.of(context).pushNamed(PageRoutesEnum.generate_geodatabase_replica_from_feature_service.path),
              ),
            ),
            Card(
              child: ListTile(
                leading: const Icon(Icons.touch_app_rounded),
                title: const Text('Select a feature layer and get layer data'),
                onTap: () => Navigator.of(context).pushNamed(PageRoutesEnum.select_features_in_feature_layer.path),
              ),
            ),
            Card(
              child: ListTile(
                leading: const Icon(Icons.back_hand_rounded),
                title: const Text('Manage map'),
                onTap: () => Navigator.of(context).pushNamed(PageRoutesEnum.manage_map.path),
              ),
            ),
          ]
        )
      ),
    );
  }
}