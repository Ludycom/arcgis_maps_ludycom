import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';


import 'package:arcgis_maps/widgets/agml_map.dart';
import 'package:arcgis_maps/entities/agml_params.dart';
import 'package:arcgis_maps/utils/agml_controller.dart';



class SetPointsPage extends StatefulWidget {

  const SetPointsPage({super.key});

  @override
  State<SetPointsPage> createState() => _SetPointsPageState();
}

class _SetPointsPageState extends State<SetPointsPage> {

  late final AGMLMapController mapController;

  @override
  void initState() {
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
                    child: const Icon(Icons.circle_rounded, color: Colors.white),
                    onPressed: () => mapController.setPointCurrentLocation(
                      attributes: {
                        "test": "test"
                      }
                    ),
                  )
                ],
              ),
            ),
          )
        ],
      ),
    );
  }
}