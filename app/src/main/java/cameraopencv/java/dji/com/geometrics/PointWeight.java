package cameraopencv.java.dji.com.geometrics;

public class PointWeight {
    public double x =0;
    public double y=0;
    public double weight=0;

    public PointWeight() {}
    public PointWeight(double x, double y, double weight) {
        this.x =x;
        this.y =y;
        this.weight=weight;
    }
    public PointWeight(double x, double y) {
        this.x =x;
        this.y =y;
        this.weight=1;
    }
}
