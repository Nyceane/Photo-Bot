package com.canon.ccapisample;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static com.canon.ccapisample.Constants.Settings.Key.*;


public class SubActivity extends AppCompatActivity implements EventListener, AuthenticateListener, DisconnectListener{
    enum Screen {
        RemoteCapture,
        DeviceInformation,
        CameraFunctions,
        ContentsViewer,
    }

    static final String SCREEN = "Screen";
    static final String EVENT_METHOD = "EventMethod";
    static final String MESSAGE = "Message";
    static final String IS_DISCONNECTION = "IsDisconnection";

    private static final String TAG = SubActivity.class.getSimpleName();
    private EventThread mEventThread;
    private WifiMonitoringThread mWifiMonitoringThread;

    private Screen mCurrentScreen;
    private Map<String, Object> mSettingsInfoMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        setContentView(R.layout.activity_sub);

        Intent intent = getIntent();
        Constants.EventMethod eventMethod = (Constants.EventMethod) intent.getSerializableExtra(EVENT_METHOD);

        WebAPI.getInstance().setListener(this, this);

        try {
            String settings = readFile(Constants.Settings.FileName.SETTINGS);
            JSONObject jsonSettings = new JSONObject(settings);

            mSettingsInfoMap.put(SHOOTING_SETTINGS, jsonSettings.getJSONArray(SHOOTING_SETTINGS));
            mSettingsInfoMap.put(DEVICE_INFORMATION, jsonSettings.getJSONArray(DEVICE_INFORMATION));
            mSettingsInfoMap.put(CAMERA_FUNCTIONS, jsonSettings.getJSONArray(CAMERA_FUNCTIONS));
        }
        catch (JSONException | IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "File Read Failed.", Toast.LENGTH_SHORT).show();
        }

        // Display the initial screen.
        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            mCurrentScreen = (Screen) intent.getSerializableExtra(SCREEN);
            setTitle(mCurrentScreen.toString());

            switch (mCurrentScreen) {
                case RemoteCapture:
                    transaction.replace(R.id.container, RemoteCaptureFragment.newInstance(mSettingsInfoMap.get(SHOOTING_SETTINGS).toString()));
                    break;
                case DeviceInformation:
                    transaction.replace(R.id.container, ListViewFragment.newInstance(mSettingsInfoMap.get(DEVICE_INFORMATION).toString()));
                    break;
                case CameraFunctions:
                    transaction.replace(R.id.container, ListViewFragment.newInstance(mSettingsInfoMap.get(CAMERA_FUNCTIONS).toString()));
                    break;
                case ContentsViewer:
                    transaction.replace(R.id.container, ContentsViewerFragment.newInstance());
                    break;
            }
            transaction.commit();
        }

        mEventThread = new EventThread(eventMethod, this);
        mEventThread.start();

        // Start monitoring of the wireless connection.
        try {
            URL url = new URL(WebAPI.getInstance().getUrl());
            mWifiMonitoringThread = new WifiMonitoringThread(url.getHost(), this);
            mWifiMonitoringThread.start();
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
            Log.d(TAG, "WifiMonitoringThread cannot start.");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_fragment_main_drawer, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        if(mWifiMonitoringThread != null) {
            mWifiMonitoringThread.interrupt();
        }

        mEventThread.stopThread(new EventThread.Callback() {
            @Override
            public void onComplete() {
                Log.d(TAG, "Event stop onComplete");
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        manager.popBackStack();

        if (id == R.id.action_remote_capture && mCurrentScreen != Screen.RemoteCapture) {
            mCurrentScreen = Screen.RemoteCapture;
            transaction.replace(R.id.container, RemoteCaptureFragment.newInstance(mSettingsInfoMap.get(SHOOTING_SETTINGS).toString()));
            transaction.commit();
        }
        else if (id == R.id.action_device_information && mCurrentScreen != Screen.DeviceInformation) {
            mCurrentScreen = Screen.DeviceInformation;
            transaction.replace(R.id.container, ListViewFragment.newInstance(mSettingsInfoMap.get(DEVICE_INFORMATION).toString()));
            transaction.commit();
        }
        else if (id == R.id.action_camera_functions && mCurrentScreen != Screen.CameraFunctions) {
            mCurrentScreen = Screen.CameraFunctions;
            transaction.replace(R.id.container, ListViewFragment.newInstance(mSettingsInfoMap.get(CAMERA_FUNCTIONS).toString()));
            transaction.commit();
        }
        else if (id == R.id.action_contents_viewer && mCurrentScreen != Screen.ContentsViewer) {
            mCurrentScreen = Screen.ContentsViewer;
            transaction.replace(R.id.container, ContentsViewerFragment.newInstance());
            transaction.commit();
        }

        setTitle(mCurrentScreen.toString());
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onNotifyEvent(String response) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
        if (fragment != null && fragment instanceof EventListener) {
            ((EventListener) fragment).onNotifyEvent(response);
        }
        else{
            Log.d(TAG, "onNotifyEvent : Error.");
        }
    }

    @Override
    public void onNotifyDisconnect(String message, Boolean isDisconnection) {
        Log.d(TAG, "onNotifyDisconnect");
        Intent intent = new Intent();
        intent.putExtra(SubActivity.MESSAGE, message);
        intent.putExtra(SubActivity.IS_DISCONNECTION, isDisconnection);
        setResult(RESULT_OK, intent);
        finish();
    }

    private String readFile(String filename) throws IOException{
        String ret = null;
        InputStream input = this.getAssets().open(filename);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(input));
        String buffer = "";
        String str;
        while ((str = bufferedReader.readLine()) != null) {
            buffer = buffer + str + "\n";
        }
        input.close();
        ret = buffer;
        return ret;
    }

    @Override
    public void showAuthDialog(DialogFragment dialogFragment, DialogInterface.OnDismissListener onDismissListener) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        dialogFragment.show(fragmentManager, "Authentication Required");
        fragmentManager.executePendingTransactions();
        dialogFragment.getDialog().setOnDismissListener(onDismissListener);
    }
}
