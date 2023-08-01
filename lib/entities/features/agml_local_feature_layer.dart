import 'package:arcgis_maps/entities/agml_view_point.dart';
import 'package:arcgis_maps/entities/features/abstract_agml_feature_layer.dart';



class AGMLLocalFeatureLayer extends AbstractAGMLFeatureLayer {

  final String path;

  AGMLLocalFeatureLayer({
    super.id,
    required this.path,
    super.viewPoint
  });

  Map<String, dynamic> toMap() => {
    "id": id,
    "path": path,
    "viewPoint": viewPoint?.toJson()
  };

  AGMLLocalFeatureLayer copyWith({
    String? id,
    String? path,
    AGMLViewPoint? viewPoint
  }) => AGMLLocalFeatureLayer(
    id: id ?? this.id,
    path: path ?? this.path,
    viewPoint: viewPoint ?? viewPoint
  );

}