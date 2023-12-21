
import 'package:arcgis_maps/entities/agml_view_point.dart';

class AGMLChangeSpacialReferenceParams {

  final AGMLViewPoint point;
  final int fromSpacialReference;
  final int toSpacialReference;

  AGMLChangeSpacialReferenceParams({
    required this.point,
    required this.fromSpacialReference,
    required this.toSpacialReference
  });

  Map<String, dynamic> toJson() => {
    "point": point.toJson(),
    "fromSpacialReference": fromSpacialReference,
    "toSpacialReference": toSpacialReference
  };

}