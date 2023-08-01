import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';

import 'package:flutter_dotenv/flutter_dotenv.dart';


import 'package:arcgis_maps/widgets/agml_map.dart';
import 'package:arcgis_maps/entities/agml_params.dart';
import 'package:arcgis_maps/entities/agml_view_point.dart';
import 'package:arcgis_maps/entities/features/agml_portal_item.dart';
import 'package:arcgis_maps/entities/features/agml_arcgis_online_portal_item.dart';
import 'package:arcgis_maps/utils/agml_controller.dart';



class LoadPortalFeatureLayerPage extends StatefulWidget {
  
  const LoadPortalFeatureLayerPage({super.key});

  @override
  State<LoadPortalFeatureLayerPage> createState() => _LoadPortalFeatureLayerPageState();
}

class _LoadPortalFeatureLayerPageState extends State<LoadPortalFeatureLayerPage> {

  late final AGMLMapController mapController;

  @override
  Widget build(BuildContext context) {

    return Scaffold(
      appBar: AppBar(
        title: const FittedBox(
          child: Text(
            'Load portal feature layer',
            style: TextStyle(color: Colors.white),
          )
        ),
      ),
      body: Stack(
        children: [
          AGMLMap(
            creationParams: AGMLCreationParams(
              apiKey: dotenv.env['API_KEY']
            ),
            onMapCreated: (controller) => mapController = controller,
            onChangeMapServiceLayers: (layers) {
              if (kDebugMode) {
                print(layers);
              }
            },
          ),
          Align(
            alignment: Alignment.bottomRight,
            child: Padding(
              padding: const EdgeInsets.all(8.0),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.end,
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  TextButton(
                    style: const ButtonStyle(
                      backgroundColor: MaterialStatePropertyAll<Color>(Colors.blue)
                    ),
                    onPressed: () {
                      mapController.loadPortalItemFeatureLayer(
                        AGMLPortalItem(
                          url: 'https://www.arcgis.com/home/item.html?id=68ec42517cdd439e81b036210483e8e7',
                          viewPoint: AGMLViewPoint(
                            latitude: 39.7294, 
                            longitude: -104.8319,
                            scale: 500000.0
                          )                     
                        )
                      );
                    }, 
                    child: const Text(
                      'Load Portal Feature Layer',
                      style: TextStyle(color: Colors.white)
                    )
                  ),
                  TextButton(
                    style: const ButtonStyle(
                      backgroundColor: MaterialStatePropertyAll<Color>(Colors.blue)
                    ),
                    onPressed: () {
                      mapController.loadArcGISOnlinePortalItemFeatureLayer(
                        AGMLArcGISOnlinePortalItem(
                          itemID: '1759fd3e8a324358a0c58d9a687a8578',
                          viewPoint: AGMLViewPoint(
                            latitude: 45.5266, 
                            longitude: -122.6219,
                            scale: 2500.0
                          )
                        )
                      );
                    }, 
                    child: const Text(
                      'Load ArcGIS Online Portal Feature Layer',
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
                      'Remove Feature Layers',
                      style: TextStyle(color: Colors.white)
                    )
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}