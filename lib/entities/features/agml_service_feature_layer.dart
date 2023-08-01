import 'package:arcgis_maps/entities/agml_view_point.dart';
import 'package:arcgis_maps/entities/features/abstract_agml_feature_layer.dart';



class AGMLServiceFeatureLayer extends AbstractAGMLFeatureLayer {

  final String url;

  AGMLServiceFeatureLayer({
    super.id,
    required this.url,
    super.viewPoint
  });

  Map<String, dynamic> toMap() => {
    "id": id,
    "url": url,
    "viewPoint": viewPoint?.toJson()
  };

  AGMLServiceFeatureLayer copyWith({
    String? id,
    String? url,
    AGMLViewPoint? viewPoint
  }) => AGMLServiceFeatureLayer(
    id: id ?? this.id,
    url: url ?? this.url,
    viewPoint: viewPoint ?? viewPoint
  ); 

}