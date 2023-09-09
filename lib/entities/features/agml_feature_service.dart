import 'package:arcgis_maps/entities/agml_view_point.dart';
import 'package:arcgis_maps/entities/features/abstract_agml_feature_layer.dart';



class AGMLFeatureService extends AbstractAGMLFeatureLayer {

  final String url;

  AGMLFeatureService({
    super.id,
    required this.url,
    super.viewPoint
  });

  Map<String, dynamic> toJson() => {
    "id": id,
    "url": url,
    "viewPoint": viewPoint?.toJson()
  };

  AGMLFeatureService copyWith({
    String? id,
    String? url,
    AGMLViewPoint? viewPoint
  }) => AGMLFeatureService(
    id: id ?? this.id,
    url: url ?? this.url,
    viewPoint: viewPoint ?? viewPoint
  ); 

}