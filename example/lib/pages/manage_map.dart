import 'package:arcgis_maps/utils/agml_auth_manager.dart';
import 'package:flutter/material.dart';

import 'package:flutter_dotenv/flutter_dotenv.dart';

import 'package:arcgis_maps/widgets/agml_map.dart';
import 'package:arcgis_maps/entities/agml_params.dart';
import 'package:arcgis_maps/utils/agml_controller.dart';



class ManageMapPage extends StatefulWidget {

  const ManageMapPage({super.key});

  @override
  State<ManageMapPage> createState() => _ManageMapPageState();
}

class _ManageMapPageState extends State<ManageMapPage> {

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
            'Manage Map',
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
              padding: const EdgeInsets.only(right: 5, bottom: 25),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  TextButton(
                    style: const ButtonStyle(
                      backgroundColor: MaterialStatePropertyAll<Color>(Colors.blue),
                    ),
                    child: const Icon(Icons.zoom_in_rounded, color: Colors.white),
                    onPressed: () => mapController.zoomIn(),
                  ),
                  TextButton(
                    style: const ButtonStyle(
                      backgroundColor: MaterialStatePropertyAll<Color>(Colors.blue)
                    ),
                    child: const Icon(Icons.zoom_out_rounded, color: Colors.white),
                    onPressed: () => mapController.zoomOut(),
                  ),
                  TextButton(
                    style: const ButtonStyle(
                      backgroundColor: MaterialStatePropertyAll<Color>(Colors.blue)
                    ),
                    child: const Icon(Icons.location_searching_rounded, color: Colors.white),
                    onPressed: () => mapController.startLocation(),
                  ),
                  TextButton(
                    style: const ButtonStyle(
                      backgroundColor: MaterialStatePropertyAll<Color>(Colors.blue)
                    ),
                    child: const Icon(Icons.location_disabled_rounded, color: Colors.white),
                    onPressed: () => mapController.startLocation(),
                  ),
                  TextButton(
                    style: const ButtonStyle(
                      backgroundColor: MaterialStatePropertyAll<Color>(Colors.blue)
                    ),
                    child: const Icon(Icons.my_location_rounded, color: Colors.white),
                    onPressed: () => mapController.autoPaneModeCenterLocation(),
                  ),
                  TextButton(
                    style: const ButtonStyle(
                      backgroundColor: MaterialStatePropertyAll<Color>(Colors.blue)
                    ),
                    child: const Icon(Icons.navigation_rounded, color: Colors.white),
                    onPressed: () => mapController.autoPaneModeCompassNavigation(),
                  ),
                  TextButton(
                    style: const ButtonStyle(
                      backgroundColor: MaterialStatePropertyAll<Color>(Colors.blue)
                    ),
                    child: const Icon(Icons.compass_calibration_rounded, color: Colors.white),
                    onPressed: () => mapController.autoPaneModeCompassNavigation(),
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