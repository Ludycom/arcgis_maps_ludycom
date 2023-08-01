import 'package:arcgis_maps/entities/agml_view_point.dart';

class AGMLPortalItem {

  final String url;
  final AGMLViewPoint? viewPoint;

  AGMLPortalItem({
    required this.url, 
    this.viewPoint
  });

  factory AGMLPortalItem.fromJson(Map<String, dynamic> json) => AGMLPortalItem(
    url: json["url"],
    viewPoint: json["viewPoint"] != null
      ? AGMLViewPoint.fromJson(json["viewPoint"])
      : null,
  );

  Map<String, dynamic> toJson() => {
    "url": url,
    "viewPoint": viewPoint?.toJson()
  };

}