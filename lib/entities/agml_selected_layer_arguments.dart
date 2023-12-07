
class AGMLSelectedLayerArguments {

  final String? layerId;
  final int maxResults;

  AGMLSelectedLayerArguments({
    this.layerId,
    this.maxResults = -1
  });

  Map<String, dynamic> toJson() => {
    "layerId": layerId,
    "maxResults": maxResults
  };
}