package org.etrange.towards.motis

/** Average pedestrian speed in meters per second. */
typealias PedestrianSpeed = Double

/** Average cycling speed in meters per second. */
typealias CyclingSpeed = Double

/**
 * Matched token range as `[fromIndex, length]`.
 */
typealias Token = List<Double>

/**
 * Pareto set of optimal transit solutions.
 */
typealias ParetoSet = List<ParetoSetEntry>

/**
 * Multi-polygon: list of polygons, each a list of rings encoded as polylines (precision 6).
 * For each polygon, the first ring is the outer ring; subsequent rings are holes.
 */
typealias MultiPolygon = List<List<EncodedPolyline>>
