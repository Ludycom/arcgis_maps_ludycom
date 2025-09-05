import 'dart:convert';

import '../../utils/utils.dart';

class AGMLCompleteGeometry {
  final Map<String, dynamic> data;
  final AGMLGeometryTypeEnum geometryType;
  final Map<String, dynamic> featuresAttributes;

  AGMLCompleteGeometry({
    required this.data,
    required this.geometryType,
    required this.featuresAttributes,
  });

  factory AGMLCompleteGeometry.fromJson(Map<dynamic, dynamic> json) {
    return AGMLCompleteGeometry(
      featuresAttributes: jsonDecode(json['FEATURES_ATTRIBUTES']),
      data: json['DATA'] is String
          ? jsonDecode(json['DATA'])
          : Map<String, dynamic>.from(json['DATA'] ?? {}),
      geometryType: AGMLGeometryTypeEnumExtension.fromString(
        json['GEOMETRY_TYPE'] as String,
      ),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'DATA': data,
      'GEOMETRY_TYPE': geometryType.toString(),
      'FEATURES_ATTRIBUTES': featuresAttributes,
    };
  }
}
