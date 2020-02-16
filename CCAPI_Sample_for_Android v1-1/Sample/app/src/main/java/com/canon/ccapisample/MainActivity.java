package com.canon.ccapisample;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static com.canon.ccapisample.Constants.CCAPI.Method.GET;
import android.annotation.SuppressLint;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AuthenticateListener, WebAPIResultListener, DisconnectListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CODE = 1;
    private Spinner mEventMethodSpinner;
    private WifiMonitoringThread mWifiMonitoringThread;
    private Handler mHandler;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    ServerSocket serverSocket;
    Thread Thread1 = null;

    public static String SERVER_IP = "192.168.43.147";
    public static final int SERVER_PORT = 12345;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Access permission of the external storage.
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // Request permission, if not permitted by the user.
            ActivityCompat.requestPermissions(
                    this,
                    new String[] {
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    REQUEST_EXTERNAL_STORAGE
            );
        }

        mHandler = new Handler();

        setContentView(R.layout.activity_main);
        mEventMethodSpinner = (Spinner) findViewById(R.id.EventSpinner);
        mEventMethodSpinner.setSelection(1); // polling(continue)

        findViewById(R.id.DiscoveryButton).setOnClickListener(this);
        findViewById(R.id.DisconnectButton).setOnClickListener(this);
        findViewById(R.id.ToRemoteCaptureButton).setOnClickListener(this);
        findViewById(R.id.ToDeviceInformationButton).setOnClickListener(this);
        findViewById(R.id.ToCameraSettingButton).setOnClickListener(this);
        findViewById(R.id.ToContentsViewerButton).setOnClickListener(this);
        findViewById(R.id.RobotDiscoveryButton).setOnClickListener(this);
        DiscoveryButton = (Button)findViewById(R.id.RobotDiscoveryButton);
    }

    private Button DiscoveryButton;
    private PrintWriter output;
    private BufferedReader input;
    class Thread1 implements Runnable {
        public void run() {
            Socket socket;
            try {
                socket = new Socket(SERVER_IP, SERVER_PORT);
                output = new PrintWriter(socket.getOutputStream());
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this.getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                        new Thread(new Thread3("yubico")).start();
                    }
                });
                new Thread(new Thread2()).start();
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
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this.getApplicationContext(), "clinet:"+ message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
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
            output.write(message);
            output.flush();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //tvMessages.append("server: " + message + "\n");
                    //etMessage.setText("");
                    Toast.makeText(MainActivity.this.getApplicationContext(), "server:"+ message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        if(mWifiMonitoringThread != null) {
            mWifiMonitoringThread.interrupt();
        }
    }

    @Override
    public void onClick(View v){
        if (v != null) {
            switch (v.getId()) {
                case R.id.DiscoveryButton: {
                    Button discovery = (Button) findViewById(R.id.DiscoveryButton);
                    discovery.setEnabled(false);
                    WifiConnection wifiConnection = new WifiConnection();
                    asyncConnect(wifiConnection);
                    break;
                }
                case R.id.DisconnectButton: {
                    WebAPI.getInstance().setListener(this, this);
                    WebAPI.getInstance().enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.POST_FUNCTIONS_NETWORKCONNECTION, null, new WebAPIResultListener() {
                        @Override
                        public void onWebAPIResult(WebAPIResultDataSet result) {
                            Context context = getApplicationContext();
                            if (context != null) {
                                if (result.isError()) {
                                    Toast.makeText(context, result.getErrorMsg(), Toast.LENGTH_SHORT).show();
                                }
                                else{
                                    Toast.makeText(context, "Disconnect Accepted.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }));
                    break;
                }
                case R.id.ToRemoteCaptureButton: {
                    moveToSubActivity(SubActivity.Screen.RemoteCapture);
                    break;
                }
                case R.id.ToDeviceInformationButton: {
                    moveToSubActivity(SubActivity.Screen.DeviceInformation);
                    break;
                }
                case R.id.ToCameraSettingButton: {
                    moveToSubActivity(SubActivity.Screen.CameraFunctions);
                    break;
                }
                case R.id.ToContentsViewerButton: {
                    moveToSubActivity(SubActivity.Screen.ContentsViewer);
                    break;
                }
                case R.id.RobotDiscoveryButton: {
                    Thread1 = new Thread(new Thread1());
                    Thread1.start();
                    break;
                }
                default:
                    break;
            }
        }
    }

    private void setConnectionState(boolean isConnect, String message){
        Button discovery = (Button) findViewById(R.id.DiscoveryButton);
        Button disconnect = (Button) findViewById(R.id.DisconnectButton);

        if ( isConnect ) {
            discovery.setVisibility(View.GONE);
            disconnect.setVisibility(View.VISIBLE);
        }
        else{
            discovery.setVisibility(View.VISIBLE);
            disconnect.setVisibility(View.GONE);
        }

        findViewById(R.id.ToRemoteCaptureButton).setEnabled(isConnect);
        findViewById(R.id.ToDeviceInformationButton).setEnabled(isConnect);
        findViewById(R.id.ToCameraSettingButton).setEnabled(isConnect);
        findViewById(R.id.ToContentsViewerButton).setEnabled(isConnect);

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void asyncConnect(final WifiConnection wifiConnection){
        AsyncTask<Void, Void, String> asyncTask = new AsyncTask<Void, Void, String>(){
            @Override
            protected String doInBackground(Void... params) {
                return wifiConnection.execute();
            }

            @Override
            protected void onPostExecute(String result){
                callbackResult(result);
            }
        };
        asyncTask.execute();
    }

    public void callbackResult(String url){
        Button discovery = (Button) findViewById(R.id.DiscoveryButton);
        discovery.setEnabled(true);

        if(url != null) {
            Log.d(TAG, "Connect Success.");
            Log.d(TAG, url);

            WebAPI.getInstance().start(url);
            WebAPI.getInstance().setListener(this, this);

            // Get all APIs.
            // Generate the API list and update the screen, if they are got.
            Bundle args = new Bundle();
            String[] params = new String[]{GET, url, null};
            args.putStringArray(Constants.RequestCode.ACT_WEB_API.name(), params);
            WebAPI.getInstance().enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.ACT_WEB_API, args, this));
        }
        else{
            Toast.makeText(this, "Connect Failed.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Connect Failed.");
        }
    }

    private void moveToSubActivity(SubActivity.Screen screen){
        String event = mEventMethodSpinner.getSelectedItem().toString();
        Constants.EventMethod method;

        switch (event) {
            case "monitoring":
                method = Constants.EventMethod.MONITORING;
                break;
            case "polling(continue)":
                method = Constants.EventMethod.POLLING_CONTINUE;
                break;
            case "polling":
            default:
                method = Constants.EventMethod.POLLING;
                break;
        }

        // End the monitoring of the Wi-Fi connection.
        if(mWifiMonitoringThread != null) {
            mWifiMonitoringThread.interrupt();
        }

        Intent intent = new Intent(this, SubActivity.class);
        intent.putExtra(SubActivity.SCREEN, screen);
        intent.putExtra(SubActivity.EVENT_METHOD, method);
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult");
        switch (requestCode) {
            case (REQUEST_CODE):
                // When returning from the SubActivity by the Wi-Fi disconnection.
                if (resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    String message = bundle.getString(SubActivity.MESSAGE);
                    Boolean isDisconnection = bundle.getBoolean(SubActivity.IS_DISCONNECTION);

                    if (message != null) {
                        onNotifyDisconnect(message, isDisconnection);
                    }
                }
                // When returning from the SubActivity using the return key.
                else if(resultCode == RESULT_CANCELED){
                    startWifiMonitoringThread();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onWebAPIResult(WebAPIResultDataSet result) {
        if(!result.isError()){
            WebAPI.getInstance().setAPIDataList(result.getResponseBody());
            setConnectionState(true, "Connect Success.");
            startWifiMonitoringThread();
        }
        else{
            Toast.makeText(this, "Connect Failed.\n" + result.getErrorMsg(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void showAuthDialog(DialogFragment dialogFragment, DialogInterface.OnDismissListener onDismissListener) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        dialogFragment.show(fragmentManager, "Authentication Required");
        fragmentManager.executePendingTransactions();
        dialogFragment.getDialog().setOnDismissListener(onDismissListener);
    }

    @Override
    public void onNotifyDisconnect(final String message, final Boolean isDisconnection) {
        Log.d(TAG, "onNotifyDisconnect");
        if(isDisconnection) {
            // Clear digest authentication information when the wireless connection disconnected.
            WebAPI.getInstance().clearDigestAuthInfo();
        }
        else{
            startWifiMonitoringThread();
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                setConnectionState(!isDisconnection, message);
            }
        });
    }

    void startWifiMonitoringThread(){
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
}
