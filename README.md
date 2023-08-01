# ArcGIS Maps SDK by Ludycom S.A.S.

ArcGIS Maps SDK for Flutter is a plugin for ArcGIS Maps SDK for Kotlin and ArcGIS Maps SDK for Swift (Not available) created by Ludycom S.A.S. Currently in beta, it is planned to be used in production, the plugin will allow users to use ArcGIS maps through a customizable and optimized widget.

## Supported Elements

| Element | Android | iOS |
| ------ | ------ | ----- |
| BasemapStyle | :x: | :x: |
| API key | :white_check_mark:   | :x: |
| OAuth 2.0 | :x:   | :x: |
| Tokens | :x:   | :x: |
| Network Credentials | :x:   | :x: |
| Load Service Feature Table | :white_check_mark:      | :x: |
| Load PortalItem Feature Layer | :white_check_mark:   | :x: |
| Load GeoPackage Feature Layer | :white_check_mark:   | :x: |
| Load Shapefile Feature Layer | :white_check_mark:    | :x: |
| Remove Feature Layer | :white_check_mark:   | :x: |
| Remove All Feature Layers | :white_check_mark:   | :x: |
| Select features in feature layer | :white_check_mark:  | :x:  |
| Location | :white_check_mark:   | :x: |
| Download Portal Item | :white_check_mark:   | :x: |
| Check Local Portal Item Files | :white_check_mark:  | :x:  |

## Getting Started

### Requirements

The ArcGIS Maps is compatible with applications:

- Deployed on iOS 11 or higher (Not available)
- Built using the Android SDK 26 or higher

#### Android
```bash
buildscript {
    ext.kotlin_version = '1.7.20'
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath 'com.android.tools.build:gradle:7.4.2'
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven {
            url 'https://esri.jfrog.io/artifactory/arcgis'
        }
    }
}
```

#### iOS
```bash
Not available
```

### Examples

#### Basic map
![App Screenshot](https://github.com/Ludycom/arcgis_maps_ludycom/blob/Main/example/demo_assets/basic_map.jpg)

#### Load feature tablet service
![App Screenshot](https://github.com/Ludycom/arcgis_maps_ludycom/blob/Main/example/demo_assets/load_feature_tablet.jpg)

#### Load local files
##### GeoDatabase
![App Screenshot](https://github.com/Ludycom/arcgis_maps_ludycom/blob/Main/example/demo_assets/load_geodatabase.jpg)
##### GeoPackage
![App Screenshot](https://github.com/Ludycom/arcgis_maps_ludycom/blob/Main/example/demo_assets/load_geodatabase.jpg)
##### ShapeFile
![App Screenshot](https://github.com/Ludycom/arcgis_maps_ludycom/blob/Main/example/demo_assets/load_shapefile.jpg)

#### Load portal feature layer
![App Screenshot](https://via.placeholder.com/468x300?text=App+Screenshot+Here)

#### Select features in feature layer
![App Screenshot](https://github.com/Ludycom/arcgis_maps_ludycom/blob/Main/example/demo_assets/selection_0.jpg)
![App Screenshot](https://github.com/Ludycom/arcgis_maps_ludycom/blob/Main/example/demo_assets/selection_1.jpg)

#### Manage Map
![App Screenshot](https://github.com/Ludycom/arcgis_maps_ludycom/blob/Main/example/demo_assets/manage_map.gif)

#### Download from portal
![App Screenshot](https://github.com/Ludycom/arcgis_maps_ludycom/blob/Main/example/demo_assets/download_portalitem_0.jpg)
![App Screenshot](https://github.com/Ludycom/arcgis_maps_ludycom/blob/Main/example/demo_assets/download_portalitem_1.jpg)
![App Screenshot](https://github.com/Ludycom/arcgis_maps_ludycom/blob/Main/example/demo_assets/download_portalitem_2.jpg)