
class AGMLViewPoint {

  final double latitude;
  final double longitude;
  final double scale;

  AGMLViewPoint({
    required this.latitude,
    required this.longitude,
    this.scale = 120000.0
  });

  factory AGMLViewPoint.fromJson(Map<dynamic, dynamic> json) => AGMLViewPoint(
    latitude: json["latitude"]?.toDouble(),
    longitude: json["longitude"]?.toDouble(),
    scale: json["scale"]?.toDouble(),
  );

  Map<String, double> toJson() => {
    "latitude": latitude,
    "longitude": longitude,
    "scale": scale
  };

}