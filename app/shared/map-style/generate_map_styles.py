#!/usr/bin/env python3
"""Generates the Towards light and dark MapLibre styles.

Both variants share the exact same layer stack (OpenFreeMap / OpenMapTiles schema);
only the palette differs. Edit the palettes or layers here, then run:

    python3 app/shared/map-style/generate_map_styles.py
"""

import json
from pathlib import Path

OUTPUT_DIR = Path(__file__).resolve().parent.parent / "src/commonMain/composeResources/files"

LIGHT = {
    "land": "#F6F4EF",
    "residential": "#EEEBE5",
    "commercial": "#F8E9EF",
    "industrial": "#F1ECDC",
    "railway_area": "#ECEAF0",
    "military": "#F6E3DF",
    "farmland": "#EEF2DD",
    "grass": "#DCEFCB",
    "park": "#C6E8B2",
    "wood": "#B2DC9C",
    "wetland": "#D0EBE2",
    "sand": "#F7EFCD",
    "rock": "#E6E4E0",
    "ice": "#F1F8FC",
    "cemetery": "#D7E7CF",
    "sport": "#C4EAC6",
    "school": "#F7EED3",
    "hospital": "#FCE3E3",
    "reserve": "#7FC36B",
    "water": "#A4D5F7",
    "water_label": "#2E70B3",
    "water_label_halo": "#E1F1FC",
    "building": "#E6E1D9",
    "building_outline": "#D7D0C6",
    "aeroway_area": "#ECE9F3",
    "runway": "#DAD5E7",
    "motorway": "#FFC46B",
    "motorway_casing": "#E3953A",
    "trunk": "#FFD98C",
    "trunk_casing": "#E5AC52",
    "primary": "#FFECA8",
    "primary_casing": "#E3C46A",
    "secondary": "#FFFFFF",
    "secondary_casing": "#D6CFC4",
    "minor": "#FFFFFF",
    "minor_casing": "#DCD6CD",
    "service": "#FFFFFF",
    "service_casing": "#E2DDD5",
    "pedestrian": "#F3EEF8",
    "pedestrian_casing": "#DCD1E8",
    "footway": "#B8AE9F",
    "cycleway": "#3E9BDB",
    "rail": "#A29DB4",
    "rail_hatch": "#F6F4EF",
    "transit": "#A68ED6",
    "ferry": "#4E9FDC",
    "tunnel_opacity": 0.45,
    "boundary_country": "#A586C4",
    "boundary_state": "#C5B5D8",
    "label_halo": "#FFFFFF",
    "city": "#20232A",
    "town": "#30333A",
    "village": "#474A52",
    "suburb": "#6A6E7A",
    "country": "#3B3354",
    "state": "#8B7BA3",
    "road_label": "#5B5750",
    "motorway_label": "#8A4A0C",
    "motorway_label_halo": "#FFE2B8",
    "housenumber": "#A1998E",
    "transit_label": "#1F62D0",
    "airport_label": "#5856D6",
    "poi_food": "#D9701A",
    "poi_shop": "#C0429A",
    "poi_nature": "#3A8A35",
    "poi_culture": "#17889A",
    "poi_health": "#D4403A",
    "poi_education": "#9A6A22",
    "poi_lodging": "#5856D6",
    "poi_default": "#6E6A63",
    "icon_opacity": 1,
}

DARK = {
    "land": "#1A1D23",
    "residential": "#1E2128",
    "commercial": "#2A1F2B",
    "industrial": "#27251D",
    "railway_area": "#22222B",
    "military": "#2D2020",
    "farmland": "#1E241B",
    "grass": "#1C2C22",
    "park": "#1C3727",
    "wood": "#1A3B27",
    "wetland": "#1A2F2E",
    "sand": "#2E2B1F",
    "rock": "#25262B",
    "ice": "#222A34",
    "cemetery": "#1E2C24",
    "sport": "#1D3B2B",
    "school": "#2B281E",
    "hospital": "#34212A",
    "reserve": "#3C8A50",
    "water": "#143D5F",
    "water_label": "#6EB1EA",
    "water_label_halo": "#0F2E49",
    "building": "#272B33",
    "building_outline": "#323741",
    "aeroway_area": "#24232E",
    "runway": "#34333F",
    "motorway": "#B06C2A",
    "motorway_casing": "#6E4318",
    "trunk": "#957030",
    "trunk_casing": "#5E461A",
    "primary": "#6F6139",
    "primary_casing": "#463D23",
    "secondary": "#4A505B",
    "secondary_casing": "#2C3037",
    "minor": "#3B4049",
    "minor_casing": "#272A31",
    "service": "#343840",
    "service_casing": "#25282E",
    "pedestrian": "#302C3A",
    "pedestrian_casing": "#403A4D",
    "footway": "#60656F",
    "cycleway": "#3D8FC6",
    "rail": "#6A6290",
    "rail_hatch": "#1A1D23",
    "transit": "#8F78C8",
    "ferry": "#3D86C2",
    "tunnel_opacity": 0.4,
    "boundary_country": "#8C71B3",
    "boundary_state": "#5F5374",
    "label_halo": "#1A1D23",
    "city": "#F2F3F5",
    "town": "#DADCE1",
    "village": "#B8BBC2",
    "suburb": "#8E929C",
    "country": "#D9CDF2",
    "state": "#9C8FB5",
    "road_label": "#B3B7BF",
    "motorway_label": "#FFD49A",
    "motorway_label_halo": "#5A3711",
    "housenumber": "#6E737C",
    "transit_label": "#70A9FF",
    "airport_label": "#A09EFF",
    "poi_food": "#FFA45A",
    "poi_shop": "#F27CC9",
    "poi_nature": "#7DCB72",
    "poi_culture": "#5FD0DE",
    "poi_health": "#FF7A70",
    "poi_education": "#E2B46A",
    "poi_lodging": "#A09EFF",
    "poi_default": "#A3A7AF",
    "icon_opacity": 0.8,
}

FONT_REGULAR = ["Noto Sans Regular"]
FONT_BOLD = ["Noto Sans Bold"]
FONT_ITALIC = ["Noto Sans Italic"]

POI_CATEGORIES = {
    "poi_food": ["restaurant", "cafe", "bar", "fast_food", "ice_cream", "beer", "bakery", "alcohol_shop"],
    "poi_shop": [
        "shop", "grocery", "clothing_store", "florist", "furniture", "gift", "jewelry", "mobile_phone",
        "hairdresser", "laundry", "butcher", "books", "music_shop",
    ],
    "poi_nature": ["park", "garden", "campsite", "picnic_site", "playground", "golf", "pitch", "dog_park", "zoo"],
    "poi_culture": [
        "museum", "art_gallery", "theatre", "cinema", "music", "attraction", "monument", "castle",
        "aquarium", "town_hall", "place_of_worship", "stadium", "swimming",
    ],
    "poi_health": ["hospital", "pharmacy", "doctors", "dentist", "veterinary", "police", "fire_station"],
    "poi_education": ["school", "college", "library", "kindergarten", "university"],
    "poi_lodging": ["lodging"],
}
# OpenMapTiles `rank` is a per-grid-cell ordering, not a global importance, so tiers are class-based.
POI_LANDMARKS = ["museum", "attraction", "monument", "castle", "zoo", "aquarium", "stadium", "hospital", "university"]
POI_OTHERS = sorted({c for classes in POI_CATEGORIES.values() for c in classes} - set(POI_LANDMARKS))

LINE = ["match", ["geometry-type"], ["LineString", "MultiLineString"], True, False]
POLYGON = ["match", ["geometry-type"], ["Polygon", "MultiPolygon"], True, False]
POINT = ["match", ["geometry-type"], ["Point", "MultiPoint"], True, False]


def name(separator="\n"):
    return [
        "case",
        ["has", "name:nonlatin"],
        ["concat", ["get", "name:latin"], separator, ["get", "name:nonlatin"]],
        ["coalesce", ["get", "name_en"], ["get", "name"]],
    ]


def zoom(*stops, base=None):
    curve = ["exponential", base] if base else ["linear"]
    return ["interpolate", curve, ["zoom"], *stops]


def match(prop, cases, default):
    expression = ["match", ["get", prop]]
    for keys, value in cases:
        expression += [keys, value]
    return expression + [default]


def is_in(prop, values):
    return ["match", ["get", prop], values, True, False]


def fill(layer_id, source_layer, color, filter_, minzoom=None, maxzoom=None, opacity=None, outline=None):
    layer = {
        "id": layer_id,
        "type": "fill",
        "source": "openmaptiles",
        "source-layer": source_layer,
        "filter": ["all", POLYGON, *filter_],
        "paint": {"fill-color": color},
    }
    if opacity is not None:
        layer["paint"]["fill-opacity"] = opacity
    if outline is not None:
        layer["paint"]["fill-outline-color"] = outline
    if minzoom is not None:
        layer["minzoom"] = minzoom
    if maxzoom is not None:
        layer["maxzoom"] = maxzoom
    return layer


# Road widths in px for [z5, z8, z10, z12, z14, z16, z18, z20].
ROAD_ZOOMS = [5, 8, 10, 12, 14, 16, 18, 20]
ROAD_WIDTHS = {
    "motorway": [0.6, 1.2, 2.0, 3.0, 5.0, 10, 20, 38],
    "trunk": [0.5, 1.0, 1.8, 2.8, 4.6, 9, 18, 34],
    "primary": [0.2, 0.5, 1.2, 2.4, 4.2, 8, 16, 30],
    "secondary": [0, 0.4, 1.0, 1.9, 3.6, 7, 14, 26],
    "tertiary": [0, 0, 0.6, 1.5, 3.0, 6, 12, 22],
    "minor": [0, 0, 0, 0.7, 1.8, 4.5, 9.5, 18],
    "service": [0, 0, 0, 0, 0.9, 2.4, 5.5, 11],
}
ROAD_CLASSES = list(ROAD_WIDTHS)
ROAD_RANK = {"motorway": 10, "trunk": 9, "primary": 8, "secondary": 7, "tertiary": 6, "minor": 5, "service": 4}
ROAD_GROUPS = {
    "motorway": ["motorway"],
    "trunk": ["trunk"],
    "primary": ["primary"],
    "secondary": ["secondary"],
    "tertiary": ["tertiary"],
    "minor": ["minor"],
    "service": ["service", "track", "busway", "bus_guideway", "raceway"],
}
ALL_ROAD_CLASSES = [c for group in ROAD_GROUPS.values() for c in group]


def road_width(scale=1.0):
    stops = []
    for index, z in enumerate(ROAD_ZOOMS):
        per_class = match(
            "class",
            [(ROAD_GROUPS[c], round(ROAD_WIDTHS[c][index] * scale, 2)) for c in ROAD_CLASSES],
            0,
        )
        stops += [z, ["*", ["case", ["==", ["get", "ramp"], 1], 0.6, 1], per_class]]
    return ["interpolate", ["exponential", 1.5], ["zoom"], *stops]


def road_color(p, suffix=""):
    return match("class", [(ROAD_GROUPS[c], p[c.replace("tertiary", "secondary") + suffix]) for c in ROAD_CLASSES], p["minor" + suffix])


def road_sort_key():
    return match("class", [(ROAD_GROUPS[c], ROAD_RANK[c]) for c in ROAD_CLASSES], 0)


def brunnel_filter(level):
    if level == "tunnel":
        return ["==", ["get", "brunnel"], "tunnel"]
    if level == "bridge":
        return ["==", ["get", "brunnel"], "bridge"]
    return ["!", is_in("brunnel", ["bridge", "tunnel"])]


def road_layers(p, level):
    road_filter = ["all", LINE, is_in("class", ALL_ROAD_CLASSES), brunnel_filter(level)]
    casing = {
        "id": f"road_{level}_casing",
        "type": "line",
        "source": "openmaptiles",
        "source-layer": "transportation",
        "minzoom": 5,
        "filter": road_filter,
        "layout": {"line-cap": "butt" if level == "tunnel" else "round", "line-join": "round", "line-sort-key": road_sort_key()},
        "paint": {
            "line-color": road_color(p, "_casing"),
            "line-gap-width": road_width(),
            "line-width": zoom(10, 0.5, 14, 1, 18, 2),
            "line-opacity": zoom(10, 0, 11.5, 1),
        },
    }
    inner = {
        "id": f"road_{level}",
        "type": "line",
        "source": "openmaptiles",
        "source-layer": "transportation",
        "minzoom": 5,
        "filter": road_filter,
        "layout": {"line-cap": "round", "line-join": "round", "line-sort-key": road_sort_key()},
        "paint": {"line-color": road_color(p), "line-width": road_width()},
    }
    if level == "tunnel":
        casing["paint"]["line-dasharray"] = [0.6, 0.4]
        casing["paint"]["line-opacity"] = p["tunnel_opacity"]
        inner["paint"]["line-opacity"] = p["tunnel_opacity"]
    return [casing, inner]


def path_layers(p, level):
    base = ["all", LINE, ["==", ["get", "class"], "path"], brunnel_filter(level)]
    opacity = p["tunnel_opacity"] if level == "tunnel" else 1
    pedestrian = {
        "id": f"path_{level}_pedestrian",
        "type": "line",
        "source": "openmaptiles",
        "source-layer": "transportation",
        "minzoom": 14,
        "filter": [*base, ["==", ["get", "subclass"], "pedestrian"]],
        "layout": {"line-cap": "round", "line-join": "round"},
        "paint": {
            "line-color": p["pedestrian"],
            "line-width": zoom(14, 1.5, 16, 4, 18, 9, 20, 18, base=1.5),
            "line-opacity": opacity,
        },
    }
    cycleway = {
        "id": f"path_{level}_cycleway",
        "type": "line",
        "source": "openmaptiles",
        "source-layer": "transportation",
        "minzoom": 14,
        "filter": [*base, ["==", ["get", "subclass"], "cycleway"]],
        "layout": {"line-cap": "round", "line-join": "round"},
        "paint": {
            "line-color": p["cycleway"],
            "line-width": zoom(14, 0.8, 16, 1.6, 20, 4, base=1.3),
            "line-opacity": opacity * 0.85,
        },
    }
    footway = {
        "id": f"path_{level}_footway",
        "type": "line",
        "source": "openmaptiles",
        "source-layer": "transportation",
        "minzoom": 15,
        "filter": [*base, ["!", is_in("subclass", ["pedestrian", "cycleway", "platform"])]],
        "layout": {"line-join": "round"},
        "paint": {
            "line-color": p["footway"],
            "line-dasharray": [2, 1.5],
            "line-width": zoom(15, 0.6, 17, 1.2, 20, 2.5, base=1.3),
            "line-opacity": zoom(15, 0.4 * opacity, 17, 0.8 * opacity),
        },
    }
    return [pedestrian, cycleway, footway]


def rail_layers(p, level):
    opacity = p["tunnel_opacity"] if level == "tunnel" else 1
    rail_filter = ["all", LINE, ["==", ["get", "class"], "rail"], brunnel_filter(level)]
    transit_filter = ["all", LINE, ["==", ["get", "class"], "transit"], brunnel_filter(level)]
    rail_width = zoom(10, 0.6, 14, 1.6, 16, 3, 20, 7, base=1.3)
    layers = [
        {
            "id": f"rail_{level}",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "transportation",
            "minzoom": 9,
            "filter": rail_filter,
            "layout": {"line-join": "round"},
            "paint": {"line-color": p["rail"], "line-width": rail_width, "line-opacity": opacity},
        },
        {
            "id": f"rail_{level}_hatch",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "transportation",
            "minzoom": 14,
            "filter": rail_filter,
            "layout": {"line-join": "round"},
            "paint": {
                "line-color": p["rail_hatch"],
                "line-dasharray": [0.2, 4],
                "line-width": zoom(14, 4, 16, 7, 20, 14, base=1.3),
                "line-opacity": opacity,
            },
        },
        {
            "id": f"transit_{level}",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "transportation",
            "minzoom": 12,
            "filter": transit_filter,
            "layout": {"line-join": "round", "line-cap": "round"},
            "paint": {
                "line-color": p["transit"],
                "line-width": zoom(12, 0.6, 16, 2, 20, 5, base=1.3),
                "line-opacity": opacity * 0.9,
            },
        },
    ]
    if level == "tunnel":
        layers = [layers[0], layers[2]]
        for layer in layers:
            layer["paint"]["line-dasharray"] = [2, 2]
            layer["paint"]["line-opacity"] = opacity * 0.6
        layers[0]["paint"]["line-width"] = zoom(10, 0.4, 14, 1, 16, 1.6, 20, 3, base=1.3)
    return layers


def symbol(layer_id, source_layer, filter_, layout, paint, minzoom=None, maxzoom=None):
    layer = {
        "id": layer_id,
        "type": "symbol",
        "source": "openmaptiles",
        "source-layer": source_layer,
        "filter": filter_,
        "layout": layout,
        "paint": paint,
    }
    if minzoom is not None:
        layer["minzoom"] = minzoom
    if maxzoom is not None:
        layer["maxzoom"] = maxzoom
    return layer


def text_paint(color, halo, width=1.4, blur=0.5):
    return {"text-color": color, "text-halo-color": halo, "text-halo-width": width, "text-halo-blur": blur}


def poi_color(p):
    return match("class", [(classes, p[key]) for key, classes in POI_CATEGORIES.items()], p["poi_default"])


def poi_layer(p, layer_id, classes, minzoom):
    filter_ = ["all", POINT, is_in("class", classes), ["has", "name"]]
    return symbol(
        layer_id,
        "poi",
        filter_,
        {
            "icon-image": ["coalesce", ["image", ["concat", ["get", "class"], "_11"]], ["image", "circle_11"]],
            "icon-size": zoom(15, 0.8, 18, 1),
            "icon-padding": 4,
            "symbol-sort-key": ["get", "rank"],
            "text-field": name(),
            "text-font": FONT_REGULAR,
            "text-size": zoom(15, 10.5, 18, 12.5),
            "text-max-width": 8,
            "text-anchor": "top",
            "text-offset": [0, 0.8],
            "text-padding": 6,
        },
        {**text_paint(poi_color(p), p["label_halo"]), "icon-opacity": p["icon_opacity"]},
        minzoom=minzoom,
    )


def place_layer(p, layer_id, filter_, color, font, size, minzoom=None, maxzoom=None, uppercase=False, extra=None):
    layout = {
        "text-field": name(),
        "text-font": font,
        "text-size": size,
        "text-max-width": 8,
        "symbol-sort-key": ["coalesce", ["get", "rank"], 99],
    }
    if uppercase:
        layout["text-transform"] = "uppercase"
        layout["text-letter-spacing"] = 0.08
    if extra:
        layout.update(extra)
    return symbol(
        layer_id,
        "place",
        ["all", POINT, *filter_],
        layout,
        text_paint(color, p["label_halo"], width=1.6),
        minzoom=minzoom,
        maxzoom=maxzoom,
    )


def build(p, variant):
    layers = [
        {"id": "background", "type": "background", "paint": {"background-color": p["land"]}},
        fill(
            "landuse_residential",
            "landuse",
            p["residential"],
            [is_in("class", ["residential", "suburb", "quarter", "neighbourhood"])],
            opacity=zoom(6, 0.6, 12, 1),
        ),
        fill("landuse_commercial", "landuse", p["commercial"], [is_in("class", ["commercial", "retail"])], minzoom=11),
        fill("landuse_industrial", "landuse", p["industrial"], [is_in("class", ["industrial", "garages", "quarry", "dam"])], minzoom=11),
        fill("landuse_railway", "landuse", p["railway_area"], [["==", ["get", "class"], "railway"]], minzoom=12),
        fill("landuse_military", "landuse", p["military"], [["==", ["get", "class"], "military"]], minzoom=10),
        fill("landcover_farmland", "landcover", p["farmland"], [["==", ["get", "class"], "farmland"]], opacity=zoom(8, 0.5, 12, 1)),
        fill("landcover_sand", "landcover", p["sand"], [["==", ["get", "class"], "sand"]]),
        fill("landcover_rock", "landcover", p["rock"], [["==", ["get", "class"], "rock"]]),
        fill("landcover_ice", "landcover", p["ice"], [["==", ["get", "class"], "ice"]]),
        fill(
            "landcover_grass",
            "landcover",
            match(
                "subclass",
                [(["park", "garden", "village_green", "recreation_ground", "golf_course", "playground"], p["park"])],
                p["grass"],
            ),
            [["==", ["get", "class"], "grass"]],
        ),
        fill("landcover_wood", "landcover", p["wood"], [["==", ["get", "class"], "wood"]], opacity=zoom(6, 0.6, 12, 1)),
        fill("landcover_wetland", "landcover", p["wetland"], [["==", ["get", "class"], "wetland"]]),
        fill("landuse_cemetery", "landuse", p["cemetery"], [["==", ["get", "class"], "cemetery"]], minzoom=12),
        fill("landuse_sport", "landuse", p["sport"], [is_in("class", ["pitch", "playground", "track", "stadium"])], minzoom=13),
        fill(
            "landuse_education",
            "landuse",
            p["school"],
            [is_in("class", ["school", "university", "college", "kindergarten", "library"])],
            minzoom=12,
        ),
        fill("landuse_hospital", "landuse", p["hospital"], [["==", ["get", "class"], "hospital"]], minzoom=12),
        fill("park_reserve", "park", p["reserve"], [], minzoom=6, opacity=zoom(6, 0.08, 12, 0.14)),
        {
            "id": "park_reserve_outline",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "park",
            "minzoom": 8,
            "filter": POLYGON,
            "paint": {
                "line-color": p["reserve"],
                "line-dasharray": [2, 1.5],
                "line-width": zoom(8, 0.6, 14, 1.4),
                "line-opacity": 0.5,
            },
        },
        fill("water", "water", p["water"], [["!=", ["get", "brunnel"], "tunnel"]]),
        {
            "id": "waterway",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "waterway",
            "filter": ["all", LINE, ["!=", ["get", "brunnel"], "tunnel"]],
            "layout": {"line-cap": "round", "line-join": "round"},
            "paint": {
                "line-color": p["water"],
                "line-width": [
                    "interpolate", ["exponential", 1.4], ["zoom"],
                    8, match("class", [(["river"], 1), (["canal"], 0.8)], 0),
                    13, match("class", [(["river"], 3), (["canal"], 2.5)], 1),
                    20, match("class", [(["river"], 20), (["canal"], 16)], 6),
                ],
            },
        },
        fill("aeroway_area", "aeroway", p["aeroway_area"], [is_in("class", ["aerodrome", "heliport", "apron"])], minzoom=10),
        {
            "id": "aeroway_runway",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "aeroway",
            "minzoom": 10,
            "filter": ["all", LINE, is_in("class", ["runway", "taxiway"])],
            "paint": {
                "line-color": p["runway"],
                "line-width": [
                    "interpolate", ["exponential", 1.5], ["zoom"],
                    10, match("class", [(["runway"], 1.5)], 0.5),
                    14, match("class", [(["runway"], 12)], 3),
                    18, match("class", [(["runway"], 80)], 20),
                ],
            },
        },
        fill(
            "building",
            "building",
            p["building"],
            [],
            minzoom=13,
            opacity=zoom(13, 0, 14.5, 1),
            outline=p["building_outline"],
        ),
        fill("pier", "transportation", p["land"], [["==", ["get", "class"], "pier"]], minzoom=13),
        *path_layers(p, "tunnel"),
        *road_layers(p, "tunnel"),
        *rail_layers(p, "tunnel"),
        *path_layers(p, "ground"),
        *road_layers(p, "ground"),
        *rail_layers(p, "ground"),
        *path_layers(p, "bridge"),
        *road_layers(p, "bridge"),
        *rail_layers(p, "bridge"),
        {
            "id": "ferry",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "transportation",
            "minzoom": 10,
            "filter": ["all", LINE, ["==", ["get", "class"], "ferry"]],
            "layout": {"line-join": "round"},
            "paint": {"line-color": p["ferry"], "line-dasharray": [2, 2], "line-width": zoom(10, 0.8, 16, 1.6)},
        },
        symbol(
            "road_oneway",
            "transportation",
            ["all", LINE, is_in("oneway", [1, -1]), is_in("class", ALL_ROAD_CLASSES)],
            {
                "icon-image": "oneway",
                "icon-rotate": ["match", ["get", "oneway"], -1, 180, 0],
                "icon-rotation-alignment": "map",
                "icon-size": zoom(16, 0.5, 19, 0.8),
                "icon-padding": 2,
                "symbol-placement": "line",
                "symbol-spacing": 220,
            },
            {"icon-opacity": 0.35},
            minzoom=16,
        ),
        {
            "id": "boundary_state",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "boundary",
            "minzoom": 4,
            "filter": ["all", ["==", ["get", "admin_level"], 4], ["!=", ["get", "maritime"], 1]],
            "layout": {"line-join": "round"},
            "paint": {
                "line-color": p["boundary_state"],
                "line-dasharray": [3, 2],
                "line-width": zoom(4, 0.6, 10, 1.2, 16, 2),
            },
        },
        {
            "id": "boundary_country",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "boundary",
            "filter": ["all", ["==", ["get", "admin_level"], 2], ["!=", ["get", "maritime"], 1], ["!=", ["get", "disputed"], 1]],
            "layout": {"line-cap": "round", "line-join": "round"},
            "paint": {"line-color": p["boundary_country"], "line-width": zoom(2, 0.8, 6, 1.4, 12, 2.4, 18, 4)},
        },
        {
            "id": "boundary_country_disputed",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "boundary",
            "filter": ["all", ["==", ["get", "admin_level"], 2], ["==", ["get", "disputed"], 1]],
            "paint": {
                "line-color": p["boundary_country"],
                "line-dasharray": [2, 2],
                "line-width": zoom(2, 0.8, 6, 1.4, 12, 2.4),
            },
        },
        symbol(
            "waterway_name",
            "waterway",
            ["all", LINE, ["has", "name"]],
            {
                "symbol-placement": "line",
                "symbol-spacing": 400,
                "text-field": name(" "),
                "text-font": FONT_ITALIC,
                "text-size": zoom(12, 10, 16, 13),
                "text-letter-spacing": 0.1,
            },
            text_paint(p["water_label"], p["water_label_halo"]),
            minzoom=12,
        ),
        symbol(
            "water_name_line",
            "water_name",
            ["all", LINE],
            {
                "symbol-placement": "line",
                "symbol-spacing": 400,
                "text-field": name(" "),
                "text-font": FONT_ITALIC,
                "text-size": 12,
                "text-letter-spacing": 0.1,
            },
            text_paint(p["water_label"], p["water_label_halo"]),
        ),
        symbol(
            "water_name_point",
            "water_name",
            ["all", POINT, ["!=", ["get", "class"], "ocean"]],
            {
                "text-field": name(),
                "text-font": FONT_ITALIC,
                "text-size": zoom(4, 10, 12, 12, 16, 14),
                "text-max-width": 6,
                "text-letter-spacing": 0.1,
            },
            text_paint(p["water_label"], p["water_label_halo"]),
        ),
        symbol(
            "water_name_ocean",
            "water_name",
            ["all", POINT, ["==", ["get", "class"], "ocean"]],
            {
                "text-field": name(),
                "text-font": FONT_ITALIC,
                "text-size": zoom(0, 11, 4, 15),
                "text-max-width": 6,
                "text-letter-spacing": 0.2,
            },
            text_paint(p["water_label"], p["water_label_halo"]),
        ),
        symbol(
            "housenumber",
            "housenumber",
            ["all", POINT],
            {"text-field": ["to-string", ["get", "housenumber"]], "text-font": FONT_REGULAR, "text-size": 10, "text-padding": 4},
            text_paint(p["housenumber"], p["label_halo"], width=1),
            minzoom=17,
        ),
        symbol(
            "road_name",
            "transportation_name",
            ["all", LINE, ["has", "name"], ["!", is_in("class", ["motorway", "rail", "transit", "ferry"])]],
            {
                "symbol-placement": "line",
                "symbol-spacing": 300,
                "text-field": name(" "),
                "text-font": FONT_REGULAR,
                "text-size": zoom(13, 10, 16, 12, 19, 14),
                "text-max-angle": 30,
                "text-padding": 2,
                "text-rotation-alignment": "map",
                "symbol-sort-key": road_sort_key(),
            },
            text_paint(p["road_label"], p["label_halo"], width=1.5),
            minzoom=13,
        ),
        symbol(
            "road_ref_motorway",
            "transportation_name",
            ["all", LINE, is_in("class", ["motorway", "trunk"]), ["has", "ref"], ["<=", ["get", "ref_length"], 6]],
            {
                "symbol-placement": "line",
                "symbol-spacing": 400,
                "text-field": ["to-string", ["get", "ref"]],
                "text-font": FONT_BOLD,
                "text-size": zoom(8, 9, 14, 11),
                "text-rotation-alignment": "viewport",
                "text-padding": 6,
            },
            text_paint(p["motorway_label"], p["motorway_label_halo"], width=2.5, blur=0),
            minzoom=8,
        ),
        symbol(
            "airport",
            "aerodrome_label",
            ["all", POINT, is_in("class", ["international", "public", "regional"]), ["has", "iata"]],
            {
                "icon-image": "airport_11",
                "text-field": name(),
                "text-font": FONT_REGULAR,
                "text-size": 11,
                "text-anchor": "top",
                "text-offset": [0, 0.8],
                "text-max-width": 8,
                "text-optional": True,
            },
            {**text_paint(p["airport_label"], p["label_halo"]), "icon-opacity": p["icon_opacity"]},
            minzoom=9,
        ),
        symbol(
            "park_name",
            "park",
            ["all", POINT, ["has", "name"]],
            {
                "text-field": name(),
                "text-font": FONT_ITALIC,
                "text-size": 11,
                "text-max-width": 8,
                "symbol-sort-key": ["get", "rank"],
            },
            text_paint(p["poi_nature"], p["label_halo"]),
            minzoom=10,
        ),
        poi_layer(p, "poi", POI_OTHERS, minzoom=16.5),
        poi_layer(p, "poi_landmark", POI_LANDMARKS, minzoom=15),
        symbol(
            "transit_bus_stop",
            "poi",
            ["all", POINT, ["==", ["get", "class"], "bus"], ["has", "name"]],
            {
                "icon-image": "bus_11",
                "icon-size": 0.85,
                "text-field": name(),
                "text-font": FONT_REGULAR,
                "text-size": 10.5,
                "text-anchor": "top",
                "text-offset": [0, 0.8],
                "text-max-width": 8,
                "text-optional": True,
                "symbol-sort-key": ["get", "rank"],
            },
            {**text_paint(p["transit_label"], p["label_halo"]), "icon-opacity": p["icon_opacity"]},
            minzoom=16,
        ),
        symbol(
            "transit_station",
            "poi",
            ["all", POINT, ["==", ["get", "class"], "railway"], is_in("subclass", ["station", "halt", "subway", "tram_stop", "light_rail"])],
            {
                "icon-image": match(
                    "subclass",
                    [(["subway"], "railway_metro_11"), (["tram_stop", "light_rail"], "railway_light_11")],
                    "railway_11",
                ),
                "icon-size": zoom(12, 0.8, 16, 1),
                "text-field": name(),
                "text-font": FONT_BOLD,
                "text-size": zoom(12, 10, 16, 12),
                "text-anchor": "top",
                "text-offset": [0, 0.8],
                "text-max-width": 8,
                "text-optional": True,
                "symbol-sort-key": [
                    "+",
                    match("subclass", [(["station", "halt"], 0), (["subway"], 100)], 200),
                    ["coalesce", ["get", "rank"], 0],
                ],
            },
            {
                **text_paint(p["transit_label"], p["label_halo"], width=1.6),
                "icon-opacity": p["icon_opacity"],
                "text-opacity": ["step", ["zoom"], ["match", ["get", "subclass"], ["station", "halt"], 1, 0], 14, 1],
            },
            minzoom=12,
        ),
        place_layer(
            p,
            "place_neighbourhood",
            [is_in("class", ["neighbourhood", "quarter", "hamlet", "isolated_dwelling"])],
            p["suburb"],
            FONT_REGULAR,
            zoom(13, 10, 16, 12),
            minzoom=13,
            maxzoom=17,
            uppercase=True,
        ),
        place_layer(
            p,
            "place_suburb",
            [["==", ["get", "class"], "suburb"]],
            p["suburb"],
            FONT_REGULAR,
            zoom(11, 10, 15, 13),
            minzoom=11,
            maxzoom=16,
            uppercase=True,
        ),
        place_layer(p, "place_village", [["==", ["get", "class"], "village"]], p["village"], FONT_REGULAR, zoom(10, 10, 15, 14), minzoom=10, maxzoom=16),
        place_layer(p, "place_town", [["==", ["get", "class"], "town"]], p["town"], FONT_REGULAR, zoom(8, 11, 15, 16), minzoom=7, maxzoom=16),
        place_layer(
            p,
            "place_city",
            [["==", ["get", "class"], "city"]],
            p["city"],
            FONT_BOLD,
            ["interpolate", ["linear"], ["zoom"], 4, ["case", ["<=", ["get", "rank"], 3], 13, 11], 12, ["case", ["<=", ["get", "rank"], 3], 22, 17]],
            minzoom=3,
            maxzoom=15,
        ),
        place_layer(
            p,
            "place_state",
            [["==", ["get", "class"], "state"]],
            p["state"],
            FONT_REGULAR,
            zoom(4, 10, 8, 13),
            minzoom=4,
            maxzoom=9,
            uppercase=True,
        ),
        place_layer(
            p,
            "place_country",
            [["==", ["get", "class"], "country"]],
            p["country"],
            FONT_BOLD,
            ["interpolate", ["linear"], ["zoom"], 1, ["case", ["<=", ["get", "rank"], 2], 11, 9], 6, ["case", ["<=", ["get", "rank"], 2], 17, 13]],
            maxzoom=9,
        ),
    ]
    return {
        "version": 8,
        "name": f"Towards {variant.capitalize()}",
        "metadata": {"towards:variant": variant},
        "sources": {"openmaptiles": {"type": "vector", "url": "https://tiles.openfreemap.org/planet"}},
        "sprite": "https://tiles.openfreemap.org/sprites/ofm_f384/ofm",
        "glyphs": "https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf",
        "layers": layers,
    }


def main():
    for variant, palette in (("light", LIGHT), ("dark", DARK)):
        path = OUTPUT_DIR / f"towards_map_{variant}.json"
        path.write_text(json.dumps(build(palette, variant), indent=2) + "\n")
        print(f"Wrote {path}")


if __name__ == "__main__":
    main()
