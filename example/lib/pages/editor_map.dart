import 'package:flutter/material.dart';
import 'package:arcgis_maps/arcgis_maps_ludycom.dart';


class EditorMapPage extends StatefulWidget {

  const EditorMapPage({super.key});

  @override
  State<EditorMapPage> createState() => _EditorMapPageState();
}

class _EditorMapPageState extends State<EditorMapPage> {
  late final AGMLDownloadPortalItemManager downloadPortalItemManager;
  List<AGMLDownloadPortalItem> downloadPortalItemList = [];
  late final AGMLMapController mapController;
  bool isEditing = false;

  @override
  void initState() {
    super.initState();
    downloadPortalItemManager = AGMLDownloadPortalItemManager();
    _downloadPortalItem();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const FittedBox(
          child: Text(
            'Editor map',
            style: TextStyle(color: Colors.white),
          )
        ),
      ),
      body: StreamBuilder(
        stream: downloadPortalItemManager.onChangedDownloadsStream.stream,
        builder: (context, snapshot) {
          if(!snapshot.hasData || snapshot.data == null) {
            return const Center(
              child: CircularProgressIndicator(),
            );
          }
          if(snapshot.data!.isEmpty ||
              snapshot.data!.first.downloadStatus == AGMLDownloadPortalItemStatusEnum.failure) {
            return const Center(
              child: Icon(Icons.warning_rounded, color: Colors.red),
            );
          }
          if(snapshot.data!.first.downloadStatus == AGMLDownloadPortalItemStatusEnum.downloading) {
            return const Center(
              child: CircularProgressIndicator(),
            );
          }

          return Stack(
            children: [
              AGMLMap(
                creationParams: AGMLCreationParams(),
                onMapCreated: (_) {
                  mapController = _;
                  mapController.loadMobileMapPackage(AGMLMobileMapPackage(
                    path: snapshot.data!.first.pathLocation
                  ));
                  mapController.startLocation();
                },
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
                          backgroundColor: WidgetStatePropertyAll<Color>(Colors.blue)
                        ),
                        child: const Icon(Icons.my_location_rounded, color: Colors.white),
                        onPressed: () => mapController.autoPaneModeCenterLocation(),
                      ),
                     TextButton(
                        style: ButtonStyle(
                          backgroundColor: WidgetStatePropertyAll<Color>(
                            isEditing ? Colors.grey : Colors.blue
                          )
                        ),
                        onPressed: isEditing
                            ? null
                            : () => _startEditing(AGMLGeometryTypeEnum.point),
                        child: const Text('Create Point', style: TextStyle(color: Colors.white)),
                      ),
                     TextButton(
                        style: ButtonStyle(
                          backgroundColor: WidgetStatePropertyAll<Color>(
                            isEditing ? Colors.grey : Colors.blue
                          )
                        ),
                        onPressed: isEditing
                            ? null
                            : () => _startEditing(AGMLGeometryTypeEnum.multipoint),
                        child: const Text('Create Multipoint', style: TextStyle(color: Colors.white)),
                      ),
                     TextButton(
                        style: ButtonStyle(
                          backgroundColor: WidgetStatePropertyAll<Color>(
                            isEditing ? Colors.grey : Colors.blue
                          )
                        ),
                        onPressed: isEditing
                            ? null
                            : () => _startEditing(AGMLGeometryTypeEnum.polyline),
                        child: const Text('Create Polyline', style: TextStyle(color: Colors.white)),
                      ),
                     TextButton(
                        style: ButtonStyle(
                          backgroundColor: WidgetStatePropertyAll<Color>(
                            isEditing ? Colors.grey : Colors.blue
                          )
                        ),
                        onPressed: isEditing
                            ? null
                            : () => _startEditing(AGMLGeometryTypeEnum.polygon),
                        child: const Text('Create Polygon', style: TextStyle(color: Colors.white)),
                      ),
                      TextButton(
                        style: ButtonStyle(
                          backgroundColor: WidgetStatePropertyAll<Color>(
                            isEditing ? Colors.blue: Colors.grey
                          )
                        ),
                        onPressed: isEditing
                            ? _onCompleteGeometry
                            : null,
                        child: const Text('Complete', style: TextStyle(color: Colors.white)),
                      ),
                      TextButton(
                        style: ButtonStyle(
                          backgroundColor: WidgetStatePropertyAll<Color>(
                            isEditing ? Colors.blue: Colors.grey
                          )
                        ),
                        onPressed: isEditing
                            ? () {
                                mapController.cancelEditing();
                                _setIsEditing(!isEditing);
                              }
                            : null,
                        child: const Text('Cancel', style: TextStyle(color: Colors.white)),
                      ),
                      if(isEditing) StreamBuilder<bool>(
                        stream: mapController.canUndoStream,
                        builder: (context, snapshot) {
                          return TextButton(
                            style: ButtonStyle(
                              backgroundColor: WidgetStatePropertyAll<Color>(
                                snapshot.data == true ? Colors.blue: Colors.grey
                              )
                            ),
                            onPressed: isEditing
                                ? () => mapController.undoGeometry()
                                : null,
                            child: const Icon(Icons.arrow_back_ios_rounded, color: Colors.white),
                          );
                        }
                      ),
                      if(isEditing) StreamBuilder<bool>(
                        stream: mapController.canRedoStream,
                        builder: (context, snapshot) {
                          return TextButton(
                            style: ButtonStyle(
                              backgroundColor: WidgetStatePropertyAll<Color>(
                                snapshot.data == true ? Colors.blue: Colors.grey
                              )
                            ),
                            onPressed: isEditing
                                ? () => mapController.redoGeometry()
                                : null,
                            child: const Icon(Icons.arrow_forward_ios_rounded, color: Colors.white),
                          );
                        }
                      ),
                      if(isEditing) StreamBuilder<bool>(
                          stream: mapController.canDeleteStream,
                          builder: (context, snapshot) {
                            return TextButton(
                              style: ButtonStyle(
                                  backgroundColor: WidgetStatePropertyAll<Color>(
                                      snapshot.data == true ? Colors.blue: Colors.grey
                                  )
                              ),
                              onPressed: isEditing
                                  ? () => mapController.deleteSelectedGeometryElement()
                                  : null,
                              child: const Icon(Icons.highlight_remove_rounded, color: Colors.white),
                            );
                          }
                      )
                    ],
                  ),
                ),
              )
            ],
          );
        },
      )
    );
  }

  void _onCompleteGeometry() async {
    final geometry = await mapController.completeEditing();
    mapController.addGeometry(geometry);
    _setIsEditing(!isEditing);

    Future.delayed(const Duration(seconds: 10), () {
      mapController.removeGeometry(geometry);
    });
  }

  void _downloadPortalItem() {
    downloadPortalItemManager.downloadPortalItems(portalItemList);
  }

  void _startEditing(AGMLGeometryTypeEnum editType) {
    mapController.startEditing(editType);
    _setIsEditing(!isEditing);
  }

  void _setIsEditing(bool value) {
    setState(() {
      isEditing = value;
    });
  }
}

final portalItemList = <AGMLPortalItem>[
  // TODO: Add your portal item id
];