package se.pjodd.glada;


/**
 * @author kalle
 * @since 2012-01-21 14:22
 */
public class Grid {
    private double latitudeStep;
    private double longitudeStep;

    private long height;
    private long width;

    public Grid(double cellWidthKilometers) {

        this.width = (long) (6378.1d / cellWidthKilometers);
        this.height = width;

        init();
    }


    public Grid(long width, long height) {
        this.height = height;
        this.width = width;
        init();
    }

    public void init() {
        latitudeStep = 180d / (double) height;
        longitudeStep = 360d / (double) width;
    }

    public class Cell {
        private long top;
        private long left;

        public Cell(long top, long left) {
            this.top = top;
            this.left = left;
        }

        private Coordinate getNorthwest() {
            return new Coordinate(90d - (top * latitudeStep), -180d + (left * longitudeStep));
        }

        public Envelope getEnvelope() {
            Coordinate southwest = getSouthwest();
            Coordinate northeast = getNortheast();
            return new Envelope(southwest, northeast);
        }

        public Coordinate getNortheast() {
            Coordinate northWest = getNorthwest();
            return new Coordinate(getNorthwest().getLatitude(), northWest.getLongitude() + longitudeStep);
        }

        public Coordinate getSouthwest() {
            Coordinate northWest = getNorthwest();
            return new Coordinate(getNorthwest().getLatitude() - latitudeStep, northWest.getLongitude());
        }

        public long getIdentity() {
            return (top * width) + left;
        }

        public long getTop() {
            return top;
        }

        public long getLeft() {
            return left;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Cell cell = (Cell) o;

            if (left != cell.left) return false;
            if (top != cell.top) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) (top ^ (top >>> 32));
            result = 31 * result + (int) (left ^ (left >>> 32));
            return result;
        }
    }

    public Cell getCell(long id) {
        long top = (id / this.width);
        long left = (id - (top * this.height));
        return new Cell(top, left);
    }

    public Cell getCell(double latitude, double longitude) {
        long top = (long) ((90d - latitude) / latitudeStep);
        long left = (long) ((longitude - -180d) / longitudeStep);
        return new Cell(top, left);
    }

    public Cell getCell(Coordinate coordinate) {
        return getCell(coordinate.getLatitude(), coordinate.getLongitude());
    }


    public long getHeight() {
        return height;
    }

    public long getWidth() {
        return width;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Grid grid = (Grid) o;

        if (height != grid.height) return false;
        if (Double.compare(grid.latitudeStep, latitudeStep) != 0) return false;
        if (Double.compare(grid.longitudeStep, longitudeStep) != 0) return false;
        if (width != grid.width) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = latitudeStep != +0.0d ? Double.doubleToLongBits(latitudeStep) : 0L;
        result = (int) (temp ^ (temp >>> 32));
        temp = longitudeStep != +0.0d ? Double.doubleToLongBits(longitudeStep) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (int) (height ^ (height >>> 32));
        result = 31 * result + (int) (width ^ (width >>> 32));
        return result;
    }

    public static class Coordinate {
        private double latitude;
        private double longitude;

        public Coordinate(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Coordinate that = (Coordinate) o;

            if (Double.compare(that.latitude, latitude) != 0) return false;
            return Double.compare(that.longitude, longitude) == 0;

        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            temp = Double.doubleToLongBits(latitude);
            result = (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(longitude);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }
    }

    public static class Envelope {
        private Coordinate southwest;
        private Coordinate northeast;

        private Envelope() {
        }

        private Envelope(Coordinate southwest, Coordinate northeast) {
            this.southwest = southwest;
            this.northeast = northeast;
        }

        public Coordinate getSouthwest() {
            return southwest;
        }

        private void setSouthwest(Coordinate southwest) {
            this.southwest = southwest;
        }

        public Coordinate getNortheast() {
            return northeast;
        }

        private void setNortheast(Coordinate northeast) {
            this.northeast = northeast;
        }

    }


}
