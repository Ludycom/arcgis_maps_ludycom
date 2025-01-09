class AGMLSpatialReference {
  final int? latestWkid;
  final int? wkid;

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