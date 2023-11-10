
import 'package:arcgis_maps/entities/agml_geodatabase.dart';
import 'package:arcgis_maps/entities/agml_params.dart';
import 'package:arcgis_maps/entities/features/agml_feature_service.dart';
import 'package:arcgis_maps/utils/agml_controller.dart';
import 'package:arcgis_maps/utils/agml_download_portal_item_manager.dart';
import 'package:arcgis_maps/widgets/agml_map.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';


class GenerateGeodatabaseReplicaFromFeaturePage extends StatefulWidget {

  const GenerateGeodatabaseReplicaFromFeaturePage({super.key});

  @override
  State<GenerateGeodatabaseReplicaFromFeaturePage> createState() => _GenerateGeodatabaseReplicaFromFeaturePageState();
}

class _GenerateGeodatabaseReplicaFromFeaturePageState extends State<GenerateGeodatabaseReplicaFromFeaturePage> {

  late final AGMLDownloadPortalItemManager agmlDownloadPortalItemManager;
  late final AGMLMapController mapController;
  bool isLoading = false;
  String? errorMessage;
  AGMLGeodatabase? geodatabase;

  @override
  void initState() {
    super.initState();
    agmlDownloadPortalItemManager = AGMLDownloadPortalItemManager();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const FittedBox(
          child: Text(
            'Generate FeatureService replica, load geodatabase and sync',
            style: TextStyle(color: Colors.white),
          )
        ),
      ),
      body: Column(
        children: [
          SizedBox(
            height: 100,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                Column(
                  children: [
                    OutlinedButton(
                        onPressed: () async {
                          setState(() => isLoading = true);
                          geodatabase = await agmlDownloadPortalItemManager.generateGeodatabaseReplicaFromFeatureService(agmlFeatureServicePortal);
                          setState(() => isLoading = false);
                        },
                        child: const Text('Generate replica - Portal')
                    ),
                    OutlinedButton(
                        onPressed: () async {
                          setState(() => isLoading = true);
                          geodatabase = await agmlDownloadPortalItemManager.generateGeodatabaseReplicaFromFeatureService(agmlFeatureServiceOnline);
                          setState(() => isLoading = false);
                        },
                        child: const Text('Generate replica - ArcGIS Online')
                    ),
                  ],
                ),
                OutlinedButton(
                    onPressed: () async {
                      if(geodatabase == null) return;

                      setState(() => isLoading = true);
                      mapController.loadGeoDatabase(geodatabase!);
                      setState(() => isLoading = false);
                    },
                    child: const Text('load/edit geodatabase')
                ),
                OutlinedButton(
                    onPressed: () async {
                      if(geodatabase == null) return;

                      setState(() => isLoading = true);
                      agmlDownloadPortalItemManager.syncGeodatabaseReplicaToFeatureService(geodatabase!);
                      setState(() => isLoading = false);
                    },
                    child: const Text('Sync replica')
                ),
                if(isLoading) const SizedBox(height: 50, width: 50, child: CircularProgressIndicator())
              ],
            ),
          ),
          Expanded(
            child: AGMLMap(
              creationParams: AGMLCreationParams(
                basemapStyle: AGMLBasemapStyleEnum.arcGISTopographic
              ),
              onMapCreated: (controller) => mapController = controller,
              onChangeMapLocalLayers: (layers) {
                if(kDebugMode) print(layers);
              },
            ),
          )
        ],
      ),
    );
  }
}

final agmlFeatureServiceOnline = AGMLFeatureService(
    url: 'https://services.arcgis.com/UxVKZtnb6p8rrswX/arcgis/rest/services/Pruebas_SYNC_18oct_WFL1/FeatureServer'
);

final agmlFeatureServicePortal = AGMLFeatureService(
    // url: 'https://services2.arcgis.com/ZQgQTuoyBrtmoGdP/ArcGIS/rest/services/canyonlands_roads_trails/FeatureServer'
    url: 'https://pliga-server.cvc.gov.co/arcgis/rest/services/PRUEBAS/Prueba_sync_tradi/FeatureServer'
);