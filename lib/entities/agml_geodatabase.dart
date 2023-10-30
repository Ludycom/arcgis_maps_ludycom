
import 'package:arcgis_maps/entities/agml_view_point.dart';

class AGMLGeodatabase {

  final String? id;
  final String? path;
  final String? url;
  final AGMLViewPoint? viewPoint;

  AGMLGeodatabase({
    this.id,
    this.path,
    this.url,
    this.viewPoint
  });

  Map<String, dynamic> toJson() => {
    "id": id,
    "path": path,
    "url": url,
    "viewPoint": viewPoint?.toJson()
  };

  factory AGMLGeodatabase.fromJson(Map<String, dynamic> json) => AGMLGeodatabase(
    id: json['id:'],
    path: json['path'],
    url: json['url'],
    viewPoint: json['viewPoint'] != null ? AGMLViewPoint.fromJson(json['viewPoint']) : null
  );

}