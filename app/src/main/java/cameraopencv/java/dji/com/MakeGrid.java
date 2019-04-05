package cameraopencv.java.dji.com;


import cameraopencv.java.dji.com.geometrics.Polygon;
import cameraopencv.java.dji.com.geometrics.Point2D;
import cameraopencv.java.dji.com.geometrics.Rect2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MakeGrid {
    static List<Rect2D> myShapesVert = new ArrayList<Rect2D>();
    static List<Point2D> myCentersVert = new ArrayList<Point2D>();
    static List<Rect2D> myShapesHori = new ArrayList<Rect2D>();
    static List<Point2D> myCentersHori = new ArrayList<Point2D>();

    static List<Rect2D> myShapes = new ArrayList<Rect2D>();
    static List<Point2D> myCenters = new ArrayList<Point2D>();
    private static double imageWidth = 10;
    private static double imageHeight = 5;

    private static void calculateFrameSize(double altitude){
        // calculating by using trigonometry
        // 2*tan(theta) = d/h
        double fov_angle_y = 27; // degree
        double fov_angle_x = 35; // degree

        double droneAltitude = altitude;
        imageWidth = 2* Math.tan(fov_angle_x/2) * droneAltitude;
        imageHeight = 2* Math.tan(fov_angle_y/2) * droneAltitude;

    }

    public static void main(String[] args) {
        calculateFrameSize(40);
        Polygon p = Polygon.Builder()
                .addVertex(new Point2D(10, 10))
                .addVertex(new Point2D(700, 20))
                .addVertex(new Point2D(1000, 400))
                .addVertex(new Point2D(700, 700))
                .addVertex(new Point2D(500, 600))
                .addVertex(new Point2D(500, 600))
                .addVertex(new Point2D(300, 700))
                .build();



        makeGridItVert(p.getMinX()-100,p.getMinY()-100,p);
        makeGridItHori(p.getMinX()-100,p.getMinY()-100,p);
        List<Rect2D> copyHorizontal = new ArrayList<Rect2D>(myShapesHori);
        List<Rect2D> copyVertical = new ArrayList<Rect2D>(myShapesVert);
        copyHorizontal = hasFourNeighboursHorizontal(copyHorizontal,myCentersHori);
        copyVertical = hasFourNeighboursVertical(copyVertical,myCentersVert);
        if(copyHorizontal.size() <= copyVertical.size()) {
            myShapes = copyHorizontal;
            myCenters = myCentersHori;
        }else {
            myShapes= copyVertical;
            myCenters = myCentersVert;
        }
        System.out.println(myShapes.size()+ "     "+ copyHorizontal.size() + "     " + copyVertical.size());

    }


    private static int makeGridItVert(double x, double y, Polygon p) {
        int i=0;

        while(x+imageWidth <= p.getMaxX()+100) {
            while(y+imageHeight<=p.getMaxY()+100) {
                if(p.contains(new Point2D(x,y))
                        && p.contains(new Point2D(x+imageWidth,y+imageHeight))
                        && p.contains(new Point2D(x+imageWidth,y))
                        && p.contains(new Point2D(x,y+imageHeight)))
                {
                    Rect2D r = new Rect2D(x,y,imageWidth,imageHeight);
                    Point2D center = new Point2D(r.centerX(), r.centerY());

                    myShapesVert.add(r);

                    i++;
                    myCentersVert.add(center);


                }
                y+=imageHeight;
            }

            x+=imageWidth;
            while(y-imageHeight>=p.getMinY()-100) {
                y-=imageHeight;
                if(p.contains(new Point2D(x,y))
                        && p.contains(new Point2D(x+imageWidth,y+imageHeight))
                        && p.contains(new Point2D(x+imageWidth,y))
                        && p.contains(new Point2D(x,y+imageHeight)))
                {
                    Rect2D r = new Rect2D(x,y,imageWidth,imageHeight);

                    Point2D center = new Point2D(r.centerX(), r.centerY());

                    myShapesVert.add(r);

                    i++;

                    myCentersVert.add(center);

                }
            }

            x+=imageWidth;

        }
        return i;
    }

    private static int makeGridItHori(double x,double y,Polygon p) {
        int i=0;

        while(y+imageHeight <= p.getMaxY()+100) {
            while(x+imageHeight<=p.getMaxX()+100) {
                if(p.contains(new Point2D(x,y))
                        && p.contains(new Point2D(x+imageWidth,y+imageHeight))
                        && p.contains(new Point2D(x+imageWidth,y))
                        && p.contains(new Point2D(x,y+imageHeight)))
                {
                    Rect2D r = new Rect2D(x,y,imageWidth,imageHeight);
                    Point2D center = new Point2D(r.centerX(), r.centerY());

                    myShapesHori.add(r);

                    i++;
                    myCentersHori.add(center);


                }
                x+=imageWidth;
            }
            //	hasFourNeighbours(lastRow,lastCenters,offset);

            y+=imageHeight;
            while(x-imageWidth>=p.getMinX()-100) {
                x-=imageWidth;
                if(p.contains(new Point2D(x,y))
                        && p.contains(new Point2D(x+imageWidth,y+imageHeight))
                        && p.contains(new Point2D(x+imageWidth,y))
                        && p.contains(new Point2D(x,y+imageHeight)))
                {
                    Rect2D r = new Rect2D(x,y,imageWidth,imageHeight);

                    Point2D center = new Point2D(r.centerX(), r.centerY());

                    myShapesHori.add(r);

                    i++;

                    myCentersHori.add(center);

                }
            }
            //	hasFourNeighbours(lastRow,lastCenters,offset);

            y+=imageHeight;

        }
        return i;
    }


    private static List<Rect2D> hasFourNeighboursVertical(List<Rect2D> myShapes,List<Point2D> myCenters) {

        for(int i = 0; i< myCenters.size();i++) {
            if (i == 0 || i == myCenters.size() - 1)
                continue;
            if ((myCenters.get(i).y - imageHeight == myCenters.get(i - 1).y
                    && myCenters.get(i).x == myCenters.get(i - 1).x)
                    && (myCenters.get(i).y + imageHeight == myCenters.get(i + 1).y
                    && myCenters.get(i).x == myCenters.get(i + 1).x)) {
                myShapes.set(i, null);
            }
            if ((myCenters.get(i).y + imageHeight == myCenters.get(i - 1).y
                    && myCenters.get(i).x == myCenters.get(i - 1).x)
                    && (myCenters.get(i).y - imageHeight == myCenters.get(i + 1).y
                    && myCenters.get(i).x == myCenters.get(i + 1).x)) {
                myShapes.set(i, null);
            }
        }
        myShapes.removeAll(Collections.singletonList(null));

        return myShapes;

    }
    private static List<Rect2D> hasFourNeighboursHorizontal(List<Rect2D> myShapes,List<Point2D> myCenters) {
        for(int i = 0; i< myCenters.size();i++) {
            if(i == 0 || i == myCenters.size()-1)
                continue;
            if((myCenters.get(i).x - imageWidth == myCenters.get(i-1).x
                    && myCenters.get(i).y == myCenters.get(i-1).y)
                    && (myCenters.get(i).x + imageWidth == myCenters.get(i+1).x
                    && myCenters.get(i).y == myCenters.get(i+1).y))
            {
                myShapes.set(i, null);
            }
            if((myCenters.get(i).x + imageWidth == myCenters.get(i-1).x
                    && myCenters.get(i).y == myCenters.get(i-1).y)
                    && (myCenters.get(i).x - imageWidth == myCenters.get(i+1).x
                    && myCenters.get(i).y == myCenters.get(i+1).y))
            {
                myShapes.set(i, null);
            }



        }
        myShapes.removeAll(Collections.singletonList(null));

        return myShapes;

    }


}


