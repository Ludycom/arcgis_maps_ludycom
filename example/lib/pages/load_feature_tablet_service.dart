import 'package:flutter/material.dart';

import 'package:flutter_dotenv/flutter_dotenv.dart';


import 'package:arcgis_maps/widgets/agml_map.dart';
import 'package:arcgis_maps/utils/agml_controller.dart';
import 'package:arcgis_maps/entities/agml_params.dart';
import 'package:arcgis_maps/entities/agml_view_point.dart';
import 'package:arcgis_maps/entities/features/agml_service_feature_layer.dart';



class LoadFeatureTabletServicePage extends StatefulWidget {

  const LoadFeatureTabletServicePage({super.key});

  @override
  State<LoadFeatureTabletServicePage> createState() => _LoadFeatureTabletServicePageState();
}

class _LoadFeatureTabletServicePageState extends State<LoadFeatureTabletServicePage> {

  late final AGMLMapController mapController;

  @override
  Widget build(BuildContext context) {

    return Scaffold(
      appBar: AppBar(
        title: const FittedBox(
          child: Text(
            'Load feature tablet service',
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
                      mapController.loadServiceFeatureTable(
                        AGMLServiceFeatureLayer(
                          url: 'https://sampleserver6.arcgisonline.com/arcgis/rest/services/NapervilleShelters/FeatureServer/0',
                          viewPoint: AGMLViewPoint(
                            latitude: 41.70, 
                            longitude: -88.20
                          )
                        )
                      );
                    },
                    child: const Text(
                      'Load Feature Tablet',
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
                      'Remove Feature Tablet',
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
