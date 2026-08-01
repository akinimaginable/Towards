// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_app_shared",
  platforms: [
    .iOS("15.0")
  ],
  products: [
    .library(
      name: "_app_shared",
      type: .none,
      targets: ["_app_shared"]
    )
  ],
  dependencies: [
    .package(
      url: "https://github.com/maplibre/maplibre-gl-native-distribution.git",
      exact: "6.28.0"
    )
  ],
  targets: [
    .target(
      name: "_app_shared",
      dependencies: [
        .product(
          name: "MapLibre",
          package: "maplibre-gl-native-distribution"
        )
      ]
    )
  ]
)
