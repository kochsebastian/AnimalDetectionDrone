package cameraopencv.java.dji.com;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import cameraopencv.java.dji.com.utils.GeneralUtils;
import cameraopencv.java.dji.com.utils.ToastUtils;
import cameraopencv.java.dji.com.view.PresentableView;
import dji.common.error.DJIError;
import dji.common.gimbal.Attitude;
import dji.common.gimbal.Rotation;
import dji.common.mission.hotpoint.HotpointHeading;
import dji.common.mission.hotpoint.HotpointMission;
import dji.common.mission.hotpoint.HotpointStartPoint;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.Triggerable;
import dji.sdk.mission.timeline.TimelineElement;
import dji.sdk.mission.timeline.TimelineEvent;
import dji.sdk.mission.timeline.TimelineMission;
import dji.sdk.mission.timeline.actions.GimbalAttitudeAction;
import dji.sdk.mission.timeline.actions.GoHomeAction;
import dji.sdk.mission.timeline.actions.GoToAction;
import dji.sdk.mission.timeline.actions.HotpointAction;
import dji.sdk.mission.timeline.actions.RecordVideoAction;
import dji.sdk.mission.timeline.actions.ShootPhotoAction;
import dji.sdk.mission.timeline.actions.TakeOffAction;
import dji.sdk.mission.timeline.triggers.AircraftLandedTrigger;
import dji.sdk.mission.timeline.triggers.BatteryPowerLevelTrigger;
import dji.sdk.mission.timeline.triggers.Trigger;
import dji.sdk.mission.timeline.triggers.TriggerEvent;
import dji.sdk.mission.timeline.triggers.WaypointReachedTrigger;
import dji.sdk.products.Aircraft;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Class for Timeline MissionControl.
 */
public class TimelineFlightView extends Activity implements OnClickListener,PresentableView {

    private MissionControl missionControl;
    private FlightController flightController;
    private TimelineEvent preEvent;
    private TimelineElement preElement;
    private DJIError preError;

    protected Button getHomeBtn;
    protected Button prepareBtn;
    protected Button startBtn;
    protected Button stopBtn;
    protected Button pauseBtn;
    protected Button resumeBtn;
    protected Button cleanBtn;

    protected TextView timelineInfoTV;
    protected TextView runningInfoTV;
    protected ProgressBar progressBar;

    protected double homeLatitude = 181;
    protected double homeLongitude = 181;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_timeline);
        initUI();
    }

    private void setRunningResultToText(final String s) {
       /* post(new Runnable() {
            @Override
            public void run() {
                if (runningInfoTV == null) {
                //    Toast.makeText(this.getContext(), "textview = null", Toast.LENGTH_SHORT).show();
                } else {
                    runningInfoTV.append(s + "\n");
                }
            }
        });*/
    }

    private void setTimelinePlanToText(final String s) {

      /*  post(new Runnable() {
            @Override
            public void run() {
                if (timelineInfoTV == null) {
                  //  Toast.makeText(this.getContext(), "textview = null", Toast.LENGTH_SHORT).show();
                } else {
                    timelineInfoTV.append(s + "\n");
                }
            }
        });*/

    }
    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(TimelineFlightView.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Demo on BatteryPowerLevelTrigger.  Once the batter remaining power is equal or less than the value,
     * the trigger's action will be called.
     *
     * @param triggerTarget which can be any action object or timeline object.
     */
    private void addBatteryPowerLevelTrigger(Triggerable triggerTarget) {
        float value = 20f;
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
    }

    /**
     * Demo on AircraftLandedTrigger. Once the aircraft is landed, this trigger action will be called if the timeline is
     * not finished yet.
     * @param triggerTarget
     */
    private void addAircraftLandedTrigger(Triggerable triggerTarget) {
        AircraftLandedTrigger trigger = new AircraftLandedTrigger();
        addTrigger(trigger, triggerTarget, "");
    }

    private Trigger.Listener triggerListener = new Trigger.Listener() {
        @Override
        public void onEvent(Trigger trigger, TriggerEvent event, @Nullable DJIError error) {
            setRunningResultToText("Trigger " + trigger.getClass().getSimpleName() + " event is " + event.name() + (error==null? " ":error.getDescription()));
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

    private void initTimeline() {
        if (!GeneralUtils.checkGpsCoordinate(homeLatitude, homeLongitude)) {
            ToastUtils.setResultToToast("No home point!!!");
            return;
        }

        List<TimelineElement> elements = new ArrayList<>();

        missionControl = MissionControl.getInstance();
        final TimelineEvent preEvent = null;
        MissionControl.Listener listener = new MissionControl.Listener() {
            @Override
            public void onEvent(@Nullable TimelineElement element, TimelineEvent event, DJIError error) {
                updateTimelineStatus(element, event, error);
            }
        };

        //Step 1: takeoff from the ground
        setTimelinePlanToText("Step 1: takeoff from the ground");
        elements.add(new TakeOffAction());

        //Step 2: reset the gimbal to horizontal angle in 2 seconds.
        setTimelinePlanToText("Step 2: set the gimbal pitch -30 angle in 2 seconds");
        Attitude attitude = new Attitude(-30, Rotation.NO_ROTATION, Rotation.NO_ROTATION);
        GimbalAttitudeAction gimbalAction = new GimbalAttitudeAction(attitude);
        gimbalAction.setCompletionTime(2);
        elements.add(gimbalAction);

        //Step 3: Go 10 meters from home point
    //    setTimelinePlanToText("Step 3: Go 10 meters from home point");
      //  elements.add(new GoToAction(new LocationCoordinate2D(homeLatitude, homeLongitude), 10));


        //Step 3: Go 10 meters from home point
      //  setTimelinePlanToText("Step 3: Go 10 meters from home point");
       // elements.add(new GoToAction(new LocationCoordinate2D(homeLatitude, homeLongitude), 10));

        //Step 7: start a waypoint mission while the aircraft is still recording the video
        setTimelinePlanToText("Step 7: start a waypoint mission while the aircraft is still recording the video");
        TimelineElement waypointMission = TimelineMission.elementFromWaypointMission(initTestingWaypointMission());
        elements.add(waypointMission);
        addWaypointReachedTrigger(waypointMission);


        //Step 11: go back home
        setTimelinePlanToText("Step 11: go back home");
        elements.add(new GoHomeAction());



        addAircraftLandedTrigger(missionControl);
        addBatteryPowerLevelTrigger(missionControl);

        if (missionControl.scheduledCount() > 0) {
            missionControl.unscheduleEverything();
            missionControl.removeAllListeners();
        }

        missionControl.scheduleElements(elements);
        missionControl.addListener(listener);
    }

    private void updateTimelineStatus(@Nullable TimelineElement element, TimelineEvent event, DJIError error) {

        if (element == preElement && event == preEvent && error == preError) {
            return;
        }

        if (element != null) {
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

    private WaypointMission initTestingWaypointMission() {
        if (!GeneralUtils.checkGpsCoordinate(homeLatitude, homeLongitude)) {
            ToastUtils.setResultToToast("No home point!!!");
            return null;
        }

        WaypointMission.Builder waypointMissionBuilder = new WaypointMission.Builder().autoFlightSpeed(5f)
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
        List<Waypoint> waypoints = new LinkedList<>();

        Waypoint northPoint = new Waypoint(homeLatitude + 10 * GeneralUtils.ONE_METER_OFFSET, homeLongitude, 10f);
        Waypoint eastPoint =
                new Waypoint(homeLatitude, homeLongitude + 10 * GeneralUtils.calcLongitudeOffset(homeLatitude), 15f);
        Waypoint southPoint = new Waypoint(homeLatitude - 10 * GeneralUtils.ONE_METER_OFFSET, homeLongitude, 10f);
        Waypoint westPoint =
                new Waypoint(homeLatitude, homeLongitude - 10 * GeneralUtils.calcLongitudeOffset(homeLatitude), 15f);

        northPoint.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -60));
        southPoint.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, 60));

        waypoints.add(northPoint);
        waypoints.add(eastPoint);
        waypoints.add(southPoint);
        waypoints.add(westPoint);

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

    private void stopTimeline() {
        MissionControl.getInstance().stopTimeline();
    }

    private void pauseTimeline() {
        MissionControl.getInstance().pauseTimeline();
    }


    private void resumeTimeline() {
        MissionControl.getInstance().resumeTimeline();
    }

    private void cleanTimelineDataAndLog() {
        if (missionControl.scheduledCount() > 0) {
            missionControl.unscheduleEverything();
            missionControl.removeAllListeners();
        }
        runningInfoTV.setText("");
        timelineInfoTV.setText("");
    }



    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
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
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (missionControl != null && missionControl.scheduledCount() > 0) {
            missionControl.unscheduleEverything();
            missionControl.removeAllListeners();
        }
    }

    private void initUI() {
       // setClickable(true);
      //  LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        //layoutInflater.inflate(R.layout.view_timeline, this, true);

        timelineInfoTV = (TextView) findViewById(R.id.tv_timeline_info);
        runningInfoTV = (TextView) findViewById(R.id.tv_running_info);
        getHomeBtn = (Button) findViewById(R.id.btn_run);
        prepareBtn = (Button) findViewById(R.id.btn_timeline_init);
        startBtn = (Button) findViewById(R.id.btn_timeline_start);
        stopBtn = (Button) findViewById(R.id.btn_timeline_stop);
        pauseBtn = (Button) findViewById(R.id.btn_timeline_pause);
        resumeBtn = (Button) findViewById(R.id.btn_timeline_resume);
        cleanBtn = (Button) findViewById(R.id.btn_timeline_clean);

        getHomeBtn.setOnClickListener(this);
        prepareBtn.setOnClickListener(this);
        startBtn.setOnClickListener(this);
        stopBtn.setOnClickListener(this);
        pauseBtn.setOnClickListener(this);
        resumeBtn.setOnClickListener(this);
        cleanBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.btn_run) {
            if (FPVDemoApplication.getProductInstance() instanceof Aircraft && !GeneralUtils.checkGpsCoordinate(
                    homeLatitude,
                    homeLongitude) && flightController != null) {
                flightController.getHomeLocation(new CommonCallbacks.CompletionCallbackWith<LocationCoordinate2D>() {
                    @Override
                    public void onSuccess(LocationCoordinate2D locationCoordinate2D) {
                        homeLatitude = locationCoordinate2D.getLatitude();
                        homeLongitude = locationCoordinate2D.getLongitude();
                        if (GeneralUtils.checkGpsCoordinate(homeLatitude, homeLongitude)) {
                            setTimelinePlanToText("home point latitude: " + homeLatitude + "\nhome point longitude: " + homeLongitude);
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

        if (!GeneralUtils.checkGpsCoordinate(homeLatitude, homeLongitude)) {
            ToastUtils.setResultToToast("Home coordinates not yet set...");
            return;
        }

        switch (v.getId()) {
            case R.id.btn_timeline_init:
                initTimeline();
                break;
            case R.id.btn_timeline_start:
                startTimeline();
                break;
            case R.id.btn_timeline_stop:
                stopTimeline();
                break;
            case R.id.btn_timeline_pause:
                ToastUtils.setResultToToast("Timeline just supports the pause on the pausable elements, such as hotpoint mission, waypoint mission");
                pauseTimeline();
                break;
            case R.id.btn_timeline_resume:
                ToastUtils.setResultToToast("Timeline just supports the resume on the pausable elements, such as hotpoint mission, waypoint mission");
                resumeTimeline();
                break;
            case R.id.btn_timeline_clean:
                cleanTimelineDataAndLog();
                break;
            default:
                break;
        }
    }

    @Override
    public int getDescription() {
        return R.string.component_listview_timeline_mission_control;
    }

    @NonNull
    @Override
    public String getHint() {
        return this.getClass().getSimpleName() + ".java";
    }
}