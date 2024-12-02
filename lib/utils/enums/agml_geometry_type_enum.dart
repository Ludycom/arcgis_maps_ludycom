
enum AGMLGeometryTypeEnum {
  point,
  polyline,
  polygon,
  multipoint,
}

extension AGMLGeometryTypeEnumExtension on AGMLGeometryTypeEnum {
  String getString() {
    switch(this) {
      case AGMLGeometryTypeEnum.point: return 'POINT';
      case AGMLGeometryTypeEnum.polyline: return 'POLYLINE';
      case AGMLGeometryTypeEnum.polygon: return 'POLYGON';
      case AGMLGeometryTypeEnum.multipoint: return 'MULTIPOINT';
      default: return 'POINT';
    }
  }

  static AGMLGeometryTypeEnum fromString(String value) {
    switch (value) {
      case 'POINT':
        return AGMLGeometryTypeEnum.point;
      case 'POLYLINE':
        return AGMLGeometryTypeEnum.polyline;
      case 'POLYGON':
        return AGMLGeometryTypeEnum.polygon;
      case 'MULTIPOINT':
        return AGMLGeometryTypeEnum.multipoint;
      default:
        throw ArgumentError('Invalid geometry type string: $value');
    }
  }
}