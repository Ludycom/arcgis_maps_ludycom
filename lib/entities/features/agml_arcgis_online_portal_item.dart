import 'package:arcgis_maps/entities/agml_view_point.dart';

class AGMLArcGISOnlinePortalItem {

  final String itemID;
  final AGMLViewPoint? viewPoint;

  AGMLArcGISOnlinePortalItem({
    required this.itemID, 
    this.viewPoint
  });

  Map<String, dynamic> toMap() => {
    "itemID": itemID,
    "viewPoint": viewPoint?.toJson()
  };

}