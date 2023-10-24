import 'package:arcgis_maps/utils/agml_auth_manager.dart';
import 'package:flutter/material.dart';

import 'package:flutter_dotenv/flutter_dotenv.dart';


import 'package:arcgis_maps/entities/features/agml_local_shapefile.dart';
import 'package:arcgis_maps/entities/features/agml_local_geopackage.dart';
import 'package:arcgis_maps/entities/features/agml_local_geodatabase.dart';

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
    AGMLAuthManager().setApiKey(dotenv.env['API_KEY'] ?? '');
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
                      mapController.loadGeoDatabaseFeatureLayer(
                        AGMLLocalGeodatabase(
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
                      mapController.loadGeoDatabaseFeatureLayer(
                        AGMLLocalGeodatabase(
                          path: '/storage/emulated/0/Android/data/com.ludycom.arcgis_maps_example/filestest.geodatabase',
                          viewPoint: AGMLViewPoint(
                            latitude: 34.0772, 
                            longitude: -118.7989,
                            scale: 600000.0
                          )
                        ) 
                      );
                    },
                    child: const Text(
                      'Load Geodatabase From FeatureServer',
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
                ],
              ),
            ),
          )
        ],
      ),
    );
  }
}