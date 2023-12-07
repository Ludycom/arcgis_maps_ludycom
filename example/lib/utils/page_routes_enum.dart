// ignore_for_file: constant_identifier_names

enum PageRoutesEnum {
  home,
  basic_map,
  load_feature_tablet_service,
  load_portal_feature_layer,
  download_form_portal,
  load_local_files,
  select_features_in_feature_layer,
  manage_map,
  generate_geodatabase_replica_from_feature_service,
  set_points
}

extension PageRoutesEnumExtension on PageRoutesEnum {
  String get path {
    switch(this) {
      case PageRoutesEnum.home: return "/home";
      case PageRoutesEnum.basic_map: return "/basic_map";
      case PageRoutesEnum.load_feature_tablet_service: return "/load_feature_tablet_service";
      case PageRoutesEnum.load_portal_feature_layer: return "/load_portal_feature_layer";
      case PageRoutesEnum.download_form_portal: return "/download_form_portal";
      case PageRoutesEnum.load_local_files: return "/load_loca_files";
      case PageRoutesEnum.select_features_in_feature_layer: return "/select_features_in_feature_layer";
      case PageRoutesEnum.manage_map: return "/manage_map";
      case PageRoutesEnum.generate_geodatabase_replica_from_feature_service: return "/generate_geodatabase_replica_from_feature_service";
      case PageRoutesEnum.set_points: return "/set_points";
    }
  }
}