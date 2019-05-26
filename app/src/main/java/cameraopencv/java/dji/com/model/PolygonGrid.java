package cameraopencv.java.dji.com.model;


import cameraopencv.java.dji.com.geometrics.Polygon;
import cameraopencv.java.dji.com.geometrics.Point2D;
import cameraopencv.java.dji.com.geometrics.Rect2D;
import cameraopencv.java.dji.com.utils.GeneralUtils;
import com.google.android.gms.maps.model.LatLng;
import dji.common.model.LocationCoordinate2D;
import org.opencv.android.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static cameraopencv.java.dji.com.utils.GeneralUtils.ONE_METER_OFFSET;
import static cameraopencv.java.dji.com.utils.GeneralUtils.calcLongitudeOffset;


public class PolygonGrid {
    final  double THRESHOLD = .000000000001;
    private List<Rect2D> myShapesVert = new ArrayList<Rect2D>();
    private List<Point2D> myCentersVert = new ArrayList<Point2D>();
    private List<Rect2D> myShapesHori = new ArrayList<Rect2D>();
    private List<Point2D> myCentersHori = new ArrayList<Point2D>();

    private List<Rect2D> myShapes = new ArrayList<Rect2D>();
    private List<Point2D> myCenters = new ArrayList<Point2D>();
    private double imageWidth = 10;
    private  double imageHeight = 5;
    private Polygon p;

    private double oneMeter_x = ONE_METER_OFFSET;
    private double oneMeter_y = ONE_METER_OFFSET;
    private static double overlap = (1 - 0.3);


    private void calculateGridSize (double altitude,double latitude){


        // calculating by using trigonometry
        // 2*tan(theta) = d/h
        double fov_angle_y = 27; // degree
        double fov_angle_x = 35; // degree

        double droneAltitude = altitude;
        oneMeter_x =calcLongitudeOffset(latitude);
        imageWidth = 2* Math.tan(Math.toRadians(fov_angle_x/2)) * droneAltitude *  calcLongitudeOffset(latitude);
        imageHeight = 2* Math.tan(Math.toRadians(fov_angle_y/2)) * droneAltitude * ONE_METER_OFFSET;
    }

    public boolean isContainedInField(LatLng point) {
        Point2D p2D = new Point2D(point.latitude,point.longitude);
        return p.contains(p2D);
    }

    public List<Point2D> makeGrid(double altitude, List<LatLng> vertices) {

        myShapes.clear();
        myCenters.clear();
        myCentersHori.clear();
        myCentersVert.clear();
        myShapesHori.clear();
        myShapesVert.clear();

        if(!(vertices.size()>=3))
            throw new IllegalArgumentException();
        calculateGridSize(altitude,vertices.get(0).latitude);

        List<Point2D> vertexPoints = new ArrayList<>();
        for(LatLng v : vertices){
            vertexPoints.add(new Point2D(v.latitude,v.longitude));
        }
        p = null;
        p = Polygon.Builder().addVertices(vertexPoints).build();

        makeGridItVert(p.getMinX(),p.getMinY(),p);
        makeGridItHori(p.getMinX(),p.getMinY(),p);
        List<Rect2D> copyHorizontal = new ArrayList<Rect2D>(myShapesHori);
        List<Rect2D> copyVertical = new ArrayList<Rect2D>(myShapesVert);

        copyHorizontal = hasTwoNeighboursHorizontal(copyHorizontal,myCentersHori);
        copyVertical = hasTwoNeighboursVertical(copyVertical,myCentersVert);

        if(copyHorizontal.size() <= copyVertical.size()) {
            myShapes = copyHorizontal;
            myCenters = myCentersHori;
        }else {
            myShapes= copyVertical;
            myCenters = myCentersVert;
        }/*
       if(myShapesHori.size() < myShapesVert.size()){
           myShapes = myShapesHori;
       }else
           myShapes = myShapesVert;
*/
        if(myShapes.size() < 2)
            throw new NullPointerException();
        List<Point2D> waypoints = new ArrayList<>();
        for(Rect2D wayPoint : myShapes){
            waypoints.add(new Point2D(wayPoint.centerX(),wayPoint.centerY()));
        }
        //System.out.println(myShapes.size()+ "     "+ copyHorizontal.size() + "     " + copyVertical.size());
        return waypoints;
    }
    private int makeGridItVert(double x,double y,Polygon p) {
        int i=0;
        int offset = 0;
        double dir = 1;

        while(x <= p.getMaxX()) {
            dir = 1;
            while(y<=p.getMaxY()) {
                if(p.contains(new Point2D(x,y))
                        && p.contains(new Point2D(x+imageWidth,y+imageHeight))
                        && p.contains(new Point2D(x+imageWidth,y))
                        && p.contains(new Point2D(x,y+imageHeight)))
                {
                    Rect2D r = new Rect2D(x,y,imageWidth,imageHeight);
                    Point2D center = new Point2D(r.centerX(), r.centerY());

                    myShapesVert.add(r);
                    myCentersVert.add(center);


                }
                y+=oneMeter_y;
            }
            x+=imageWidth*overlap;
            y=p.getMaxY();
            dir = -1;
            while(y >= p.getMinY()) {

                if(p.contains(new Point2D(x,y))
                        && p.contains(new Point2D(x+imageWidth,y+imageHeight))
                        && p.contains(new Point2D(x+imageWidth,y))
                        && p.contains(new Point2D(x,y+imageHeight)))
                {
                    Rect2D r = new Rect2D(x,y,imageWidth,imageHeight);

                    Point2D center = new Point2D(r.centerX(), r.centerY());

                    myShapesVert.add(r);
                    myCentersVert.add(center);

                }
                y-=oneMeter_y;


            }
            y=p.getMinY();
            x+=imageWidth*overlap;

        }
        x=p.getMaxX()-imageWidth-oneMeter_x;
        if(dir == 1) {
            while(y >= p.getMinY()) {
                if(p.contains(new Point2D(x,y))
                        && p.contains(new Point2D(x+imageWidth,y+imageHeight))
                        && p.contains(new Point2D(x+imageWidth,y))
                        && p.contains(new Point2D(x,y+imageHeight)))
                {
                    Rect2D r = new Rect2D(x,y,imageWidth,imageHeight);

                    Point2D center = new Point2D(r.centerX(), r.centerY());

                    myShapesVert.add(r);
                    myCentersVert.add(center);


                }
                y-=oneMeter_y;
            }
        }else {
            while(y<=p.getMaxY()) {
                if(p.contains(new Point2D(x,y))
                        && p.contains(new Point2D(x+imageWidth,y+imageHeight))
                        && p.contains(new Point2D(x+imageWidth,y))
                        && p.contains(new Point2D(x,y+imageHeight)))
                {
                    Rect2D r = new Rect2D(x,y,imageWidth,imageHeight);
                    Point2D center = new Point2D(r.centerX(), r.centerY());

                    myShapesVert.add(r);
                    myCentersVert.add(center);


                }
                y+=oneMeter_y;
            }
        }

        return i;
    }

    private int makeGridItHori(double x,double y,Polygon p) {
        int i=0;
        int offset = 0;
        double dir = 1;
        double copySize = imageHeight;
        imageHeight = imageWidth;
        imageWidth = copySize;
        while(y <= p.getMaxY()) {
            dir=1;
            while(x<=p.getMaxX()) {
                if(p.contains(new Point2D(x,y))
                        && p.contains(new Point2D(x+imageWidth,y+imageHeight))
                        && p.contains(new Point2D(x+imageWidth,y))
                        && p.contains(new Point2D(x,y+imageHeight)))
                {
                    Rect2D r = new Rect2D(x,y,imageWidth,imageHeight);
                    Point2D center = new Point2D(r.centerX(), r.centerY());

                    myShapesHori.add(r);
                    offset ++;
                    i++;
                    myCentersHori.add(center);


                }

                x+=oneMeter_x*overlap;
            }
            y+=imageHeight;
            x=p.getMaxX();
            dir=-1;
            while(x>=p.getMinX()) {

                if(p.contains(new Point2D(x,y))
                        && p.contains(new Point2D(x+imageWidth,y+imageHeight))
                        && p.contains(new Point2D(x+imageWidth,y))
                        && p.contains(new Point2D(x,y+imageHeight)))
                {
                    Rect2D r = new Rect2D(x,y,imageWidth,imageHeight);

                    Point2D center = new Point2D(r.centerX(), r.centerY());

                    myShapesHori.add(r);
                    myCentersHori.add(center);

                }
                x-=oneMeter_x;
            }
            x=p.getMinX();


            y+=imageHeight*overlap;

        }
        y=p.getMaxY()-imageHeight-oneMeter_y;
        if(dir == 1) {
            while(x >= p.getMinX()) {
                if(p.contains(new Point2D(x,y))
                        && p.contains(new Point2D(x+imageWidth,y+imageHeight))
                        && p.contains(new Point2D(x+imageWidth,y))
                        && p.contains(new Point2D(x,y+imageHeight)))
                {
                    Rect2D r = new Rect2D(x,y,imageWidth,imageHeight);

                    Point2D center = new Point2D(r.centerX(), r.centerY());

                    myShapesHori.add(r);
                    myCentersHori.add(center);


                }
                x-=oneMeter_x;
            }
        }else {
            while(x<=p.getMaxX()) {
                if(p.contains(new Point2D(x,y))
                        && p.contains(new Point2D(x+imageWidth,y+imageHeight))
                        && p.contains(new Point2D(x+imageWidth,y))
                        && p.contains(new Point2D(x,y+imageHeight)))
                {
                    Rect2D r = new Rect2D(x,y,imageWidth,imageHeight);
                    Point2D center = new Point2D(r.centerX(), r.centerY());

                    myShapesHori.add(r);
                    myCentersHori.add(center);


                }
                x+=oneMeter_x;
            }
        }
        imageWidth = imageHeight;
        imageHeight = copySize;
        return i;
    }




    private  List<Rect2D> hasTwoNeighboursVertical(List<Rect2D> myShapes,List<Point2D> myCenters) {
        int delete = 0;
        for(int i = 0; i< myCenters.size();i++) {
            if (i == 0 || i == myCenters.size() - 1)
                continue;
            if (((Math.abs((myCenters.get(i).y - oneMeter_y) - myCenters.get(i - 1).y) < THRESHOLD)
                    && Math.abs((myCenters.get(i).x) - myCenters.get(i - 1).x) < THRESHOLD)
                    && ((Math.abs((myCenters.get(i).y + oneMeter_y) - myCenters.get(i + 1).y) < THRESHOLD)
                    && Math.abs((myCenters.get(i).x) - myCenters.get(i + 1).x) < THRESHOLD)) {
//                if(delete < 5){
//                    delete++;
                myShapes.set(i, null);
//                }else{
//                    delete = 0;
//                }
            }
            if (((Math.abs((myCenters.get(i).y + oneMeter_y) - myCenters.get(i - 1).y) < THRESHOLD)
                    && Math.abs((myCenters.get(i).x) - myCenters.get(i - 1).x) < THRESHOLD)
                    && ((Math.abs((myCenters.get(i).y - oneMeter_y) - myCenters.get(i + 1).y) < THRESHOLD)
                    && Math.abs((myCenters.get(i).x) - myCenters.get(i + 1).x) < THRESHOLD)) {
//                if(delete < 5){
//                    delete++;
                myShapes.set(i, null);
//                }else{
//                    delete = 0;
//                }
            }
        }
        myShapes.removeAll(Collections.singletonList(null));

        return myShapes;

    }
    private  List<Rect2D> hasTwoNeighboursHorizontal(List<Rect2D> myShapes,List<Point2D> myCenters) {
        double copySize = imageHeight;
        imageHeight = imageWidth;
        imageWidth = copySize;
        int delete = 0;
        for(int i = 0; i< myCenters.size();i++) {
            if(i == 0 || i == myCenters.size()-1)
                continue;
            if (((Math.abs((myCenters.get(i).x - oneMeter_x) - myCenters.get(i - 1).x) < THRESHOLD)
                    && Math.abs((myCenters.get(i).y) - myCenters.get(i - 1).y) < THRESHOLD)
                    && ((Math.abs((myCenters.get(i).x + oneMeter_x) - myCenters.get(i + 1).x) < THRESHOLD)
                    && Math.abs((myCenters.get(i).y) - myCenters.get(i + 1).y) < THRESHOLD)) {
//                if(delete < 5){
//                    delete++;
                myShapes.set(i, null);
//                }else{
//                    delete = 0;
//                }

            }
            if (((Math.abs((myCenters.get(i).x + oneMeter_x) - myCenters.get(i - 1).x) < THRESHOLD)
                    && Math.abs((myCenters.get(i).y) - myCenters.get(i - 1).y) < THRESHOLD)
                    && ((Math.abs((myCenters.get(i).x - oneMeter_x) - myCenters.get(i + 1).x) < THRESHOLD)
                    && Math.abs((myCenters.get(i).y) - myCenters.get(i + 1).y) < THRESHOLD)) {
//                if(delete < 5){
//                    delete++;
                myShapes.set(i, null);
//                }else{
//                    delete = 0;
//                }
            }



        }
        myShapes.removeAll(Collections.singletonList(null));
        imageWidth = imageHeight;
        imageHeight = copySize;
        return myShapes;

    }

    public double calcDistance(List<Point2D> waypoints ){
        double distance =0;
        for(int i = 1; i< waypoints.size()-1;i++){
            distance += distFrom(waypoints.get(i-1).x,waypoints.get(i-1).y,waypoints.get(i).x,waypoints.get(i).y);
        }
        return distance;

    }
    public double distFrom(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double dist = (double) (earthRadius * c);

        return dist;
    }


}