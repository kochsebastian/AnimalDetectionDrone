package cameraopencv.java.dji.com.geometrics;

public class Rect2D  {
    private double x;
    private double y;
    private double width;
    private double height;

    public Rect2D(double x, double y,double width, double height){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public double centerX(){
        return (x+(x+width))/2;
    }

    public double centerY(){
        return (y+(y+height))/2;
    }
}
