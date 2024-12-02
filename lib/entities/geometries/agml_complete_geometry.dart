import 'dart:convert';

import '../../utils/utils.dart';

class AGMLCompleteGeometry {
  final Map<String, dynamic> data;
  final AGMLGeometryTypeEnum geometryType;

  AGMLCompleteGeometry({required this.data, required this.geometryType});

  factory AGMLCompleteGeometry.fromJson(Map<dynamic, dynamic> json) {
    return AGMLCompleteGeometry(
      data: jsonDecode(json['DATA']),
      geometryType: AGMLGeometryTypeEnumExtension.fromString(
        json['GEOMETRY_TYPE'] as String,
      ),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'DATA': data,
      'GEOMETRY_TYPE': geometryType,
    };
  }
}