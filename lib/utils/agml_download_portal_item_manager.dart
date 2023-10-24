import 'dart:async';
import 'dart:convert';

import 'package:arcgis_maps/entities/features/agml_feature_service.dart';
import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';


import 'package:arcgis_maps/utils/enums/agml_download_portal_item_manager_status_num.dart';
import 'package:arcgis_maps/utils/enums/agml_download_portal_item_status_enum.dart';

import 'package:arcgis_maps/entities/features/agml_portal_item.dart';
import 'package:arcgis_maps/entities/agml_download_portal_item.dart';



class AGMLDownloadPortalItemManager {

  late MethodChannel _channel;

  var _status = AGMLDownloadPortalItemManagerStatusEnum.prepared;
  AGMLDownloadPortalItemManagerStatusEnum get status => _status;

  final List<AGMLDownloadPortalItem> _downloads = [];
  List<AGMLDownloadPortalItem> get downloads => _downloads;
  final StreamController<List<AGMLDownloadPortalItem>> _onChangedDownloadsStream = StreamController();
  StreamController<List<AGMLDownloadPortalItem>> get onChangedDownloadsStream => _onChangedDownloadsStream;

  final StreamController<AGMLDownloadPortalItemManagerStatusEnum> _onChangedManagerStatusStream = StreamController();
  StreamController<AGMLDownloadPortalItemManagerStatusEnum> get onChangedManagerStatusStream => _onChangedManagerStatusStream;


  AGMLDownloadPortalItemManager() {
    _channel = const MethodChannel('arcgis_maps');
  }


  Future<List<AGMLDownloadPortalItem>> checkDownloadedPortalItems(List<AGMLPortalItem> portalItems) async {
    const method = '/checkDownloadedPortalItems';

    try {
      final channelResponse = await _channel.invokeListMethod<String>(
        method, 
        portalItems.map((pI) => pI.toJson()).toList()
      );
      final downloadPortalItems = List<AGMLDownloadPortalItem>.from(channelResponse!.map(
          (json) => AGMLDownloadPortalItem.fromJson(jsonDecode(json))
        )
      );
      return downloadPortalItems;
    } catch (e) {
      if(kDebugMode) print(e);
      return [];
    }
  }


  Future<List<AGMLDownloadPortalItem>> downloadPortalItems(List<AGMLPortalItem> portalItems) async {
    if(_status == AGMLDownloadPortalItemManagerStatusEnum.downloading) {
      throw statusWarnningException;
    }
    _status = AGMLDownloadPortalItemManagerStatusEnum.downloading;
    _onChangedManagerStatusStream.add(_status);

    final downloadPortalItems = portalItems.map((portalItem) => AGMLDownloadPortalItem(portalItem: portalItem));

    _downloads.clear();
    _downloads.addAll(downloadPortalItems);
    
    for (int i=0; i<_downloads.length; i++) {
      _downloads[i] = _downloads[i].copyWith(
        downloadStatus: AGMLDownloadPortalItemStatusEnum.downloading
      );
      _onChangedDownloadsStream.add(_downloads);

      final portalItemResponse = await _downloadPortalItem(_downloads[i].portalItem);

      _downloads[i] = portalItemResponse;
      _onChangedDownloadsStream.add(_downloads);
    }
    final downloadsToReturn = List<AGMLDownloadPortalItem>.from(_downloads);

    _onChangedDownloadsStream.add(_downloads);
    _status = AGMLDownloadPortalItemManagerStatusEnum.prepared;
    _onChangedManagerStatusStream.add(_status);

    return downloadsToReturn;
  }


  Future<AGMLDownloadPortalItem> _downloadPortalItem(AGMLPortalItem portalItem) async {
    const method = '/downloadPortalItem';
    late final AGMLDownloadPortalItem downloadPortalItem;

    try {
      final channelResponse = await _channel.invokeMethod(method, portalItem.toJson()) as String;
      downloadPortalItem = AGMLDownloadPortalItem.fromJson(jsonDecode(channelResponse));
    } catch (e) {
      if (kDebugMode) print(e);
      downloadPortalItem = AGMLDownloadPortalItem(
        portalItem: portalItem,
        downloadStatus: AGMLDownloadPortalItemStatusEnum.failure
      );
    }

    return downloadPortalItem;
  }

  Future<AGMLDownloadPortalItem> downloadClipPortalItemGeoDatabase(AGMLFeatureService featureService) async {
    const method = '/downloadClipPortalItemGeoDatabase';
    // late final AGMLDownloadPortalItem downloadPortalItem;

    try {
      // final channelResponse = await
      _channel.invokeMethod(method, featureService.toJson());
      // downloadPortalItem = AGMLDownloadPortalItem.fromJson(jsonDecode(channelResponse));
    } catch (e) {
      if (kDebugMode) print(e);
      
    }

    // return downloadPortalItem;
    return AGMLDownloadPortalItem(portalItem: AGMLPortalItem(url: 'url'));
  }


  static const statusWarnningException = FormatException('in-process downloads: The AGMLDownloadPortalItemManager is downloading, you have to validate AGMLDownloadPortalItemManager.status');
}