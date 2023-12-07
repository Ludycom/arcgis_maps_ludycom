import 'package:arcgis_maps/entities/agml_view_point.dart';


abstract class AbstractAGMLFeatureLayer {

  final String? id;
  final AGMLViewPoint? viewPoint;

  AbstractAGMLFeatureLayer({
    this.id,
    required this.viewPoint
  });



}