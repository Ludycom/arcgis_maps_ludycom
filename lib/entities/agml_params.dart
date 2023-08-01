import 'package:arcgis_maps/entities/agml_view_point.dart';



class AGMLCreationParams {

  final String? apiKey;
  final AGMLViewPoint? initViewPoint;

  AGMLCreationParams({
    this.apiKey,
    this.initViewPoint
  });

  Map<String, dynamic> toMap() => {
    "apiKey": apiKey ?? '',
    "initViewPoint": initViewPoint?.toJson()
  };

}