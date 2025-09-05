import '../agml_spatial_reference.dart';
import 'agml_geometry_interface.dart';

class AGMLPolygon extends AGMLGeometryInterface {
  final List<List<List<double>>> rings;
  final AGMLSpatialReference spatialReference;
  final Map<String, dynamic> featuresAttributes;

  AGMLPolygon({
    required this.rings,
    required this.spatialReference,
    required this.featuresAttributes,
  });

  factory AGMLPolygon.fromJson(Map<String, dynamic> json) => AGMLPolygon(
    rings: List<List<List<double>>>.from(json["rings"].map((x) => List<List<double>>.from(x.map((x) => List<double>.from(x.map((x) => x?.toDouble())))))),
    spatialReference: AGMLSpatialReference.fromJson(json["spatialReference"]),
    featuresAttributes: json["featuresAttributes"],
  );

  @override
  Map<String, dynamic> toJson() => {
    "rings": List<dynamic>.from(rings.map((x) => List<dynamic>.from(x.map((x) => List<dynamic>.from(x.map((x) => x)))))),
    "spatialReference": spatialReference.toJson(),
    "featuresAttributes": featuresAttributes,
  };
}