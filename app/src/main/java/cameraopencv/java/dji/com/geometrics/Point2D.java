package cameraopencv.java.dji.com.geometrics;

public class Point2D {

    public Point2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double x;
    public double y;

    @Override
    public String toString() {
        return String.format("(%f,%f)", x, y);
    }
}
