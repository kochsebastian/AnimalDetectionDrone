package cameraopencv.java.dji.com;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import cameraopencv.java.dji.com.utils.GeneralUtils;
import cameraopencv.java.dji.com.utils.ToastUtils;
import com.google.android.gms.maps.model.LatLng;
import dji.common.error.DJIError;
import dji.common.mission.waypoint.*;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.Triggerable;
import dji.sdk.mission.timeline.TimelineElement;
import dji.sdk.mission.timeline.TimelineEvent;
import dji.sdk.mission.timeline.TimelineMission;
import dji.sdk.mission.timeline.actions.GoHomeAction;
import dji.sdk.mission.timeline.actions.TakeOffAction;
import dji.sdk.mission.timeline.triggers.*;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.products.Aircraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for Timeline MissionControl.
 */
public class TimelineFlight {

    private MissionControl missionControl;
    private FlightController flightController;
    private TimelineEvent preEvent;
    private TimelineElement preElement;
    private DJIError preError;


    protected TextView timelineInfoTV;
    protected TextView runningInfoTV;
    protected ProgressBar progressBar;

    protected double homeLatitude = 181;
    protected double homeLongitude = 181;

    private Activity context_;
    private TextView text;
    private TextView title;

    private List<LatLng> myFlightWaypoints;
    private int waypointIndex = 0;
    private boolean startedMission = false;

    private int countedReached = 0;
    private int countLanded = 0;
    private boolean once = false;

    public TimelineFlight(Activity context, TextView title, TextView text) {
        context_ = context;
        this.text = text;
        this.title = title;

    }

    private void setRunningResultToText(final String s) {
        if (runningInfoTV == null) {
            context_.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //      Toast.makeText(context_, "RR: "+s, Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            runningInfoTV.append(s + "\n");

        }

    }

    private void setTimelinePlanToText(final String s) {
        if (timelineInfoTV == null) {
            context_.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //   Toast.makeText(context_, "TP: "+s, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            timelineInfoTV.append(s + "\n");
        }
    }

    /**
     * Demo on BatteryPowerLevelTrigger.  Once the batter remaining power is equal or less than the value,
     * the trigger's action will be called.
     *
     * @param triggerTarget which can be any action object or timeline object.
     */
    private void addBatteryPowerLevelTrigger(Triggerable triggerTarget) {
        float value = 10f;
        BatteryPowerLevelTrigger trigger = new BatteryPowerLevelTrigger();
        trigger.setPowerPercentageTriggerValue(value);
        addTrigger(trigger, triggerTarget, " at level " + value);
    }

    /**
     * Demo on WaypointReachedTrigger.  Once the expected waypoint is reached in the waypoint mission execution process,
     * this trigger's action will be called. If user has some special things to do for this waypoint, the code can be put
     * in this trigger action method.
     *
     * @param triggerTarget
     */
    private void addWaypointReachedTrigger(Triggerable triggerTarget) {
        int value = 1;
        WaypointReachedTrigger trigger = new WaypointReachedTrigger();
        trigger.setWaypointIndex(value);
        addTrigger(trigger, triggerTarget, " at index " + value);
        startedMission = true;
    }

    /**
     * Demo on AircraftLandedTrigger. Once the aircraft is landed, this trigger action will be called if the timeline is
     * not finished yet.
     *
     * @param triggerTarget
     */

    private void addAircraftLandedTrigger(Triggerable triggerTarget) {
        AircraftLandedTrigger trigger = new AircraftLandedTrigger();

        addTrigger(trigger, triggerTarget, "");
    }

    private Trigger.Listener triggerListener = new Trigger.Listener() {
        @Override
        public void onEvent(Trigger trigger, TriggerEvent event, @Nullable DJIError error) {
            setRunningResultToText("Trigger " + trigger.getClass().getSimpleName() + " event is " + event.name() + (error == null ? " " : error.getDescription()));

            if (trigger.getClass().equals(new AircraftLandedTrigger().getClass())) {


//                missionControl = MissionControl.getInstance();
//                missionControl.unscheduleEverything();
//                missionControl.removeAllListeners();
//                waypointIndex=0;
            }
            if (trigger.getClass().equals(new WaypointReachedTrigger().getClass())) {
                if (countedReached == 1) {
                    Intent intent = new Intent(context_, FlightActivity.class);
                    context_.startActivity(intent);
                }
//                if(countedReached == myFlightWaypoints.size()){ //isnt working
//                    Intent intent = new Intent(context_, MapActivity.class);
//                    context_.startActivity(intent);
//                }
                countedReached++;
                context_.runOnUiThread(new Runnable() {
                                           @Override
                                           public void run() {
                                               //    ToastUtils.showToast("Reached");
                                           }
                                       }
                );

            }
        }
    };

    private void initTrigger(final Trigger trigger) {
        trigger.addListener(triggerListener);
        trigger.setAction(new Trigger.Action() {
            @Override
            public void onCall() {
                setRunningResultToText("Trigger " + trigger.getClass().getSimpleName() + " Action method onCall() is invoked");
            }
        });
    }

    private void addTrigger(Trigger trigger, Triggerable triggerTarget, String additionalComment) {

        if (triggerTarget != null) {

            initTrigger(trigger);
            List<Trigger> triggers = triggerTarget.getTriggers();
            if (triggers == null) {
                triggers = new ArrayList<>();
            }

            triggers.add(trigger);
            triggerTarget.setTriggers(triggers);

            setTimelinePlanToText(triggerTarget.getClass().getSimpleName()
                    + " Trigger "
                    + triggerTarget.getTriggers().size()
                    + ") "
                    + trigger.getClass().getSimpleName()
                    + additionalComment);
        }
    }

    private void uploadWaypoints(List<TimelineElement> elements) {
        int waypointsNext;
        if (myFlightWaypoints.size() - waypointIndex == 3) {
            waypointsNext = 3;
        } else {
            waypointsNext = Math.min(2, myFlightWaypoints.size() - waypointIndex);
            if (waypointsNext == 0) {
                Intent intent = new Intent(context_, MapActivity.class);
                context_.startActivity(intent);
                elements.add(new GoHomeAction());
                return;
            }
        }
        List<LatLng> missionWaypoints = myFlightWaypoints.subList(waypointIndex, waypointIndex + waypointsNext);
        TimelineElement waypointMission = TimelineMission.elementFromWaypointMission(initTestingWaypointMission(missionWaypoints));
        waypointIndex += waypointsNext;

        elements.add(waypointMission);
        addWaypointReachedTrigger(waypointMission);

    }

    private void initTimeline() {
        if (!GeneralUtils.checkGpsCoordinate(homeLatitude, homeLongitude)) {
            ToastUtils.setResultToToast("No home point!!!");
            return;
        }
        missionControl = MissionControl.getInstance();
        MissionControl.Listener listener = new MissionControl.Listener() {
            @Override
            public void onEvent(@Nullable TimelineElement element, TimelineEvent event, DJIError error) {
                updateTimelineStatus(element, event, error);
            }
        };
        //   waypointIndex=0;

        List<TimelineElement> elements = new ArrayList<>();

        setTimelinePlanToText("Step 1: takeoff from the ground");
        elements.add(new TakeOffAction());

        this.uploadWaypoints(elements);

        if (missionControl.scheduledCount() > 0) {
            missionControl.removeAllListeners();
            missionControl.unscheduleEverything();
        }

        missionControl.scheduleElements(elements);
        addAircraftLandedTrigger(missionControl);
        addBatteryPowerLevelTrigger(missionControl);

        missionControl.addListener(listener);
    }

    private void updateTimelineStatus(@Nullable TimelineElement element, TimelineEvent event, DJIError error) {

        if (element == preElement && event == preEvent && error == preError) {
            return;
        }


        if (!missionControl.isTimelineRunning() && startedMission) {

            List<TimelineElement> elements = new ArrayList<>();
            this.uploadWaypoints(elements);
            if (missionControl.scheduledCount() > 0) {
                missionControl.unscheduleEverything();
            }

            missionControl.scheduleElements(elements);
            missionControl.startTimeline();

            return;

        }

//        context_.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                text.setText("Running: " + missionControl.isTimelineRunning() + "  Running Element: " + missionControl.getRunningElement()
//                        +"     Time" + System.currentTimeMillis());
//                title.setText("started: "+ startedMission + "      MissionIndex:" + missionControl.getCurrentTimelineMarker());
//            }}
//            );

        if (error != null) {
            if (error.getClass().equals(DJIError.COMMON_TIMEOUT)) {
                ToastUtils.showToast("found way");
            }
        }
        if (element != null) {
//            context_.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    title.setText("  Valid: " + missionControl.getRunningElement().checkValidity().getDescription() +"     Time" + System.currentTimeMillis());
//                }});
            if (element instanceof TimelineMission) {
                setRunningResultToText(((TimelineMission) element).getMissionObject().getClass().getSimpleName()
                        + " event is "
                        + event.toString()
                        + " "
                        + (error == null ? "" : error.getDescription()));
            } else {
                setRunningResultToText(element.getClass().getSimpleName()
                        + " event is "
                        + event.toString()
                        + " "
                        + (error == null ? "" : error.getDescription()));
            }
        } else {
            setRunningResultToText("Timeline Event is " + event.toString() + " " + (error == null
                    ? ""
                    : "Failed:"
                    + error.getDescription()));
        }

        preEvent = event;
        preElement = element;
        preError = error;
    }

    private WaypointMission initTestingWaypointMission(List<LatLng> coords) {
        if (!GeneralUtils.checkGpsCoordinate(homeLatitude, homeLongitude)) {
            ToastUtils.setResultToToast("No home point!!!");
            return null;
        }

        WaypointMission.Builder waypointMissionBuilder = new WaypointMission.Builder().autoFlightSpeed(10f)
                .maxFlightSpeed(10f)
                .setExitMissionOnRCSignalLostEnabled(false)
                .finishedAction(
                        WaypointMissionFinishedAction.NO_ACTION)
                .flightPathMode(
                        WaypointMissionFlightPathMode.NORMAL)
                .gotoFirstWaypointMode(
                        WaypointMissionGotoWaypointMode.SAFELY)
                .headingMode(
                        WaypointMissionHeadingMode.AUTO)
                .repeatTimes(1);

        List<Waypoint> waypoints = new ArrayList<>();
        for (LatLng wayPoint : coords) {
            waypoints.add(new Waypoint(wayPoint.latitude, wayPoint.longitude, 40));
        }
        // waypointMissionBuilder.calculateTotalDistance();
        //waypointMissionBuilder.getLastCalculatedTotalTime();
        waypointMissionBuilder.waypointList(waypoints).waypointCount(waypoints.size());
        return waypointMissionBuilder.build();
    }


    private void startTimeline() {
        if (MissionControl.getInstance().scheduledCount() > 0) {
            MissionControl.getInstance().startTimeline();
        } else {
            ToastUtils.setResultToToast("Init the timeline first by clicking the Init button");
        }
    }

    public void stopTimeline() {
        context_.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtils.showToast("STOPPPP");
            }
        });
        missionControl = MissionControl.getInstance();
        MissionControl.getInstance().stopTimeline();
        missionControl.unscheduleEverything();
        missionControl.removeAllListeners();
    }

    public void gotoHome() {
//        missionControl = MissionControl.getInstance();
//        cleanTimelineDataAndLog();
        Intent intent = new Intent(context_, MapActivity.class);
        context_.startActivity(intent);
        missionControl = MissionControl.getInstance();
        missionControl.unscheduleEverything();
        ;
        missionControl.removeAllListeners();
        missionControl.scheduleElement(new GoHomeAction());
        missionControl.startTimeline();
    }

    private void pauseTimeline() {
        MissionControl.getInstance().pauseTimeline();
    }


    private void resumeTimeline() {
        MissionControl.getInstance().resumeTimeline();
    }

    private void cleanTimelineDataAndLog() {
        missionControl = MissionControl.getInstance();
        if (missionControl.scheduledCount() > 0) {
            missionControl.unscheduleEverything();
            missionControl.removeAllListeners();
        }
        runningInfoTV.setText("");
        timelineInfoTV.setText("");
    }


    public void runTimeLine(final List<LatLng> flightWaypoints) {

        BaseProduct product = FPVDemoApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            ToastUtils.setResultToToast("Disconnect");
            missionControl = null;
            return;
        } else {
            missionControl = MissionControl.getInstance();
            if (product instanceof Aircraft) {
                flightController = ((Aircraft) product).getFlightController();
            }
        }
        if (FPVDemoApplication.getProductInstance() instanceof Aircraft && !GeneralUtils.checkGpsCoordinate(
                homeLatitude,
                homeLongitude) && flightController != null) {
            flightController.getHomeLocation(new CommonCallbacks.CompletionCallbackWith<LocationCoordinate2D>() {
                @Override
                public void onSuccess(LocationCoordinate2D locationCoordinate2D) {
                    homeLatitude = locationCoordinate2D.getLatitude();
                    homeLongitude = locationCoordinate2D.getLongitude();
                    if (GeneralUtils.checkGpsCoordinate(homeLatitude, homeLongitude)) {
                        myFlightWaypoints = flightWaypoints;
                        setTimelinePlanToText("home point latitude: " + homeLatitude + "\nhome point longitude: " + homeLongitude);
                        waypointIndex = 0;
                        //   startedMission = false;
                        initTimeline();
                        startTimeline();
                    } else {
                        ToastUtils.setResultToToast("Failed to get home coordinates: Invalid GPS coordinate");
                    }
                }

                @Override
                public void onFailure(DJIError djiError) {
                    ToastUtils.setResultToToast("Failed to get home coordinates: " + djiError.getDescription());
                }
            });
        }
        return;
    }


}