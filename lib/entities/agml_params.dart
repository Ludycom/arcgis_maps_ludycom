import 'package:arcgis_maps/entities/agml_view_point.dart';



class AGMLCreationParams {

  final AGMLViewPoint? initViewPoint;
  final AGMLBasemapStyleEnum basemapStyle;

  AGMLCreationParams({
    this.initViewPoint,
    this.basemapStyle = AGMLBasemapStyleEnum.none
  });

  Map<String, dynamic> toMap() => {
    "initViewPoint": initViewPoint?.toJson(),
    "basemapStyle": _toMapAGMLBasemapStyleEnum(basemapStyle)
  };
}

String _toMapAGMLBasemapStyleEnum(AGMLBasemapStyleEnum state) {
  switch(state) {
    case AGMLBasemapStyleEnum.none: return 'NONE';
    case AGMLBasemapStyleEnum.arcGISTopographic: return 'ARCGIS_TOPOGRAPHIC';
    default: return 'NONE';
  }
}

enum AGMLBasemapStyleEnum {
  none,
  arcGISTopographic,
}