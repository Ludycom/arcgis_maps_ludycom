// import 'package:flutter_test/flutter_test.dart';
// import 'package:arcgis_maps/arcgis_maps.dart';
// import 'package:arcgis_maps/arcgis_maps_platform_interface.dart';
// import 'package:arcgis_maps/arcgis_maps_method_channel.dart';
// import 'package:plugin_platform_interface/plugin_platform_interface.dart';

// class MockArcgisMapsPlatform
//     with MockPlatformInterfaceMixin
//     implements ArcgisMapsPlatform {

//   @override
//   Future<String?> getPlatformVersion() => Future.value('42');
// }

// void main() {
//   final ArcgisMapsPlatform initialPlatform = ArcgisMapsPlatform.instance;

//   test('$MethodChannelArcgisMaps is the default instance', () {
//     expect(initialPlatform, isInstanceOf<MethodChannelArcgisMaps>());
//   });

//   test('getPlatformVersion', () async {
//     ArcgisMaps arcgisMapsPlugin = ArcgisMaps();
//     MockArcgisMapsPlatform fakePlatform = MockArcgisMapsPlatform();
//     ArcgisMapsPlatform.instance = fakePlatform;

//     expect(await arcgisMapsPlugin.getPlatformVersion(), '42');
//   });
// }
