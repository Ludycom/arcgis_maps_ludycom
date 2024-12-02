import '../agml_spatial_reference.dart';
import 'agml_geometry_interface.dart';

class AGMLPolyline extends AGMLGeometryInterface {
  final List<List<List<double>>> paths;
  final AGMLSpatialReference spatialReference;

  AGMLPolyline({
    required this.paths,
    required this.spatialReference,
  });

  factory AGMLPolyline.fromJson(Map<String, dynamic> json) => AGMLPolyline(
    paths: List<List<List<double>>>.from(json["paths"].map((x) => List<List<double>>.from(x.map((x) => List<double>.from(x.map((x) => x?.toDouble())))))),
    spatialReference: AGMLSpatialReference.fromJson(json["spatialReference"]),
  );

  @override
  Map<String, dynamic> toJson() => {
    "paths": List<dynamic>.from(paths.map((x) => List<dynamic>.from(x.map((x) => List<dynamic>.from(x.map((x) => x)))))),
    "spatialReference": spatialReference.toJson(),
  };
}