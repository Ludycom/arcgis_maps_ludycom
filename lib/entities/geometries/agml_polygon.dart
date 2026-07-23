import '../agml_spatial_reference.dart';
import 'agml_geometry_interface.dart';

class AGMLPolygon extends AGMLGeometryInterface {
  final List<List<List<double>>> rings;
  final AGMLSpatialReference spatialReference;
  final double? areaSqMeters;
  final Map<String, dynamic> featuresAttributes;

  AGMLPolygon({
    required this.rings,
    required this.spatialReference,
    this.areaSqMeters,
    required this.featuresAttributes,
  });

  factory AGMLPolygon.fromJson(Map<String, dynamic> json) {
    final Map<String, dynamic> jsonSpatialReference = Map<String, dynamic>.from(json["spatialReference"]);
    return AGMLPolygon(
      rings: List<List<List<double>>>.from(json["rings"].map((x) => List<List<double>>.from(x.map((x) => List<double>.from(x.map((x) => x?.toDouble())))))),
      spatialReference: AGMLSpatialReference.fromJson(jsonSpatialReference),
      areaSqMeters: (json["AREA_SQ_METERS"] as num?)?.toDouble(),
      featuresAttributes: json["featuresAttributes"],
    );
  }

  @override
  Map<String, dynamic> toJson() => {
    "rings": List<dynamic>.from(rings.map((x) => List<dynamic>.from(x.map((x) => List<dynamic>.from(x.map((x) => x)))))),
    "spatialReference": spatialReference.toJson(),
    "AREA_SQ_METERS": areaSqMeters,
    "featuresAttributes": featuresAttributes,
  };
}