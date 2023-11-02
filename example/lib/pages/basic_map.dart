import 'package:flutter/material.dart';


import 'package:arcgis_maps/widgets/agml_map.dart';
import 'package:arcgis_maps/entities/agml_params.dart';



class BasicMapPage extends StatefulWidget {

  const BasicMapPage({super.key});

  @override
  State<BasicMapPage> createState() => _BasicMapPageState();
}

class _BasicMapPageState extends State<BasicMapPage> {

  @override
  void initState() {
    //AGMLAuthManager().setApiKey(dotenv.env['API_KEY'] ?? '');
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const FittedBox(
          child: Text(
            'Basic map',
            style: TextStyle(color: Colors.white),
          )
        ),
      ),
      body: AGMLMap(
        creationParams: AGMLCreationParams(
          basemapStyle: AGMLBasemapStyleEnum.arcGISTopographic
        ),
        onMapCreated: (controller) {
          
        },
      ),
    );
  }
}