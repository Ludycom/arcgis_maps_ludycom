import '../agml_spatial_reference.dart';
import 'agml_geometry_interface.dart';

class AGMLMultipoint extends AGMLGeometryInterface {
  final List<List<double>> points;
  final AGMLSpatialReference spatialReference;
  final Map<String, dynamic> featuresAttributes;

  AGMLMultipoint({
    required this.points,
    required this.spatialReference,
    required this.featuresAttributes,
  });

  factory AGMLMultipoint.fromJson(Map<String, dynamic> json) => AGMLMultipoint(
    points: List<List<double>>.from(json["points"].map((x) => List<double>.from(x.map((x) => x?.toDouble())))),
    spatialReference: AGMLSpatialReference.fromJson(json["spatialReference"]),
    featuresAttributes: json["featuresAttributes"],
  );

  @override
  Map<String, dynamic> toJson() => {
    "points": List<dynamic>.from(points.map((x) => List<dynamic>.from(x.map((x) => x)))),
    "spatialReference": spatialReference.toJson(),
    "featuresAttributes": featuresAttributes,
  };
}
