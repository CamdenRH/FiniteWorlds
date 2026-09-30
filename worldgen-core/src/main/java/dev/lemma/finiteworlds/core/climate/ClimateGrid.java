package dev.lemma.finiteworlds.core.climate;

/**
 * Persistent climate fields aligned 1:1 with the WorldBlueprint grid.
 *
 * Pass 3A stores annualized baseline temperature only. Later Phase-3 passes
 * can add atmospheric moisture, precipitation, rain-shadow, aridity, runoff,
 * and bioclimatic classification without changing the terrain API.
 */
public final class ClimateGrid {

    private final int resolution;

    private final float[] temperatureCelsius;
    private final float[] normalizedTemperature;
    private final float[] latitudeTemperatureCelsius;
    private final float[] elevationCoolingCelsius;
    private final float[] continentalAdjustmentCelsius;
    private final float[] moistureSource;
    private final float[] transportedMoisture;
    private final float[] windX;
    private final float[] windZ;
    private final float[] orographicLift;
    private final float[] orographicPrecipitation;
    private final float[] postOrographicMoisture;
    private final float[] rainShadowStrength;
    private final float[] refinedPrecipitation;
    private final float[] refinedMoisture;
    private final float[] runoffPotential;
    private final float[] snowStorageFraction;
    private final float[] effectiveDischarge;
    private final float[] specificDischarge;
    private final float[] normalizedEffectiveDischarge;
    private final byte[] bioclimaticRegion;

    private boolean moistureTransportPlanned;
    private boolean orographicPrecipitationPlanned;
    private boolean rainShadowRefinementPlanned;
    private boolean runoffDischargePlanned;
    private boolean bioclimaticRegionsPlanned;

    private double maximumEffectiveDischarge;

    public ClimateGrid(
            int resolution
    ) {
        this.resolution =
                resolution;

        int cellCount =
                resolution * resolution;

        this.temperatureCelsius =
                new float[cellCount];

        this.normalizedTemperature =
                new float[cellCount];

        this.latitudeTemperatureCelsius =
                new float[cellCount];

        this.elevationCoolingCelsius =
                new float[cellCount];

        this.continentalAdjustmentCelsius =
                new float[cellCount];

        this.moistureSource =
                new float[cellCount];

        this.transportedMoisture =
                new float[cellCount];

        this.windX =
                new float[cellCount];

        this.windZ =
                new float[cellCount];

        this.orographicLift =
                new float[cellCount];

        this.orographicPrecipitation =
                new float[cellCount];

        this.postOrographicMoisture =
                new float[cellCount];

        this.rainShadowStrength =
                new float[cellCount];

        this.refinedPrecipitation =
                new float[cellCount];

        this.refinedMoisture =
                new float[cellCount];

        this.runoffPotential =
                new float[cellCount];

        this.snowStorageFraction =
                new float[cellCount];

        this.effectiveDischarge =
                new float[cellCount];

        this.specificDischarge =
                new float[cellCount];

        this.normalizedEffectiveDischarge =
                new float[cellCount];

        this.bioclimaticRegion =
                new byte[cellCount];

        this.maximumEffectiveDischarge =
                0.0;
    }

    public int resolution() {
        return resolution;
    }

    public float temperatureCelsius(
            int x,
            int z
    ) {
        return temperatureCelsius[index(x, z)];
    }

    public float normalizedTemperature(
            int x,
            int z
    ) {
        return normalizedTemperature[index(x, z)];
    }

    public float latitudeTemperatureCelsius(
            int x,
            int z
    ) {
        return latitudeTemperatureCelsius[index(x, z)];
    }

    public float elevationCoolingCelsius(
            int x,
            int z
    ) {
        return elevationCoolingCelsius[index(x, z)];
    }

    public float continentalAdjustmentCelsius(
            int x,
            int z
    ) {
        return continentalAdjustmentCelsius[index(x, z)];
    }

    public float moistureSource(
            int x,
            int z
    ) {
        return moistureSource[index(x, z)];
    }

    public float transportedMoisture(
            int x,
            int z
    ) {
        return transportedMoisture[index(x, z)];
    }

    public float windX(
            int x,
            int z
    ) {
        return windX[index(x, z)];
    }

    public float windZ(
            int x,
            int z
    ) {
        return windZ[index(x, z)];
    }

    public float orographicLift(
            int x,
            int z
    ) {
        return orographicLift[index(x, z)];
    }

    public float orographicPrecipitation(
            int x,
            int z
    ) {
        return orographicPrecipitation[index(x, z)];
    }

    public float postOrographicMoisture(
            int x,
            int z
    ) {
        return postOrographicMoisture[index(x, z)];
    }

    public float rainShadowStrength(
            int x,
            int z
    ) {
        return rainShadowStrength[index(x, z)];
    }

    public float refinedPrecipitation(
            int x,
            int z
    ) {
        return refinedPrecipitation[index(x, z)];
    }

    public float refinedMoisture(
            int x,
            int z
    ) {
        return refinedMoisture[index(x, z)];
    }

    public float runoffPotential(
            int x,
            int z
    ) {
        return runoffPotential[index(x, z)];
    }

    public float snowStorageFraction(
            int x,
            int z
    ) {
        return snowStorageFraction[index(x, z)];
    }

    public float effectiveDischarge(
            int x,
            int z
    ) {
        return effectiveDischarge[index(x, z)];
    }

    public float specificDischarge(
            int x,
            int z
    ) {
        return specificDischarge[index(x, z)];
    }

    public float normalizedEffectiveDischarge(
            int x,
            int z
    ) {
        return normalizedEffectiveDischarge[index(x, z)];
    }

    public double maximumEffectiveDischarge() {
        return maximumEffectiveDischarge;
    }

    public boolean runoffDischargePlanned() {
        return runoffDischargePlanned;
    }

    public BioclimaticRegion bioclimaticRegion(
            int x,
            int z
    ) {
        return BioclimaticRegion.fromOrdinal(
                Byte.toUnsignedInt(
                        bioclimaticRegion[index(x, z)]
                )
        );
    }

    public boolean bioclimaticRegionsPlanned() {
        return bioclimaticRegionsPlanned;
    }

    public boolean rainShadowRefinementPlanned() {
        return rainShadowRefinementPlanned;
    }

    public boolean orographicPrecipitationPlanned() {
        return orographicPrecipitationPlanned;
    }

    public boolean moistureTransportPlanned() {
        return moistureTransportPlanned;
    }

    void setTemperature(
            int x,
            int z,
            float temperature,
            float normalized,
            float latitudeTemperature,
            float elevationCooling,
            float continentalAdjustment
    ) {
        int index =
                index(x, z);

        temperatureCelsius[index] =
                temperature;

        normalizedTemperature[index] =
                normalized;

        latitudeTemperatureCelsius[index] =
                latitudeTemperature;

        elevationCoolingCelsius[index] =
                elevationCooling;

        continentalAdjustmentCelsius[index] =
                continentalAdjustment;
    }

    void setMoistureTransport(
            int x,
            int z,
            float source,
            float transported,
            float localWindX,
            float localWindZ
    ) {
        int index =
                index(x, z);

        moistureSource[index] =
                source;

        transportedMoisture[index] =
                transported;

        windX[index] =
                localWindX;

        windZ[index] =
                localWindZ;
    }

    void finishMoistureTransport() {
        moistureTransportPlanned =
                true;
    }

    void setOrographicPrecipitation(
            int x,
            int z,
            float lift,
            float precipitation,
            float remainingMoisture
    ) {
        int index =
                index(x, z);

        orographicLift[index] =
                lift;

        orographicPrecipitation[index] =
                precipitation;

        postOrographicMoisture[index] =
                remainingMoisture;
    }

    void finishOrographicPrecipitation() {
        orographicPrecipitationPlanned =
                true;
    }

    void setRainShadowRefinement(
            int x,
            int z,
            float shadowStrength,
            float precipitation,
            float moisture
    ) {
        int index =
                index(x, z);

        rainShadowStrength[index] =
                shadowStrength;

        refinedPrecipitation[index] =
                precipitation;

        refinedMoisture[index] =
                moisture;
    }

    void finishRainShadowRefinement() {
        rainShadowRefinementPlanned =
                true;
    }

    void setRunoffDischargeCell(
            int x,
            int z,
            float localRunoff,
            float snowStorage,
            float discharge,
            float specific,
            float normalizedDischarge
    ) {
        int index =
                index(x, z);

        runoffPotential[index] =
                localRunoff;

        snowStorageFraction[index] =
                snowStorage;

        effectiveDischarge[index] =
                discharge;

        specificDischarge[index] =
                specific;

        normalizedEffectiveDischarge[index] =
                normalizedDischarge;
    }

    void finishRunoffDischarge(
            double maximumDischarge
    ) {
        maximumEffectiveDischarge =
                maximumDischarge;

        runoffDischargePlanned =
                true;
    }

    void setBioclimaticRegion(
            int x,
            int z,
            BioclimaticRegion region
    ) {
        bioclimaticRegion[index(x, z)] =
                (byte) region.ordinal();
    }

    void finishBioclimaticRegions() {
        bioclimaticRegionsPlanned =
                true;
    }

    public double minimumTransportedMoisture() {
        double result = Double.POSITIVE_INFINITY;

        for (float value : transportedMoisture) {
            result = Math.min(result, value);
        }

        return Double.isFinite(result) ? result : 0.0;
    }

    public double minimumTemperatureCelsius() {
        double result =
                Double.POSITIVE_INFINITY;

        for (float value : temperatureCelsius) {
            result =
                    Math.min(
                            result,
                            value
                    );
        }

        return Double.isFinite(result)
                ? result
                : 0.0;
    }

    public double maximumTemperatureCelsius() {
        double result =
                Double.NEGATIVE_INFINITY;

        for (float value : temperatureCelsius) {
            result =
                    Math.max(
                            result,
                            value
                    );
        }

        return Double.isFinite(result)
                ? result
                : 0.0;
    }

    public double maximumTransportedMoisture() {
        double result = Double.NEGATIVE_INFINITY;

        for (float value : transportedMoisture) {
            result = Math.max(result, value);
        }

        return Double.isFinite(result)
                ? result
                : 0.0;
    }

    private int index(
            int x,
            int z
    ) {
        return z * resolution + x;
    }
}
