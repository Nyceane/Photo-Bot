package com.canon.ccapisample;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.andretietz.android.controller.DirectionView;
import com.andretietz.android.controller.InputView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.content.Context.WIFI_SERVICE;
import static com.canon.ccapisample.Constants.CCAPI.Field.ACTION;
import static com.canon.ccapisample.Constants.CCAPI.Field.ADDED_CONTENTS;
import static com.canon.ccapisample.Constants.CCAPI.Field.CAMERADISPLAY;
import static com.canon.ccapisample.Constants.CCAPI.Field.LIVEVIEW;
import static com.canon.ccapisample.Constants.CCAPI.Field.LIVEVIEWSIZE;
import static com.canon.ccapisample.Constants.CCAPI.Field.MAX;
import static com.canon.ccapisample.Constants.CCAPI.Field.MESSAGE;
import static com.canon.ccapisample.Constants.CCAPI.Field.MIN;
import static com.canon.ccapisample.Constants.CCAPI.Field.MOVIEMODE;
import static com.canon.ccapisample.Constants.CCAPI.Field.RECBUTTON;
import static com.canon.ccapisample.Constants.CCAPI.Field.STATUS;
import static com.canon.ccapisample.Constants.CCAPI.Field.STEP;
import static com.canon.ccapisample.Constants.CCAPI.Field.ZOOM;
import static com.canon.ccapisample.Constants.CCAPI.Key.SHOOTING_CONTROL_AF;
import static com.canon.ccapisample.Constants.CCAPI.Key.SHOOTING_CONTROL_DRIVEFOCUS;
import static com.canon.ccapisample.Constants.CCAPI.Key.SHOOTING_CONTROL_MOVIEMODE;
import static com.canon.ccapisample.Constants.CCAPI.Key.SHOOTING_CONTROL_ZOOM;
import static com.canon.ccapisample.Constants.CCAPI.Key.SHOOTING_LIVEVIEW_ANGLEINFORMATION;
import static com.canon.ccapisample.Constants.CCAPI.Key.SHOOTING_LIVEVIEW_RTP;
import static com.canon.ccapisample.Constants.CCAPI.Method.DELETE;
import static com.canon.ccapisample.Constants.CCAPI.Method.GET;
import static com.canon.ccapisample.Constants.CCAPI.Method.PUT;
import static com.canon.ccapisample.Constants.CCAPI.UNIT_MAP;
import static com.canon.ccapisample.Constants.CCAPI.Value.MODE_NOT_SUPPORTED;
import static com.canon.ccapisample.Constants.CCAPI.Value.OFF;
import static com.canon.ccapisample.Constants.CCAPI.Value.ON;
import static com.canon.ccapisample.Constants.RemoteCapture.LV_ARRAY;
import static com.canon.ccapisample.Constants.RemoteCapture.LV_DISPLAY_ON;
import static com.canon.ccapisample.Constants.RemoteCapture.LV_DISPLAY_OFF;
import static com.canon.ccapisample.Constants.RemoteCapture.LV_DISPLAY_KEEP;
import static com.canon.ccapisample.Constants.RemoteCapture.LV_SIZE_MEDIUM;
import static com.canon.ccapisample.Constants.RemoteCapture.LV_SIZE_SMALL;
import static com.canon.ccapisample.Constants.RemoteCapture.LV_SIZE_OFF;
import static com.canon.ccapisample.Constants.RemoteCapture.LV_MIDDLE_ON;
import static com.canon.ccapisample.Constants.RemoteCapture.LV_MIDDLE_OFF;
import static com.canon.ccapisample.Constants.RemoteCapture.LV_MIDDLE_KEEP;
import static com.canon.ccapisample.Constants.RemoteCapture.LV_SMALL_ON;
import static com.canon.ccapisample.Constants.RemoteCapture.LV_SMALL_OFF;
import static com.canon.ccapisample.Constants.RemoteCapture.LV_SMALL_KEEP;
import static com.canon.ccapisample.Constants.RemoteCapture.LV_OFF_ON;
import static com.canon.ccapisample.Constants.RemoteCapture.LV_OFF_OFF;
import static com.canon.ccapisample.Constants.RemoteCapture.LV_OFF_KEEP;
import static com.canon.ccapisample.Constants.Settings.Key.KEY;
import static com.canon.ccapisample.Constants.Settings.Key.NAME;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RemoteCaptureFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RemoteCaptureFragment extends Fragment implements WebAPIResultListener, EventListener, View.OnClickListener, RadioGroup.OnCheckedChangeListener, AdapterView.OnItemClickListener, SeekBar.OnSeekBarChangeListener, View.OnTouchListener {
    private static final String TAG = RemoteCaptureFragment.class.getSimpleName();
    private static final String SETTINGS_INFO = "SettingsInfo";
    private static final String LIST_VIEW_KEY_NAME = "name";
    private static final String LIST_VIEW_KEY_VALUE = "value";
    private static final String RTP_SDP_FILE_NAME = "rtpsessiondesc.sdp";

    private WebAPI mWebAPI;
    private LiveViewThread mLiveViewThread;
    private boolean mIsStartRec = false;
    private boolean mIsStartRtp = false;
    private List<ContentsDataSet> mContentsDataSetList = new ArrayList<>();
    private ContentsAdapter mContentsAdapter;
    private ImageProcessor mImageProcessor;
    private JSONArray mSettingsInfo;
    private List<ListViewDataSet> mSettingsDataSetList = new ArrayList<>();
    private CustomSimpleAdapter mAdapter;
    private List<Map<String, String>> mAdapterDataList = new ArrayList<>();
    private List<Boolean> mEditableList = new ArrayList<>();
    private List<Boolean> mGrayOutList = new ArrayList<>();
    private ListViewDataSet mZoomDataSet = null;

    // The flag for determining whether or not the setting of the Live View by the POST request during the period of RemotoCapture startup is completed.
    // Ignore event's notification of the Live View until this flag is true.
    private Boolean mIsFirstLiveViewStarted = false;

    private ImageView mLiveViewImage;
    private TextView mLvSizeText;
    private LinearLayout mControlParentLayout;
    private LinearLayout mRobotParentLayout;
    private SeekBar mZoomBar;
    private CheckBox mAFCheckBox;
    private Button mShutterButton;
    private Button mMovieButton;
    private Spinner mMovieModeSpinner;
    private Spinner mFocusSpinner;
    private Spinner mLiveViewSpinner;
    private RadioGroup mLiveViewMethodRadioGroup;
    private RadioGroup mLiveViewKindRadioGroup;
    private LinearLayout mSettingsParentLayout;
    private LinearLayout mLvInfoParentLayout;
    private ExpandableListView mLvInfoListView;
    private ImageView mHistogramImageView;
    private LinearLayout mAddContentsParentLayout;
    private TextView mContentsCountText;
    private LinearLayout mContentsLayout;

    ServerSocket serverSocket;
    Thread Thread1 = null;

    public RemoteCaptureFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment RemoteCaptureFragment.
     */
    public static RemoteCaptureFragment newInstance(String settings) {
        RemoteCaptureFragment fragment = new RemoteCaptureFragment();
        Bundle args = new Bundle();
        args.putString(SETTINGS_INFO, settings);
        fragment.setArguments(args);
        return fragment;
    }


    private PrintWriter output;
    private BufferedReader input;
    class SendThread implements Runnable {
        private String message;
        SendThread(String message) {
            this.message = message;
        }

        public void run() {
            Socket socket;
            try {
                socket = new Socket(MainActivity.SERVER_IP, MainActivity.SERVER_PORT);
                output = new PrintWriter(socket.getOutputStream());
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output.write(message);
                output.flush();
                socket.close();
                //new Thread(new Thread3(message)).start();
                /*
                RemoteCaptureFragment.this.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(RemoteCaptureFragment.this.getActivity().getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                        //new Thread(new Thread3("yubico")).start();
                    }
                });*/
                //new Thread(new Thread2()).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private class Thread2 implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    final String message = input.readLine();
                    if (message != null) {
                        RemoteCaptureFragment.this.getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.e("input", message);
                                Toast.makeText(RemoteCaptureFragment.this.getActivity().getApplicationContext(), "clinet:"+ message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Log.e("input", "not connected");

                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    class Thread3 implements Runnable {
        private String message;
        Thread3(String message) {
            this.message = message;
        }
        @Override
        public void run() {
            if(output != null && message != null) {
                output.write(message);
                output.flush();
            }
            else if(output == null)
            {
                Log.e("empty", "output empty");
            }
            else
            {
                Log.e("empty", "message empty");
            }
            /*
            RemoteCaptureFragment.this.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //tvMessages.append("server: " + message + "\n");
                    //etMessage.setText("");
                    Toast.makeText(RemoteCaptureFragment.this.getActivity().getApplicationContext(), "sent:"+ message, Toast.LENGTH_SHORT).show();
                }
            });*/
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWebAPI = WebAPI.getInstance();
        mImageProcessor = new ImageProcessor();

        try {
            mSettingsInfo = new JSONArray(getArguments().getString(SETTINGS_INFO));
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        View view = inflater.inflate(R.layout.fragment_remote_capture, container, false);

        // OnClickListener
        mShutterButton = view.findViewById(R.id.ShutterButton);
        mShutterButton.setOnClickListener(this);
        mShutterButton.setTextColor(Color.BLACK);
        mMovieButton = view.findViewById(R.id.MovieButton);
        mMovieButton.setTextColor(Color.RED);
        mMovieButton.setOnClickListener(this);
        view.findViewById(R.id.MovieModeButton).setOnClickListener(this);
        view.findViewById(R.id.ShutterReleaseButton).setOnClickListener(this);
        view.findViewById(R.id.ShutterHalfPressButton).setOnClickListener(this);
        view.findViewById(R.id.ShutterFullPressButton).setOnClickListener(this);
        view.findViewById(R.id.FocusButton).setOnClickListener(this);
        view.findViewById(R.id.LiveViewSettingButton).setOnClickListener(this);
        view.findViewById(R.id.DoAFStartButton).setOnClickListener(this);
        view.findViewById(R.id.DoAFStopButton).setOnClickListener(this);
        view.findViewById(R.id.RtpStartButton).setOnClickListener(this);
        view.findViewById(R.id.RtpStopButton).setOnClickListener(this);
        view.findViewById(R.id.AngleinformationStartButton).setOnClickListener(this);
        view.findViewById(R.id.AngleinformationStopButton).setOnClickListener(this);
        view.findViewById(R.id.ShowControlButton).setOnClickListener(this);
        view.findViewById(R.id.ShowSettingsButton).setOnClickListener(this);
        view.findViewById(R.id.ShowLvInfoButton).setOnClickListener(this);
        view.findViewById(R.id.ShowAddContentsButton).setOnClickListener(this);
        view.findViewById(R.id.RobotControlButton).setOnClickListener(this);

        // OnCheckedChangeListener
        mLiveViewMethodRadioGroup = view.findViewById(R.id.LiveViewMethodRadio);
        mLiveViewMethodRadioGroup.setOnCheckedChangeListener(this);
        mLiveViewKindRadioGroup = view.findViewById(R.id.LiveViewKindRadio);
        mLiveViewKindRadioGroup.setOnCheckedChangeListener(this);

        // OnSeekBarChangeListener
        mZoomBar = view.findViewById(R.id.ZoomBar);
        mZoomBar.setOnSeekBarChangeListener(this);
        mZoomBar.setEnabled(false);

        // Live View
        mLiveViewImage = view.findViewById(R.id.LiveViewImage);
        mLiveViewImage.setOnTouchListener(this);
        mLvSizeText = view.findViewById(R.id.LvSizeText);

        // Shooting Control
        mControlParentLayout = view.findViewById(R.id.ControlParentLayout);
        mAFCheckBox = view.findViewById(R.id.AFCheckBox);
        mMovieModeSpinner = view.findViewById(R.id.MovieModeSpinner);
        mMovieModeSpinner.setEnabled(false);
        mFocusSpinner = view.findViewById(R.id.FocusSpinner);
        mLiveViewSpinner = view.findViewById(R.id.LiveViewSpinner);
        mLiveViewSpinner.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, LV_ARRAY));

        // Robot Control
        mRobotParentLayout = view.findViewById(R.id.RobotParentLayout);

        DirectionView directionView = (DirectionView) view.findViewById(R.id.viewDirection);
        directionView.setOnButtonListener(new InputView.InputEventListener() {
            @Override public void onInputEvent(View view, int buttons) {
                switch (buttons&0xff) {
                    case DirectionView.DIRECTION_DOWN:
                        new Thread(new RemoteCaptureFragment.SendThread("back")).start();
                        Log.e("clicked", "down" + "\n");
                        /*
                        if(output != null)
                        {
                            output.write("back");
                            output.flush();
                        }*/

                        break;
                    case DirectionView.DIRECTION_LEFT:
                        if(output != null) {
                            new Thread(new RemoteCaptureFragment.SendThread("left")).start();
                            Log.e("clicked", "left");
                        }
                        break;
                    case DirectionView.DIRECTION_RIGHT:
                        new Thread(new RemoteCaptureFragment.SendThread("right")).start();
                        Log.e("clicked", "right" + "\n");
                        //if(output != null) {
                        //    output.write("right");
                        //    output.flush();
                        //}
                        break;
                    case DirectionView.DIRECTION_UP:
                        new Thread(new RemoteCaptureFragment.SendThread("forward")).start();
                        Log.e("clicked", "forward" + "\n");
                        //if(output != null) {
                        //    output.write("forward");
                        //    output.flush();
                        //}
                        break;
                    case DirectionView.DIRECTION_DOWN_LEFT:
                        break;
                    case DirectionView.DIRECTION_UP_LEFT:
                        break;
                    case DirectionView.DIRECTION_DOWN_RIGHT:
                        break;
                    case DirectionView.DIRECTION_UP_RIGHT:
                        break;
                }

                // if buttons == 0, the user stopped touching the view
            }
        });

        Button StopButton = view.findViewById(R.id.StopButton);
        StopButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                new Thread(new RemoteCaptureFragment.SendThread("stop")).start();
            }
        });




        APIDataSet api = mWebAPI.getAPIData(SHOOTING_CONTROL_ZOOM);
        if(api == null){
            view.findViewById(R.id.ZoomLayout).setVisibility(View.GONE);
        }

        api = mWebAPI.getAPIData(SHOOTING_CONTROL_MOVIEMODE);
        if(api == null){
            view.findViewById(R.id.MovieModeLayout).setVisibility(View.GONE);
        }

        api = mWebAPI.getAPIData(SHOOTING_CONTROL_DRIVEFOCUS);
        if(api == null){
            view.findViewById(R.id.DriveFocusLayout).setVisibility(View.GONE);
        }

        api = mWebAPI.getAPIData(SHOOTING_CONTROL_AF);
        if(api == null){
            view.findViewById(R.id.DoAFLayout).setVisibility(View.GONE);
        }

        api = mWebAPI.getAPIData(SHOOTING_LIVEVIEW_RTP);
        if(api == null){
            view.findViewById(R.id.RtpLayout).setVisibility(View.GONE);
        }

        api = mWebAPI.getAPIData(SHOOTING_LIVEVIEW_ANGLEINFORMATION);
        if(api == null){
            view.findViewById(R.id.AngleinformationLayout).setVisibility(View.GONE);
        }

        // Shooting Settings
        ListView paramListView = view.findViewById(R.id.ParameterListView);
        mAdapter = new CustomSimpleAdapter(
                getActivity(),
                mAdapterDataList,
                R.layout.list_view_small_item_layout,
                new String[]{LIST_VIEW_KEY_NAME, LIST_VIEW_KEY_VALUE},
                new int[]{android.R.id.text1, android.R.id.text2},
                mEditableList,
                mGrayOutList);
        paramListView.setAdapter(mAdapter);
        paramListView.setOnItemClickListener(this);
        mSettingsParentLayout = view.findViewById(R.id.SettingsParentLayout);

        // LiveView Info
        mLvInfoListView = view.findViewById(R.id.LvInfoListView);
        mLvInfoParentLayout = view.findViewById(R.id.LvInfoParentLayout);
        mHistogramImageView = view.findViewById(R.id.HistogramImageView);

        // Add Contents
        mAddContentsParentLayout = view.findViewById(R.id.AddContentsParentLayout);
        mContentsCountText = view.findViewById(R.id.ContentsCountText);
        mContentsLayout = view.findViewById(R.id.AddContentsLayout);
        createThumbnailList(getActivity());

        startRemoteCapture();

        return view;
    }

    private void startRemoteCapture(){

        setLiveView((String) mLiveViewSpinner.getSelectedItem());

        startLiveViewThread(getLiveViewMethod(), getLiveViewKind());

        mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.GET_SHOOTING_CONTROL_ZOOM, null, this));
        mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.GET_SHOOTING_CONTROL_MOVIEMODE, null, this));

        getListViewSettings();
    }

    private void stopRemoteCapture(){

        stopLiveViewThread();

        setLiveView(LV_OFF_ON);

        if (mIsStartRec) {
            Bundle args = new Bundle();
            args.putString(Constants.RequestCode.POST_SHOOTING_CONTROL_RECBUTTON.name(), "stop");
            mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.POST_SHOOTING_CONTROL_RECBUTTON, args, this));
        }

        if(mIsStartRtp){
            stopRtp();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        stopRemoteCapture();
    }

    @Override
    public void onClick(View v){
        if (v != null) {
            switch (v.getId()) {
                case R.id.MovieButton: {
                    mMovieButton.setEnabled(false);
                    Bundle args = new Bundle();
                    if (!mIsStartRec) {
                        args.putString(Constants.RequestCode.POST_SHOOTING_CONTROL_RECBUTTON.name(), "start");
                    }
                    else {
                        args.putString(Constants.RequestCode.POST_SHOOTING_CONTROL_RECBUTTON.name(), "stop");
                    }
                    mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.POST_SHOOTING_CONTROL_RECBUTTON, args, this));
                    break;
                }
                case R.id.MovieModeButton: {
                    if(mMovieModeSpinner.isEnabled()) {
                        Bundle args = new Bundle();
                        String value = (String) mMovieModeSpinner.getSelectedItem();
                        args.putString(Constants.RequestCode.POST_SHOOTING_CONTROL_MOVIEMODE.name(), value);
                        mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.POST_SHOOTING_CONTROL_MOVIEMODE, args, this));
                    }
                    break;
                }
                case R.id.ShutterButton: {
                    Bundle args = new Bundle();
                    String af = String.valueOf(mAFCheckBox.isChecked());
                    args.putString(Constants.RequestCode.POST_SHOOTING_CONTROL_SHUTTERBUTTON.name(), af);

                    mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.POST_SHOOTING_CONTROL_SHUTTERBUTTON, args, this));
                    break;
                }
                case R.id.ShutterReleaseButton:
                case R.id.ShutterHalfPressButton:
                case R.id.ShutterFullPressButton: {
                    Bundle args = new Bundle();
                    String action = "";
                    String af = String.valueOf(mAFCheckBox.isChecked());

                    if(v.getId() == R.id.ShutterReleaseButton){
                        action = "release";
                    }
                    else if(v.getId() == R.id.ShutterHalfPressButton){
                        action = "half_press";
                    }
                    else if(v.getId() == R.id.ShutterFullPressButton){
                        action = "full_press";
                    }

                    args.putStringArray(Constants.RequestCode.POST_SHOOTING_CONTROL_SHUTTERBUTTON_MANUAL.name(), new String[]{action, af});
                    mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.POST_SHOOTING_CONTROL_SHUTTERBUTTON_MANUAL, args, this));
                    break;
                }
                case R.id.FocusButton: {
                    Bundle args = new Bundle();
                    String value = (String) mFocusSpinner.getSelectedItem();
                    args.putString(Constants.RequestCode.POST_SHOOTING_CONTROL_DRIVEFOCUS.name(), value);
                    mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.POST_SHOOTING_CONTROL_DRIVEFOCUS, args, this));
                    break;
                }
                case R.id.LiveViewSettingButton: {
                    setLiveView((String) mLiveViewSpinner.getSelectedItem());
                    break;
                }
                case R.id.DoAFStartButton: {
                    Bundle args = new Bundle();
                    args.putString(Constants.RequestCode.POST_SHOOTING_CONTROL_AF.name(), "start");
                    mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.POST_SHOOTING_CONTROL_AF, args, this));
                    break;
                }
                case R.id.DoAFStopButton: {
                    Bundle args = new Bundle();
                    args.putString(Constants.RequestCode.POST_SHOOTING_CONTROL_AF.name(), "stop");
                    mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.POST_SHOOTING_CONTROL_AF, args, this));
                    break;
                }
                case R.id.RtpStartButton: {
                    setLiveViewRadioEnabled(false);
                    if(mLiveViewThread != null) {
                        mLiveViewThread.stopThread(new LiveViewThread.Callback() {
                            @Override
                            public void onComplete() {
                                Log.d(TAG, "LiveView stop onComplete");
                                mLiveViewThread = null;
                                startRtp();
                            }
                        });
                    }
                    else{
                        startRtp();
                    }
                    break;
                }
                case R.id.RtpStopButton: {
                    stopRtp();
                    break;
                }
                case R.id.AngleinformationStartButton: {
                    Bundle args = new Bundle();
                    args.putString(Constants.RequestCode.POST_SHOOTING_LIVEVIEW_ANGLEINFORMATION.name(), "start");
                    mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.POST_SHOOTING_LIVEVIEW_ANGLEINFORMATION, args, this));
                    break;
                }
                case R.id.AngleinformationStopButton: {
                    Bundle args = new Bundle();
                    args.putString(Constants.RequestCode.POST_SHOOTING_LIVEVIEW_ANGLEINFORMATION.name(), "stop");
                    mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.POST_SHOOTING_LIVEVIEW_ANGLEINFORMATION, args, this));
                    break;
                }
                case R.id.ShowControlButton:{

                    // Change the display items of the shooting control.
                    // Clear lists when change the AddContents display to other display.
                    if(mAddContentsParentLayout.getVisibility() == View.VISIBLE){
                        mContentsDataSetList.clear();
                        mContentsCountText.setText("0");
                        mContentsAdapter.notifyDataSetChanged();
                    }

                    mSettingsParentLayout.setVisibility(View.GONE);
                    mLvInfoParentLayout.setVisibility(View.GONE);
                    mAddContentsParentLayout.setVisibility(View.GONE);
                    mRobotParentLayout.setVisibility(View.GONE);
                    mControlParentLayout.setVisibility(View.VISIBLE);
                    break;
                }
                case R.id.ShowSettingsButton:{

                    // Change the display lists of the shooting control.
                    // Clear lists when change the AddContents display to other display.
                    if(mAddContentsParentLayout.getVisibility() == View.VISIBLE){
                        mContentsDataSetList.clear();
                        mContentsCountText.setText("0");
                        mContentsAdapter.notifyDataSetChanged();
                    }

                    mControlParentLayout.setVisibility(View.GONE);
                    mLvInfoParentLayout.setVisibility(View.GONE);
                    mAddContentsParentLayout.setVisibility(View.GONE);
                    mRobotParentLayout.setVisibility(View.GONE);
                    mSettingsParentLayout.setVisibility(View.VISIBLE);
                    break;
                }
                case R.id.ShowLvInfoButton:{

                    // Change the display lists of Live View information.
                    // Clear lists when change the AddContents display to other display.
                    if(mAddContentsParentLayout.getVisibility() == View.VISIBLE){
                        mContentsDataSetList.clear();
                        mContentsCountText.setText("0");
                        mContentsAdapter.notifyDataSetChanged();
                    }

                    mControlParentLayout.setVisibility(View.GONE);
                    mSettingsParentLayout.setVisibility(View.GONE);
                    mAddContentsParentLayout.setVisibility(View.GONE);
                    mRobotParentLayout.setVisibility(View.GONE);
                    mLvInfoParentLayout.setVisibility(View.VISIBLE);
                    break;
                }
                case R.id.ShowAddContentsButton:{

                    // Change the display lists of the AddContents.
                    mControlParentLayout.setVisibility(View.GONE);
                    mSettingsParentLayout.setVisibility(View.GONE);
                    mLvInfoParentLayout.setVisibility(View.GONE);
                    mRobotParentLayout.setVisibility(View.GONE);
                    mAddContentsParentLayout.setVisibility(View.VISIBLE);
                    break;
                }
                case R.id.RobotControlButton:{
                    mControlParentLayout.setVisibility(View.GONE);
                    mSettingsParentLayout.setVisibility(View.GONE);
                    mLvInfoParentLayout.setVisibility(View.GONE);

                    mAddContentsParentLayout.setVisibility(View.GONE);
                    mRobotParentLayout.setVisibility(View.VISIBLE);
                    break;
                }
                default:
                    break;
            }
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        Log.d(TAG, "onCheckedChanged");
        switch (group.getId()) {
            case R.id.LiveViewMethodRadio:
            case R.id.LiveViewKindRadio: {

                // Change the display of the Live View.
                setLiveViewRadioEnabled(false);

                changeLiveViewMethod(getLiveViewMethod(), getLiveViewKind());
                break;
            }
            default:
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

        Log.d(TAG, String.format("progress[%d]", seekBar.getProgress()));
        if(mZoomDataSet != null){
            String key = mZoomDataSet.getName();
            Map<String, Map<String, Integer>> rangeMap = mZoomDataSet.getRangeAbility();
            int max = rangeMap.get(key).get(MAX);
            int min = rangeMap.get(key).get(MIN);
            int step = rangeMap.get(key).get(STEP);
            int current = seekBar.getProgress() * step;

            if(current >= min && current <= max) {
                Bundle args = new Bundle();
                args.putInt(Constants.RequestCode.POST_SHOOTING_CONTROL_ZOOM.name(), current);
                mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.POST_SHOOTING_CONTROL_ZOOM, args, this));
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        // The display of dialog when the setting list of shooting is selected.
        Log.d(TAG, String.format("onItemClick[%d]", position));

        ListViewDataSet dataSet = mSettingsDataSetList.get(position);
        String name = dataSet.getName();
        JSONObject settings = null;

        try {

            // Get information of the item which is selected.
            for (int i = 0; i < mSettingsInfo.length(); i++) {
                if (name.equals(mSettingsInfo.getJSONObject(i).getString(NAME))) {
                    settings = mSettingsInfo.getJSONObject(i);
                    break;
                }
            }

            if (settings != null) {
                APIDataSet api = mWebAPI.getAPIData(settings.getString(KEY));
                if (api != null) {
                    if (api.isPutable() || api.isDeletable()) {
                        ListViewDialogFragment dialog = ListViewDialogFragment.newInstance(this, settings, dataSet);
                        dialog.show(getActivity().getSupportFragmentManager(), name);
                    }
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:

                // Execute afframeposition API.
                if(mLiveViewThread != null) {
                    int positionX = mLiveViewThread.getImagePositionX();
                    int positionY = mLiveViewThread.getImagePositionY();
                    int positionWidth = mLiveViewThread.getImagePositionWidth();
                    int positionHeight = mLiveViewThread.getImagePositionHeight();

                    if(positionWidth != 0 && positionHeight != 0) {
                        int width = mLiveViewImage.getWidth();
                        int height = mLiveViewImage.getHeight();
                        float widthScale = positionWidth / (float) width;
                        float heightScale = positionHeight / (float) height;
                        int x = (int) (event.getX() * widthScale) + positionX;
                        int y = (int) (event.getY() * heightScale) + positionY;

                        Log.d(TAG, String.format("onTouch : TouchPosition[ x=%f y=%f ]", event.getX(), event.getY()));
                        Log.d(TAG, String.format("onTouch : ImageViewSize[ %d x %d ]", width, height));
                        Log.d(TAG, String.format("onTouch : Scale[ %f / %f ]", widthScale, heightScale));
                        Log.d(TAG, String.format("onTouch : x=%d y=%d", x, y));

                        Bundle args = new Bundle();
                        args.putIntArray(Constants.RequestCode.PUT_SHOOTING_LIVEVIEW_AFFRAMEPOSITION.name(), new int[]{x, y});
                        mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.PUT_SHOOTING_LIVEVIEW_AFFRAMEPOSITION, args, this));
                    }
                    else{
                        Log.d(TAG, "onTouch : LiveViewInfo cannot get.");
                    }
                }
                else{
                    Log.d(TAG, "onTouch : LiveViewTread is null.");
                }
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * Callback from execute WebAPI
     * @param result HTTP Request result
     */
    @Override
    public void onWebAPIResult(WebAPIResultDataSet result) {
        Log.d(TAG, String.format("%s onWebAPIResult", String.valueOf(result.getRequestCode())));
        Context context = getActivity();

        // Do nothing, if life cycle of the fragment is finished.
        if (context != null) {
            if(result.getRequestCode() == Constants.RequestCode.POST_SHOOTING_CONTROL_RECBUTTON){
                // Enable the rec button when the callback returned from starting/ending of recording.
                mMovieButton.setEnabled(true);
            }

            if (result.isError()) {

                // When RTP has not started.
                if(!mIsStartRtp) {
                    if (result.getRequestCode() == Constants.RequestCode.GET_SHOOTING_LIVEVIEW_RTPSESSIONDESC) {

                        // Restart the Live View when starting of RTP failed.
                        setLiveViewRadioEnabled(true);
                        if (mLiveViewThread == null) {
                            startLiveViewThread(getLiveViewMethod(), getLiveViewKind());
                            setLiveViewRadioEnabled(true);
                        }
                    }
                    else if (result.getRequestCode() == Constants.RequestCode.POST_SHOOTING_LIVEVIEW_RTP) {
                        String action = result.getRequestBodyFromKey(ACTION);
                        if (action != null && action.equals("start")) {

                            // Restart the Live View when starting of RTP failed.
                            setLiveViewRadioEnabled(true);
                            if (mLiveViewThread == null) {
                                startLiveViewThread(getLiveViewMethod(), getLiveViewKind());
                                setLiveViewRadioEnabled(true);
                            }
                        }
                    }
                }

                if(result.getMethod().equals(GET)
                        && result.getResponseCode() == 503
                        && (result.getResponseBodyFromKey(MESSAGE) != null && result.getResponseBodyFromKey(MESSAGE).equals(MODE_NOT_SUPPORTED))){

                    // Error message is not displayed, because setting values can not be got.
                    Log.d(TAG, String.format("%s %s", String.valueOf(result.getRequestCode()), result.getResponseBodyFromKey(MESSAGE)));
                }
                else {
                    Toast.makeText(context, result.getErrorMsg(), Toast.LENGTH_SHORT).show();
                }
            }
            else {
                switch (result.getRequestCode()) {
                    case GET_SHOOTING_CONTROL_MOVIEMODE: {
                        String status = "";

                        try {
                            JSONObject jsonObject = new JSONObject(result.getResponseBody());
                            status = jsonObject.getString(STATUS);
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }

                        updateMovieMode(status);
                        break;
                    }
                    case ACT_WEB_API: {
                        // Get names of setting items.
                        String url = result.getUrl();
                        String name = "";
                        if(url.contains("/shooting/settings/")){
                            String[] split = url.split("/shooting/settings/");
                            name = split[split.length - 1];
                        }
                        else{
                            name = result.getRequestName();
                        }

                        // The callback processing after completion of acquisition of setting values when a Fragment is displayed.
                        if (result.getMethod().equals(GET)) {
                            updateListView(name, result.getResponseBody());
                        }

                        // The callback processing after completion of modification of dialog setting values.
                        else if (result.getMethod().equals(PUT) || result.getMethod().equals(DELETE)){

                            // Reflect the response value in the ListView.
                            ListViewDataSet currentData = null;

                            // Get data of the current list.
                            for(int i = 0; i < mSettingsDataSetList.size(); i++){
                                if (mSettingsDataSetList.get(i).getName().equals(name)){
                                    currentData = mSettingsDataSetList.get(i);
                                    break;
                                }
                            }

                            if (currentData != null){
                                try {

                                    // Generate a ListViewDataSet using the response.
                                    JSONObject jsonObject = new JSONObject(result.getResponseBody());
                                    ListViewDataSet responseData = new ListViewDataSet(name, jsonObject);

                                    // Reflect values of generated data in current data.
                                    currentData.setValue(responseData.getValue());

                                    updateListViewData(currentData);

                                    mAdapter.notifyDataSetChanged();
                                }
                                catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        break;
                    }
                    case GET_SHOOTING_CONTROL_ZOOM: {
                        try {
                            mZoomDataSet = new ListViewDataSet(result.getRequestName(), new JSONObject(result.getResponseBody()));
                            updateZoom();
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                    case POST_SHOOTING_LIVEVIEW: {
                        mIsFirstLiveViewStarted = true;
                        break;
                    }
                    case GET_SHOOTING_LIVEVIEW_RTPSESSIONDESC: {
                        WifiManager manager = (WifiManager)context.getApplicationContext().getSystemService(WIFI_SERVICE);
                        WifiInfo info = manager.getConnectionInfo();
                        int addressNum = info.getIpAddress();
                        String address = String.format(Locale.US, "%02d.%02d.%02d.%02d", (addressNum)&0xff, (addressNum>>8)&0xff, (addressNum>>16)&0xff, (addressNum>>24)&0xff);

                        if(isExternalStorageWritable()) {

                            File dir = context.getExternalFilesDir(null);
                            File file = new File(dir, RTP_SDP_FILE_NAME);
                            FileOutputStream fileOutputStream = null;
                            try {
                                fileOutputStream = new FileOutputStream(file);
                                fileOutputStream.write(result.getBytesResponseBody());
                                fileOutputStream.close();
                            }
                            catch (IOException e) {
                                e.printStackTrace();
                            }
                            finally {
                                if (fileOutputStream != null) {
                                    try {
                                        fileOutputStream.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            Bundle args = new Bundle();
                            args.putStringArray(Constants.RequestCode.POST_SHOOTING_LIVEVIEW_RTP.toString(), new String[]{"start", address});
                            mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.POST_SHOOTING_LIVEVIEW_RTP, args, this));
                        }
                        else{
                            Toast.makeText(context, result.getRequestName() + " : Cannot start RTP. \nThe sdp file cannot be write to the external storage.", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    }
                    case POST_SHOOTING_LIVEVIEW_RTP: {
                        String action = result.getRequestBodyFromKey(ACTION);
                        if(action != null && action.equals("start")) {
                            mIsStartRtp = true;
                            if (isExternalStorageReadable()) {

                                File file = new File(context.getExternalFilesDir(null), RTP_SDP_FILE_NAME);

                                PackageManager packageManager = context.getPackageManager();
                                Uri uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName(), file);

                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setDataAndType(uri, "video/*");
                                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

                                if (!resolveInfo.isEmpty()) {
                                    context.startActivity(intent);
                                }
                            }
                            else {
                                Toast.makeText(context, result.getRequestName() + " : Cannot start RTP. \nThe sdp file cannot be read from the external storage.", Toast.LENGTH_SHORT).show();
                            }
                        }
                        else if(action != null && action.equals("stop")){
                            mIsStartRtp = false;
                            setLiveViewRadioEnabled(true);
                            if(mLiveViewThread == null) {
                                startLiveViewThread(getLiveViewMethod(), getLiveViewKind());
                            }
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
        }
        else{
            Log.d(TAG, String.format("%s Activity is Null.", String.valueOf(result.getRequestCode())));
        }
    }

    @Override
    public void onNotifyEvent(String response){
        try {
            JSONObject jsonObject = new JSONObject(response);
            if(jsonObject.length() != 0) {
                Iterator<String> iterator = jsonObject.keys();
                while(iterator.hasNext()) {
                    String key = iterator.next();
                    String param = jsonObject.getString(key);
                    Log.d(TAG, String.format("onNotifyEvent : %s : %s", key, param));

                    switch (key) {
                        case ZOOM:
                            JSONObject zoomObj = new JSONObject(param);
                            mZoomDataSet = new ListViewDataSet(key, zoomObj);
                            updateZoom();
                            break;
                        case MOVIEMODE:
                            JSONObject movieMode = new JSONObject(param);

                            if(!movieMode.isNull(STATUS)) {
                                String status = movieMode.getString(STATUS);
                                updateMovieMode(status);
                            }
                            break;
                        case ADDED_CONTENTS:
                            JSONArray urlArray = new JSONArray(param);
                            for (int i = 0; i < urlArray.length(); i++) {
                                ContentsDataSet contentsDataSet = new ContentsDataSet(urlArray.getString(i));
                                if (contentsDataSet.getKind() != ContentsDataSet.Kind.DIR) {
                                    mContentsDataSetList.add(contentsDataSet);
                                }
                            }
                            mContentsCountText.setText(String.valueOf(mContentsDataSetList.size()));
                            mContentsAdapter.notifyDataSetChanged();
                            break;
                        case RECBUTTON:
                            JSONObject rec = new JSONObject(param);

                            if(!rec.isNull(STATUS)) {
                                if (!mIsStartRec && rec.getString(STATUS).equals("start")) {
                                    updateRec(true);
                                }
                                else if (mIsStartRec && rec.getString(STATUS).equals("stop")) {
                                    updateRec(false);
                                }
                            }
                            break;
                        case LIVEVIEW:
                            // Ignore events until completion of the Live View setting when the RemoteCapture is displayed.
                            if(mIsFirstLiveViewStarted) {
                                updateLiveViewSetting(param);
                            }
                            break;
                        default:
                            if(key.contains("_")){
                                // Convert "_" to "/" in order to match item names of the application.
                                // ex: "soundrecording_level" -> "soundrecording/level"
                                key = key.replace("_", "/");
                            }

                            updateListView(key, param);
                            break;
                    }
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setLiveViewRadioEnabled(Boolean enable){
        for(int i = 0; i < mLiveViewMethodRadioGroup.getChildCount(); i++){
            mLiveViewMethodRadioGroup.getChildAt(i).setEnabled(enable);
        }

        int methodID = mLiveViewMethodRadioGroup.getCheckedRadioButtonId();

        if (!enable || (methodID == R.id.FlipdetailRadioButton || methodID == R.id.ScrolldetailRadioButton)) {
            for (int i = 0; i < mLiveViewKindRadioGroup.getChildCount(); i++) {
                mLiveViewKindRadioGroup.getChildAt(i).setEnabled(enable);
            }
        }
    }

    private void startLiveViewThread(Constants.LiveViewMethod method, Constants.LiveViewKind kind){
        mLiveViewThread = new LiveViewThread(getContext(), mLiveViewImage, mLvSizeText, mHistogramImageView, mLvInfoListView, method, kind);
        mLiveViewThread.start();
    }

    private void stopLiveViewThread(){
        if(mLiveViewThread != null) {
            mLiveViewThread.stopThread(new LiveViewThread.Callback() {
                @Override
                public void onComplete() {
                    Log.d(TAG, "LiveView stop onComplete");
                }
            });
        }
    }

    private void changeLiveViewMethod(final Constants.LiveViewMethod method, final Constants.LiveViewKind kind){
        if(mLiveViewThread != null) {
            mLiveViewThread.stopThread(new LiveViewThread.Callback() {
                @Override
                public void onComplete() {
                    Log.d(TAG, "LiveView stop onComplete");
                    mLiveViewThread = new LiveViewThread(getContext(), mLiveViewImage, mLvSizeText, mHistogramImageView, mLvInfoListView, method, kind);
                    mLiveViewThread.start();
                    setLiveViewRadioEnabled(true);
                }
            });
        }
    }

    private void getListViewSettings(){
        for(int i = 0; i < mSettingsInfo.length(); i++) {
            Bundle args = new Bundle();
            try {
                APIDataSet api = mWebAPI.getAPIData(mSettingsInfo.getJSONObject(i).getString(KEY));
                if ( api != null ) {

                    createListView(mSettingsInfo.getJSONObject(i).getString(NAME));

                    String[] params = new String[]{GET, api.getUrl(), null};
                    args.putStringArray(Constants.RequestCode.ACT_WEB_API.name(), params);
                    mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.ACT_WEB_API, args, this));
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private Map<String, String> createAdapterData(ListViewDataSet dataSet){
        Map<String, String> map = new HashMap<>();
        map.put(LIST_VIEW_KEY_NAME, dataSet.getName());
        StringBuilder stringBuilder = new StringBuilder();

        for(String item : dataSet.getItems()){
            String value = dataSet.getValue().get(item);
            String unit = null;

            if(!value.equals("null")) {
                if (UNIT_MAP.containsKey(item)) {
                    unit = UNIT_MAP.get(item);
                }
            }
            else{
                value = "";
            }

            if(dataSet.getItems().size() > 1) {
                stringBuilder.append(item).append(" : ").append(value);
            }
            else{
                stringBuilder.append(value);
            }

            if(unit != null){
                stringBuilder.append(" ").append(unit);
            }

            stringBuilder.append("\n");
        }

        if(stringBuilder.length() > 0) {
            map.put(LIST_VIEW_KEY_VALUE, stringBuilder.substring(0, stringBuilder.length() - 1));
        }
        else{
            map.put(LIST_VIEW_KEY_VALUE, "");
        }

        return map;
    }

    private void addListViewData(ListViewDataSet dataSet){
        Map<String, String> map = createAdapterData(dataSet);
        Boolean editable = false;
        try {
            for(int i = 0; i < mSettingsInfo.length(); i++) {
                if (mSettingsInfo.getJSONObject(i).getString(NAME).equals(dataSet.getName())) {
                    JSONObject settings = mSettingsInfo.getJSONObject(i);
                    APIDataSet api = mWebAPI.getAPIData(settings.getString(KEY));
                    if (api != null) {
                        if (api.isPutable()) {
                            editable = true;
                        }
                    }
                    break;
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        mEditableList.add(editable);

        if(editable) {
            mGrayOutList.add(true);
        }
        else{
            mGrayOutList.add(false);
        }
        mAdapterDataList.add(map);
        mSettingsDataSetList.add(dataSet);
    }

    private void updateListViewData(ListViewDataSet dataSet){
        boolean isExist = false;
        int index = 0;

        for(int i = 0; i < mSettingsDataSetList.size(); i++){
            if (mSettingsDataSetList.get(i).getName().equals(dataSet.getName())){
                isExist = true;
                index = i;
                break;
            }
        }

        if (isExist){
            Map<String, String> map = createAdapterData(dataSet);
            mAdapterDataList.set(index, map);
            mSettingsDataSetList.set(index, dataSet);

            if(mEditableList.get(index) && !dataSet.isSettable()){
                mGrayOutList.set(index, true);
            }
            else{
                mGrayOutList.set(index, false);
            }
        }
    }

    private void createListView(String name){
        Log.d(TAG, "createListView: " + name);
        ListViewDataSet listViewDataSet = new ListViewDataSet();
        listViewDataSet.setName(name);
        addListViewData(listViewDataSet);
        mAdapter.notifyDataSetChanged();
    }

    private void updateListView(String name, String responseBody){
        Log.d(TAG, "updateListView: " + name);

        try {
            JSONObject response = new JSONObject(responseBody);
            ListViewDataSet listViewDataSet = new ListViewDataSet(name, response);
            updateListViewData(listViewDataSet);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        mAdapter.notifyDataSetChanged();
    }

    private void createThumbnailList(Context context){
        LinearLayout parentLayout = new LinearLayout(context);
        parentLayout.setOrientation(LinearLayout.VERTICAL);

        GridView gridView = new GridView(context);
        mContentsAdapter = new ContentsAdapter(context, mContentsDataSetList, mImageProcessor);
        gridView.setAdapter(mContentsAdapter);
        gridView.setOnScrollListener(mContentsAdapter);
        gridView.setNumColumns(3);
        gridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        registerForContextMenu(gridView);

        parentLayout.addView(gridView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mContentsLayout.addView(parentLayout, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
    }

    private Constants.LiveViewMethod getLiveViewMethod(){
        Constants.LiveViewMethod method = Constants.LiveViewMethod.FLIPDETAIL;
        int methodID = mLiveViewMethodRadioGroup.getCheckedRadioButtonId();

        switch(methodID){
            case R.id.FlipRadioButton:
                method = Constants.LiveViewMethod.FLIP;
                break;
            case R.id.FlipdetailRadioButton:
                method = Constants.LiveViewMethod.FLIPDETAIL;
                break;
            case R.id.ScrollRadioButton:
                method = Constants.LiveViewMethod.SCROLL;
                break;
            case R.id.ScrolldetailRadioButton:
                method = Constants.LiveViewMethod.SCROLLDETAIL;
                break;
            default:
                break;
        }

        return method;
    }

    private Constants.LiveViewKind getLiveViewKind(){
        Constants.LiveViewKind kind = Constants.LiveViewKind.IMAGE;
        int kindID = mLiveViewKindRadioGroup.getCheckedRadioButtonId();

        if (kindID == R.id.ImageRadioButton) {
            kind = Constants.LiveViewKind.IMAGE;
        }
        else if (kindID == R.id.ImageAndInfoRadioButton) {
            kind = Constants.LiveViewKind.IMAGE_AND_INFO;
        }
        else if (kindID == R.id.InfoRadioButton) {
            kind = Constants.LiveViewKind.INFO;
        }
        return kind;
    }

    private void updateMovieMode(String status){
        mMovieModeSpinner.setEnabled(true);

        switch (status) {
            case ON:
                mMovieButton.setTextColor(Color.RED);
                mShutterButton.setTextColor(Color.GRAY);
                mMovieModeSpinner.setSelection(0);
                break;
            case OFF:
                mMovieButton.setTextColor(Color.GRAY);
                mShutterButton.setTextColor(Color.BLACK);
                mMovieModeSpinner.setSelection(1);
                break;
            default:
                break;
        }
    }

    private void updateZoom(){
        if(mZoomDataSet != null) {
            try {
                String key = mZoomDataSet.getName();
                Map<String, String> valueMap = mZoomDataSet.getValue();
                Map<String, Map<String, Integer>> rangeMap = mZoomDataSet.getRangeAbility();
                int current = Integer.valueOf(valueMap.get(key));
                int max = rangeMap.get(key).get(MAX);
                int min = rangeMap.get(key).get(MIN);
                int step = rangeMap.get(key).get(STEP);

                if (min == 0) {
                    mZoomBar.setProgress(current / step);
                    mZoomBar.setMax(max / step);
                    mZoomBar.setEnabled(true);
                }
                else {
                    mZoomDataSet = null;
                }
            }
            catch (NumberFormatException e) {
                e.printStackTrace();
                mZoomDataSet = null;
            }
        }
    }

    private void updateRec(Boolean isStart){
        Log.d(TAG, String.format("updateRec: %b", isStart));
        if (isStart) {
            mIsStartRec = true;
            mMovieButton.setText("■");
        }
        else {
            mIsStartRec = false;
            mMovieButton.setText("●Rec");
        }
    }

    private void updateLiveViewSetting(String json){
        try {
            JSONObject jsonObject = new JSONObject(json);
            String size = "";
            String display = "";
            String size_display = "";

            if(!jsonObject.isNull(LIVEVIEWSIZE) && !jsonObject.isNull(CAMERADISPLAY)){
                size = jsonObject.getString(LIVEVIEWSIZE);
                display = jsonObject.getString(CAMERADISPLAY);
                size_display = size + "/" + display;

                SpinnerAdapter adapter = mLiveViewSpinner.getAdapter();
                int index = 0;
                for (int i = 0; i < adapter.getCount(); i++) {
                    if (adapter.getItem(i).equals(size_display)) {
                        index = i;
                        break;
                    }
                }
                mLiveViewSpinner.setSelection(index);

                // Display a message and stop the Live View when the Live View turns OFF.
                if(size.equals(LV_SIZE_OFF)){
                    Context context = getContext();
                    if(context != null) {
                        Toast.makeText(context, "LiveView stopped.", Toast.LENGTH_SHORT).show();
                    }

                    if(mLiveViewThread != null) {
                        setLiveViewRadioEnabled(false);
                        mLiveViewThread.stopThread(new LiveViewThread.Callback() {
                            @Override
                            public void onComplete() {
                                Log.d(TAG, "LiveView stop onComplete");
                                mLiveViewThread = null;
                            }
                        });
                    }
                }
                else{
                    // Restart a thread when the Live View turns ON.
                    if(mLiveViewThread == null) {
                        startLiveViewThread(getLiveViewMethod(), getLiveViewKind());
                        setLiveViewRadioEnabled(true);
                    }
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setLiveView(String value){
        String size = "";
        String display = "";

        switch (value){
            case LV_OFF_OFF:
                size = LV_SIZE_OFF;
                display = LV_DISPLAY_OFF;
                break;
            case LV_OFF_ON:
                size = LV_SIZE_OFF;
                display = LV_DISPLAY_ON;
                break;
            case LV_OFF_KEEP:
                size = LV_SIZE_OFF;
                display = LV_DISPLAY_KEEP;
            break;
            case LV_SMALL_OFF:
                size = LV_SIZE_SMALL;
                display = LV_DISPLAY_OFF;
                break;
            case LV_SMALL_ON:
                size = LV_SIZE_SMALL;
                display = LV_DISPLAY_ON;
                break;
            case LV_SMALL_KEEP:
                size = LV_SIZE_SMALL;
                display = LV_DISPLAY_KEEP;
                break;
            case LV_MIDDLE_OFF:
                size = LV_SIZE_MEDIUM;
                display = LV_DISPLAY_OFF;
                break;
            case LV_MIDDLE_ON:
                size = LV_SIZE_MEDIUM;
                display = LV_DISPLAY_ON;
                break;
            case LV_MIDDLE_KEEP:
                size = LV_SIZE_MEDIUM;
                display = LV_DISPLAY_KEEP;
                break;
            default:
                break;
        }

        Bundle args = new Bundle();
        args.putStringArray(Constants.RequestCode.POST_SHOOTING_LIVEVIEW.name(), new String[]{size, display});
        mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.POST_SHOOTING_LIVEVIEW, args, this));
    }

    private void startRtp(){
        // Get SDP.
        // Start RTP if the getting is successful.
        mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.GET_SHOOTING_LIVEVIEW_RTPSESSIONDESC, null, this));
    }

    private void stopRtp(){
        Bundle args = new Bundle();
        args.putStringArray(Constants.RequestCode.POST_SHOOTING_LIVEVIEW_RTP.toString(), new String[]{"stop", ""});
        mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.POST_SHOOTING_LIVEVIEW_RTP, args, this));
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }
}
