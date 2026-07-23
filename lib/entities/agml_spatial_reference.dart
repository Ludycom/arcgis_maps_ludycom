class AGMLSpatialReference {
  final double? latestWkid;
  final double? wkid;

  AGMLSpatialReference({
    this.latestWkid,
    this.wkid,
  });

  factory AGMLSpatialReference.fromJson(Map<String, dynamic> json) => AGMLSpatialReference(
    // El wkid puede venir como entero (p. ej. 4326) desde el JSON crudo de un
    // feature de ArcGIS; se coacciona a double para no lanzar TypeError.
    latestWkid: (json["latestWkid"] as num?)?.toDouble(),
    wkid: (json["wkid"] as num?)?.toDouble(),
  );

  Map<String, dynamic> toJson() => {
    "latestWkid": latestWkid,
    "wkid": wkid,
  };
}