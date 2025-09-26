class AGMLSpatialReference {
  final double? latestWkid;
  final double? wkid;

  AGMLSpatialReference({
    this.latestWkid,
    this.wkid,
  });

  factory AGMLSpatialReference.fromJson(Map<String, dynamic> json) => AGMLSpatialReference(
    latestWkid: json["latestWkid"],
    wkid: json["wkid"],
  );

  Map<String, dynamic> toJson() => {
    "latestWkid": latestWkid,
    "wkid": wkid,
  };
}