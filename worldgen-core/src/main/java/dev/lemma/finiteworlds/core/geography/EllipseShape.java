package dev.lemma.finiteworlds.core.geography;

public final class EllipseShape
        implements GeoShape {

    private final double centerX;
    private final double centerZ;

    private final double radiusX;
    private final double radiusZ;

    private final double rotation;

    private final double cosRotation;
    private final double sinRotation;

    public EllipseShape(
            double centerX,
            double centerZ,
            double radiusX,
            double radiusZ,
            double rotation
    ) {
        this.centerX =
                centerX;

        this.centerZ =
                centerZ;

        this.radiusX =
                radiusX;

        this.radiusZ =
                radiusZ;

        this.rotation =
                rotation;

        this.cosRotation =
                Math.cos(rotation);

        this.sinRotation =
                Math.sin(rotation);
    }

    @Override
    public double sample(
            double x,
            double z
    ) {

        double dx =
                x - centerX;

        double dz =
                z - centerZ;

        /*
         * Rotate into ellipse-local coordinates.
         */

        double localX =
                cosRotation * dx
                        + sinRotation * dz;

        double localZ =
                -sinRotation * dx
                        + cosRotation * dz;

        double normalizedX =
                localX / radiusX;

        double normalizedZ =
                localZ / radiusZ;

        double distance =
                Math.sqrt(
                        normalizedX
                                * normalizedX
                                + normalizedZ
                                * normalizedZ
                );

        return 1.0 - distance;
    }

    public double centerX() {
        return centerX;
    }

    public double centerZ() {
        return centerZ;
    }

    public double radiusX() {
        return radiusX;
    }

    public double radiusZ() {
        return radiusZ;
    }

    public double rotation() {
        return rotation;
    }
}