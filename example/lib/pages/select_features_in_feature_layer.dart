import 'package:flutter/material.dart';

import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_json_view/flutter_json_view.dart';


import 'package:arcgis_maps/entities/agml_params.dart';
import 'package:arcgis_maps/entities/agml_view_point.dart';
import 'package:arcgis_maps/entities/features/agml_arcgis_online_portal_item.dart';
import 'package:arcgis_maps/entities/features/agml_service_feature_layer.dart';

import 'package:arcgis_maps/widgets/agml_map.dart';
import 'package:arcgis_maps/utils/agml_controller.dart';



class SelectFeaturesInFeatureLayerPage extends StatefulWidget {

  const SelectFeaturesInFeatureLayerPage({super.key});

  @override
  State<SelectFeaturesInFeatureLayerPage> createState() => _SelectFeaturesInFeatureLayerPageState();
}

class _SelectFeaturesInFeatureLayerPageState extends State<SelectFeaturesInFeatureLayerPage> {
  
  late final AGMLMapController mapController;
  List<AGMLServiceFeatureLayer> serviceLayersInMap = [];
  String selectedLayerId = '';

  double layerListPosition = -185;

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
            'Select features in feature layer',
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
            onChangeMapServiceLayers: (layers) {
              setState(() { serviceLayersInMap = layers; });
            },
            onLayerSelected: (attributesList) {

              showModalBottomSheet(
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(5)),
                context: context,
                builder: (context) {
                  return Container(
                    height: 300,
                    width: MediaQuery.of(context).size.width,
                    padding: const EdgeInsets.all(5),
                    child: ListView.builder(
                      itemExtent: MediaQuery.of(context).size.width-60,
                      itemCount: attributesList.length,
                      scrollDirection: Axis.horizontal,
                      itemBuilder: (context, index) {

                        return Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10),
                          height: 290,
                          width: MediaQuery.of(context).size.width-60,
                          child: JsonView.string(
                            attributesList[index],
                            theme: const JsonViewTheme(
                              viewType: JsonViewType.collapsible,                            
                              separator: Padding(
                                padding: EdgeInsets.symmetric(horizontal: 8.0),
                                child: Icon(
                                  Icons.arrow_right_alt_outlined,
                                  size: 20,
                                  color: Colors.green,
                                ),
                              ),
                            ),
                          ),
                        );
                      },
                    )                    
                  );
                },
              );
            },
            onMapCreated: (controller) {
              mapController = controller;

              mapController.loadServiceFeatureTable(
                AGMLServiceFeatureLayer(
                  url: 'https://sampleserver6.arcgisonline.com/arcgis/rest/services/NapervilleShelters/FeatureServer/0',
                  viewPoint: AGMLViewPoint(
                    latitude: 41.70,
                    longitude: -88.20,
                    scale: 120000.0
                  )
                )
              );
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
          ),
          AnimatedPositioned(
            duration: const Duration(milliseconds: 200),
            bottom: layerListPosition,
            child: Container(
              height: 230,
              width: MediaQuery.of(context).size.width,
              decoration: BoxDecoration(
                color: Colors.white.withOpacity(0.9),
                borderRadius: const BorderRadius.only(
                  topLeft: Radius.circular(5),
                  topRight: Radius.circular(5)
                ),
              ),
              child: Column(
                children: [
                  TextButton(
                    onPressed: () {
                      setState(() {
                        if(layerListPosition == -185) {
                          layerListPosition = 0;
                        } else {
                          layerListPosition = -185;
                        }
                      });
                    }, 
                    child: Icon(
                      layerListPosition == -185 
                        ? Icons.arrow_upward_rounded 
                        : Icons.arrow_downward_rounded,
                      color: Colors.blue,
                    )
                  ),
                  ...List<Widget>.from(
                    serviceLayersInMap.map(
                      (layer) {
                        return InkWell(
                          onTap: () {
                            if(layer.id != selectedLayerId) {
                              setState(() {
                                selectedLayerId = layer.id ?? '';
                              });
                              mapController.setSelectedFeatureLayer(selectedLayerId);
                            }
                          },
                          child: Card(
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.spaceAround,
                              children: [
                                SizedBox(
                                  width: MediaQuery.of(context).size.width*0.5,
                                  child: Text(layer.url)
                                ),
                                Switch(                                    
                                  value: layer.id == selectedLayerId,
                                  onChanged: (value) {
                                    if(value) {
                                      setState(() {
                                        selectedLayerId = layer.id ?? '';
                                      });
                                      mapController.setSelectedFeatureLayer(selectedLayerId);
                                    } 
                                  },
                                )
                              ],
                            ),
                          ),
                        );
                      }
                    )
                  )
                ]
              ),
            ), 
          ),
        ],
      ),
    );
  }
}