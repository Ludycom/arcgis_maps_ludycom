import 'package:arcgis_maps/entities/agml_view_point.dart';
import 'package:arcgis_maps/entities/features/abstract_agml_feature_layer.dart';



class AGMLFeatureServiceLayer extends AbstractAGMLFeatureLayer {

  final String url;

  AGMLFeatureServiceLayer({
    super.id,
    required this.url,
    super.viewPoint
  });

  Map<String, dynamic> toMap() => {
    "id": id,
    "url": url,
    "viewPoint": viewPoint?.toJson()
  };

  AGMLFeatureServiceLayer copyWith({
    String? id,
    String? url,
    AGMLViewPoint? viewPoint
  }) => AGMLFeatureServiceLayer(
    id: id ?? this.id,
    url: url ?? this.url,
    viewPoint: viewPoint ?? viewPoint
  ); 

}