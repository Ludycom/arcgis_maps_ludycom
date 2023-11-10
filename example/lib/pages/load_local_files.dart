import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

import 'package:flutter_dotenv/flutter_dotenv.dart';


import 'package:arcgis_maps/utils/agml_auth_manager.dart';
import 'package:arcgis_maps/entities/agml_geodatabase.dart';
import 'package:arcgis_maps/entities/agml_mobile_map_package.dart';

import 'package:arcgis_maps/entities/features/agml_local_shapefile.dart';
import 'package:arcgis_maps/entities/features/agml_local_geopackage.dart';

import 'package:arcgis_maps/widgets/agml_map.dart';
import 'package:arcgis_maps/entities/agml_params.dart';
import 'package:arcgis_maps/utils/agml_controller.dart';
import 'package:arcgis_maps/entities/agml_view_point.dart';



class LoadLocalFilesPage extends StatefulWidget {

  const LoadLocalFilesPage({super.key});

  @override
  State<LoadLocalFilesPage> createState() => _LoadLocalFilesPageState();
}

class _LoadLocalFilesPageState extends State<LoadLocalFilesPage> {
  
  late final AGMLMapController mapController;

  @override
  void initState() {
    // AGMLAuthManager().setApiKey(dotenv.env['API_KEY'] ?? '');
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const FittedBox(
          child: Text(
            'Load local files',
            style: TextStyle(color: Colors.white),
          )
        ),
      ),
      body: Stack(
        children: [
          AGMLMap(
            creationParams: AGMLCreationParams(
              basemapStyle: AGMLBasemapStyleEnum.arcGISTopographic
            ),
            onMapCreated: (controller) => mapController = controller,
          ),
          Align(
            alignment: Alignment.bottomRight,
            child: Padding(
              padding: const EdgeInsets.all(8.0),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  TextButton(
                    style: const ButtonStyle(
                      backgroundColor: MaterialStatePropertyAll<Color>(Colors.blue)
                    ),
                    onPressed: () {
                      mapController.loadGeoDatabase(
                        AGMLGeodatabase(
                          path: '/storage/emulated/0/Android/data/com.ludycom.arcgis_maps_example/files/Portal Items/cb1b20748a9f4d128dad8a87244e3e37/LA_Trails.geodatabase',
                          viewPoint: AGMLViewPoint(
                            latitude: 34.0772, 
                            longitude: -118.7989,
                            scale: 600000.0
                          )
                        ) 
                      );
                    },
                    child: const Text(
                      'Load Geodatabase',
                      style: TextStyle(color: Colors.white)
                    )
                  ),
                  TextButton(
                    style: const ButtonStyle(
                      backgroundColor: MaterialStatePropertyAll<Color>(Colors.blue)
                    ),
                    onPressed: () {
                      mapController.loadMobileMapPackage(
                        AGMLMobileMapPackage(
                          // path: '/storage/emulated/0/Android/data/com.ludycom.arcgis_maps_example/files/Portal Items/260eb6535c824209964cf281766ebe43/SanFrancisco.mmpk',
                          // path: '/storage/emulated/0/Android/data/com.ludycom.arcgis_maps_example/files/Portal Items/0f9441926c0245c2bbd59419b91e5bc5/Mapa_Base_CVC.mmpk',
                          path: '/storage/emulated/0/Android/data/com.ludycom.arcgis_maps_example/files/Portal Items/24df54d524ef43f9a559b4ae8232aaf3/Mapa_base_CVC.mmpk',
                          viewPoint: AGMLViewPoint(
                            latitude: 34.0772, 
                            longitude: -118.7989,
                            scale: 600000.0
                          )
                        ) 
                      );
                    },
                    child: const Text(
                      'Load Mobile Map Package',
                      style: TextStyle(color: Colors.white)
                    )
                  ),
                  TextButton(
                    style: const ButtonStyle(
                      backgroundColor: MaterialStatePropertyAll<Color>(Colors.blue)
                    ),
                    onPressed: () {
                      mapController.loadGeoPackageFeatureLayer(
                        AGMLLocalGeopackage(
                          path: '/storage/emulated/0/Android/data/com.ludycom.arcgis_maps_example/files/Portal Items/68ec42517cdd439e81b036210483e8e7/AuroraCO.gpkg',
                          viewPoint: AGMLViewPoint(
                            latitude: 39.7294, 
                            longitude: -104.8319,
                            scale: 500000.0
                          )
                        )
                      );
                    },
                    child: const Text(
                      'Load Geopackage',
                      style: TextStyle(color: Colors.white)
                    )
                  ),
                  TextButton(
                    style: const ButtonStyle(
                      backgroundColor: MaterialStatePropertyAll<Color>(Colors.blue)
                    ),
                    onPressed: () {
                      mapController.loadShapefileFeatureLayer(
                        AGMLLocalShapefile(
                          path: '/storage/emulated/0/Android/data/com.ludycom.arcgis_maps_example/files/Portal Items/15a7cbd3af1e47cfa5d2c6b93dc44fc2/ScottishWildlifeTrust_ReserveBoundaries_20201102.shp',
                          viewPoint: AGMLViewPoint(
                            latitude: 56.641344, 
                            longitude: -3.889066,
                            scale: 6000000.0                            
                          )
                        )
                      );
                    },
                    child: const Text(
                      'Load ShapeFile',
                      style: TextStyle(color: Colors.white)
                    )
                  ),
                  
                  TextButton(
                    style: const ButtonStyle(
                      backgroundColor: MaterialStatePropertyAll<Color>(Colors.blue)
                    ),
                    onPressed: () {
                      mapController.removeAllFeatureLayer();
                    }, 
                    child: const Text(
                      'Remove Feature layers',
                      style: TextStyle(color: Colors.white)
                    )
                  ),

                  TextButton(
                    style: const ButtonStyle(
                        backgroundColor: MaterialStatePropertyAll<Color>(Colors.blue)
                    ),
                    child: const Icon(Icons.location_history, color: Colors.white),
                    onPressed: () async {
                      final location = await mapController.getLocation();
                      if(kDebugMode) print(location);
                      final location9377 = await mapController.getLocation9377AndSetPoint();
                      if(kDebugMode) print(location9377);
                    },
                  ),
                  TextButton(
                    style: const ButtonStyle(
                      backgroundColor: MaterialStatePropertyAll<Color>(Colors.blue)
                    ),
                    child: const Icon(Icons.location_searching_rounded, color: Colors.white),
                    onPressed: () async {
                      mapController.startLocation();

                    },
                  ),
                  TextButton(
                    style: const ButtonStyle(
                      backgroundColor: MaterialStatePropertyAll<Color>(Colors.blue)
                    ),
                    child: const Icon(Icons.circle, color: Colors.white),
                    onPressed: () async {
                      mapController.setPoint4326(AGMLViewPoint(latitude: 4.41761268, longitude: -76.07825769));
                      mapController.setViewPoint4326(AGMLViewPoint(latitude: 4.41761268, longitude: -76.07825769));
                    },
                  ),
                ],
              ),
            ),
          )
        ],
      ),
    );
  }
}