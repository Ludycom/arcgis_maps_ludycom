class AGMLColor {
  final int r;
  final int g;
  final int b;
  final int a;

  const AGMLColor({
    required this.r,
    required this.g,
    required this.b,
    this.a = 255,
  });

  /// Naranja por defecto usado en la librería.
  static const AGMLColor orange = AGMLColor(r: 0xFE, g: 0x87, b: 0x00);

  Map<String, int> toMap() => {'colorR': r, 'colorG': g, 'colorB': b, 'colorA': a};
}
