import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';


import 'package:arcgis_maps/entities/features/agml_portal_item.dart';
import 'package:arcgis_maps/entities/features/agml_feature_service.dart';
import 'package:arcgis_maps/entities/agml_download_portal_item.dart';
import 'package:arcgis_maps/utils/agml_download_portal_item_manager.dart';
import 'package:arcgis_maps/utils/enums/agml_download_portal_item_status_enum.dart';
import 'package:arcgis_maps/utils/enums/agml_download_portal_item_manager_status_num.dart';



class DownloadFormPortalPage extends StatefulWidget {

  const DownloadFormPortalPage({super.key});

  @override
  State<DownloadFormPortalPage> createState() => _DownloadFormPortalPageState();
}

class _DownloadFormPortalPageState extends State<DownloadFormPortalPage> {

  bool isDownloading = false;
  late final AGMLDownloadPortalItemManager agmlDownloadPortalItemManager;
  List<AGMLDownloadPortalItem> downloadPortalItemList = [];
  List<AGMLDownloadPortalItem> checkPortalItemList = [];

  @override
  void initState() {
    super.initState();
    agmlDownloadPortalItemManager = AGMLDownloadPortalItemManager();

    agmlDownloadPortalItemManager.onChangedDownloadsStream.stream.listen((event) {
      setState(() {
        downloadPortalItemList = List.from(event);
      });
    });

    agmlDownloadPortalItemManager.onChangedManagerStatusStream.stream.listen((event) {
      if(event == AGMLDownloadPortalItemManagerStatusEnum.downloading) {
        setState(() { isDownloading = true; });
      } else {
        setState(() { isDownloading = false; });
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const FittedBox(
          child: Text(
            'Download from portal',
            style: TextStyle(color: Colors.white),
          )
        ),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.start,
          children: [
            OutlinedButton(
              onPressed: () async {
                setState(() { downloadPortalItemList.clear(); });
                final response = await agmlDownloadPortalItemManager.downloadPortalItems(agmlPortalItemList);
                if (kDebugMode) {
                  print(response);
                }
              },
              child: const Text('Download')
            ),
            OutlinedButton(
              onPressed: () async {
                final checkIfHaveFiles = await agmlDownloadPortalItemManager.checkDownloadedPortalItems(agmlPortalItemList);
                setState(() {
                  checkPortalItemList = checkIfHaveFiles;
                });
              },
              child: const Text('Check if have Portal Layers')
            ),
            Container(
              margin: const EdgeInsets.symmetric(vertical: 10),
              decoration: BoxDecoration(
                color: isDownloading ? Colors.green : Theme.of(context).colorScheme.primary,
                borderRadius: BorderRadius.circular(5)
              ),
              width: 200,
              height: 60,
              child: Center(
                child: Text(
                  isDownloading ? 'Downloading' : 'Prepared to download',
                  style: const TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.bold
                  ),
                ),
              ),
            ),
            Expanded(
              child: SingleChildScrollView(
                child: Column(
                  children: [
                    const Text('In device files'),
            
                    ...List<_InDeviceFileCard>.from(
                      agmlPortalItemList.map((portalItem) {
                        final downloadPortalItem = checkPortalItemList.firstWhere(
                          (element) => element.portalItem.url == portalItem.url,
                          orElse: () => AGMLDownloadPortalItem(portalItem: portalItem),
                        );
            
                        return _InDeviceFileCard(
                          onlineUrl: portalItem.url,
                          pathLocation: downloadPortalItem.pathLocation ?? 'No data',
                          state: downloadPortalItem.downloadStatus
                        );
                      })
                    ),
            
                    const Padding(
                      padding: EdgeInsets.all(5),
                      child: Text('Downloads'),
                    ),
            
                    ...List<_DownloadFileCard>.from(
                      downloadPortalItemList.map((downloadPortalItem) {
                        return _DownloadFileCard(
                          url: downloadPortalItem.portalItem.url,
                          state: downloadPortalItem.downloadStatus,
                          location: downloadPortalItem.pathLocation  ?? 'No data'
                        );
                      })
                    )
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}


class _DownloadFileCard extends StatelessWidget {

  final String url;
  final AGMLDownloadPortalItemStatusEnum state;
  final String location;

  const _DownloadFileCard({
    required this.url, 
    required this.state, 
    required this.location,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Column(
        children: [
          Container(
            margin: const EdgeInsets.all(5),
            padding: const EdgeInsets.all(5),
            decoration: BoxDecoration(
              color: Colors.grey.withOpacity(0.5),                            
              borderRadius: BorderRadius.circular(5)
            ),
            child: Text(url),
          ),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              Container(
                height: 30,
                width: 110,
                alignment: Alignment.center,
                margin: const EdgeInsets.all(5),
                padding: const EdgeInsets.all(5),
                decoration: BoxDecoration(
                  color: getStatusColor(state),
                  borderRadius: BorderRadius.circular(5)
                ),
                child: Text(
                  state.name,
                  style: const TextStyle(color: Colors.white),
                ),
              ),
              Container(
                margin: const EdgeInsets.only(bottom: 5),                  
                decoration: BoxDecoration(
                  color: Colors.black.withOpacity(0.2),
                  borderRadius: BorderRadius.circular(5)
                ),
                width: MediaQuery.of(context).size.width*0.6,
                child: Text(
                  location.isNotEmpty
                    ? location
                    : 'No data',
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 12
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _InDeviceFileCard extends StatelessWidget {

  final String onlineUrl;
  final String pathLocation;
  final AGMLDownloadPortalItemStatusEnum state;

  const _InDeviceFileCard({
    required this.onlineUrl,
    required this.pathLocation,
    required this.state,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              Container(
                width: MediaQuery.of(context).size.width*0.6,
                padding: const EdgeInsets.all(8.0),
                child: Text(onlineUrl),
              ),
              Center(
                child: Container(
                  decoration: BoxDecoration(
                    color: getStatusColor(state),
                    borderRadius: BorderRadius.circular(5)
                  ),
                  padding: const EdgeInsets.all(10),
                  child: Text(state.name, style: const TextStyle(color: Colors.white),),
                ),
              )
            ],
          ),
          Padding(
            padding: const EdgeInsets.only(left: 10, right: 10, bottom: 10),
            child: Text(
              'pathLocation: $pathLocation',
              style: const TextStyle(
                fontSize: 12
              ),
            ),
          )
        ],
      ),
    );
  }
}

Color getStatusColor(AGMLDownloadPortalItemStatusEnum state) {
  switch(state) {
    case AGMLDownloadPortalItemStatusEnum.none: return Colors.blueGrey;
    case AGMLDownloadPortalItemStatusEnum.downloading: return Colors.green;
    case AGMLDownloadPortalItemStatusEnum.fileExists: return Colors.blue;
    case AGMLDownloadPortalItemStatusEnum.fileNoExists: return Colors.yellow;
    case AGMLDownloadPortalItemStatusEnum.failure: return Colors.red;
    case AGMLDownloadPortalItemStatusEnum.success: return Colors.black;
  }
}

final agmlPortalItemList = [
  AGMLPortalItem(url: 'https://www.arcgis.com/home/item.html?id=cb1b20748a9f4d128dad8a87244e3e37'),
  AGMLPortalItem(url: 'https://www.arcgis.com/home/item.html?id=15a7cbd3af1e47cfa5d2c6b93dc44fc2'),
  AGMLPortalItem(url: 'https://www.arcgis.com/home/item.html?id=68ec42517cdd439e81b036210483e8e7')
];