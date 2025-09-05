import '../agml_spatial_reference.dart';
import 'agml_geometry_interface.dart';

class AGMLPoint extends AGMLGeometryInterface {
  final AGMLSpatialReference spatialReference;
  final double x;
  final double y;
  final Map<String, dynamic> featuresAttributes;

  AGMLPoint({
    required this.spatialReference,
    required this.x,
    required this.y,
    required this.featuresAttributes
  });

  factory AGMLPoint.fromJson(Map<String, dynamic> json) => AGMLPoint(
    spatialReference: AGMLSpatialReference.fromJson(json["spatialReference"]),
    x: json["x"]?.toDouble(),
    y: json["y"]?.toDouble(),
    featuresAttributes: json["featuresAttributes"],
  );

  @override
  Map<String, dynamic> toJson() => {
    "spatialReference": spatialReference.toJson(),
    "x": x,
    "y": y,
    "featuresAttributes": featuresAttributes,
  };
}