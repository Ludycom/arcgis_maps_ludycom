import '../agml_spatial_reference.dart';
import 'agml_geometry_interface.dart';

class AGMLPolyline extends AGMLGeometryInterface {
  final List<List<List<double>>> paths;
  final AGMLSpatialReference spatialReference;
  final double? lengthMeters;
  final Map<String, dynamic> featuresAttributes;

  AGMLPolyline({
    required this.paths,
    required this.spatialReference,
    this.lengthMeters,
    required this.featuresAttributes,
  });

  factory AGMLPolyline.fromJson(Map<String, dynamic> json) {
    final Map<String, dynamic> jsonSpatialReference = Map<String, dynamic>.from(json["spatialReference"]);
    return AGMLPolyline(
    paths: List<List<List<double>>>.from(json["paths"].map((x) => List<List<double>>.from(x.map((x) => List<double>.from(x.map((x) => x?.toDouble())))))),
    spatialReference: AGMLSpatialReference.fromJson(jsonSpatialReference),
    lengthMeters: (json["LENGTH_METERS"] as num?)?.toDouble(),
    featuresAttributes: json["featuresAttributes"],
  );
  }

  @override
  Map<String, dynamic> toJson() => {
    "paths": List<dynamic>.from(paths.map((x) => List<dynamic>.from(x.map((x) => List<dynamic>.from(x.map((x) => x)))))),
    "spatialReference": spatialReference.toJson(),
    "LENGTH_METERS": lengthMeters,
    "featuresAttributes": featuresAttributes,
  };
}