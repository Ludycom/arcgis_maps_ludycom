import 'package:arcgis_maps/entities/features/agml_portal_item.dart';
import 'package:arcgis_maps/utils/enums/agml_download_portal_item_status_enum.dart';



class AGMLDownloadPortalItem {

  final AGMLPortalItem portalItem;
  final AGMLDownloadPortalItemStatusEnum downloadStatus;
  final String? pathLocation;

  AGMLDownloadPortalItem({
    required this.portalItem,
    this.downloadStatus = AGMLDownloadPortalItemStatusEnum.none,
    this.pathLocation
  });

  factory AGMLDownloadPortalItem.fromJson(Map<String, dynamic> json) => AGMLDownloadPortalItem(
    portalItem: AGMLPortalItem.fromJson(json['portalItem']),
    downloadStatus: _getDownloadPortalItemStatusEnum(json['downloadStatus']),
    pathLocation: json['pathLocation']
  );

  AGMLDownloadPortalItem copyWith ({
    AGMLPortalItem? portalItem,
    AGMLDownloadPortalItemStatusEnum? downloadStatus,
    String? pathLocation
  }) => AGMLDownloadPortalItem(
    portalItem: portalItem ?? this.portalItem,
    downloadStatus: downloadStatus ?? this.downloadStatus,
    pathLocation: pathLocation ?? this.pathLocation
  );

}

AGMLDownloadPortalItemStatusEnum _getDownloadPortalItemStatusEnum(String state) {
  switch(state) {
    case 'FAILED': return AGMLDownloadPortalItemStatusEnum.failure;
    case 'FILE_EXISTS': return AGMLDownloadPortalItemStatusEnum.fileExists;
    case 'FILE_NO_EXISTS': return AGMLDownloadPortalItemStatusEnum.fileNoExists;
    case 'SUCCESS': return AGMLDownloadPortalItemStatusEnum.success;
    default: return AGMLDownloadPortalItemStatusEnum.none;
  }
}
