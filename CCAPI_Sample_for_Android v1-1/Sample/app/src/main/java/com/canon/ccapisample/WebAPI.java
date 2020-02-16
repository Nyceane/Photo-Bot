package com.canon.ccapisample;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.canon.ccapisample.Constants.CCAPI.SUPPORTED_API_VERSION;
import static com.canon.ccapisample.Constants.CCAPI.Field.ACTION;
import static com.canon.ccapisample.Constants.CCAPI.Field.CAMERADISPLAY;
import static com.canon.ccapisample.Constants.CCAPI.Field.DATETIME;
import static com.canon.ccapisample.Constants.CCAPI.Field.DST;
import static com.canon.ccapisample.Constants.CCAPI.Field.LIVEVIEWSIZE;
import static com.canon.ccapisample.Constants.CCAPI.Field.POSITION_X;
import static com.canon.ccapisample.Constants.CCAPI.Field.POSITION_Y;
import static com.canon.ccapisample.Constants.CCAPI.Field.STATUS;
import static com.canon.ccapisample.Constants.CCAPI.Field.STORAGE_NAME;
import static com.canon.ccapisample.Constants.CCAPI.Field.VALUE;
import static com.canon.ccapisample.Constants.CCAPI.Key.DEVICESTATUS_STORAGE;
import static com.canon.ccapisample.Constants.CCAPI.Key.EVENT_MONITORING;
import static com.canon.ccapisample.Constants.CCAPI.Key.EVENT_POLLING;
import static com.canon.ccapisample.Constants.CCAPI.Key.FUNCTIONS_CARDFORMAT;
import static com.canon.ccapisample.Constants.CCAPI.Key.FUNCTIONS_DATETIME;
import static com.canon.ccapisample.Constants.CCAPI.Key.FUNCTIONS_WIFICONNECTION;
import static com.canon.ccapisample.Constants.CCAPI.Key.FUNCTIONS_NETWORKCONNECTION;
import static com.canon.ccapisample.Constants.CCAPI.Key.FUNCTIONS_WIFISETTING;
import static com.canon.ccapisample.Constants.CCAPI.Key.SHOOTING_CONTROL_AF;
import static com.canon.ccapisample.Constants.CCAPI.Key.SHOOTING_CONTROL_DRIVEFOCUS;
import static com.canon.ccapisample.Constants.CCAPI.Key.SHOOTING_CONTROL_MOVIEMODE;
import static com.canon.ccapisample.Constants.CCAPI.Key.SHOOTING_CONTROL_RECBUTTON;
import static com.canon.ccapisample.Constants.CCAPI.Key.SHOOTING_CONTROL_SHOOTINGMODE;
import static com.canon.ccapisample.Constants.CCAPI.Key.SHOOTING_CONTROL_SHUTTERBUTTON;
import static com.canon.ccapisample.Constants.CCAPI.Key.SHOOTING_CONTROL_SHUTTERBUTTON_MANUAL;
import static com.canon.ccapisample.Constants.CCAPI.Key.SHOOTING_CONTROL_ZOOM;
import static com.canon.ccapisample.Constants.CCAPI.Key.SHOOTING_LIVEVIEW;
import static com.canon.ccapisample.Constants.CCAPI.Key.SHOOTING_LIVEVIEW_AFFRAMEPOSITION;
import static com.canon.ccapisample.Constants.CCAPI.Key.SHOOTING_LIVEVIEW_ANGLEINFORMATION;
import static com.canon.ccapisample.Constants.CCAPI.Key.SHOOTING_LIVEVIEW_FLIP;
import static com.canon.ccapisample.Constants.CCAPI.Key.SHOOTING_LIVEVIEW_FLIPDETAIL;
import static com.canon.ccapisample.Constants.CCAPI.Key.SHOOTING_LIVEVIEW_RTP;
import static com.canon.ccapisample.Constants.CCAPI.Key.SHOOTING_LIVEVIEW_RTPSESSIONDESC;
import static com.canon.ccapisample.Constants.CCAPI.Key.SHOOTING_LIVEVIEW_SCROLL;
import static com.canon.ccapisample.Constants.CCAPI.Key.SHOOTING_LIVEVIEW_SCROLLDETAIL;
import static com.canon.ccapisample.Constants.CCAPI.Method.DELETE;
import static com.canon.ccapisample.Constants.CCAPI.Method.GET;
import static com.canon.ccapisample.Constants.CCAPI.Method.POST;
import static com.canon.ccapisample.Constants.CCAPI.Method.PUT;


class WebAPI{
    private static final String TAG = WebAPI.class.getSimpleName();
    private static final int DEFAULT_TIMEOUT = 60000;

    private static WebAPI sWebAPI = null;
    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private static final int KEEP_ALIVE_TIME = 1;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

    private final ThreadPoolExecutor mTaskThreadPool;

    private Map<String, APIDataSet> mAPIDataMap = new HashMap<>();
    private SerialThread mSerialThread = null;
    private ParallelThread mParallelThread = null;
    private String mUrl;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private final Object mSyncAuthObj = new Object();
    private final Object mSyncCurrentComObj = new Object();
    private HttpDigestAuth mHttpDigestAuth = null;
    private AuthenticateListener mAuthenticateListener;
    private DisconnectListener mDisconnectListener;
    private HttpCommunication mCurrentSerialCommunication = null;
    private int mAuthErrorCount = 0;

    private class SerialThread extends Thread{
        private ConcurrentLinkedQueue<WebAPIQueueDataSet> mSerialQueue = new ConcurrentLinkedQueue<>();
        private Boolean isWait = false;
        @Override
        public void run() {
            Log.d(TAG, "SerialThread Start.");

            while(!mSerialThread.isInterrupted()){
                try {
                    if(mSerialQueue.isEmpty() || isWait) {
                        // Wait for the enqueueRequest to be called.
                        synchronized(this) {
                            this.wait();
                        }
                    }
                    else {
                        // Receive a queue.
                        final WebAPIQueueDataSet webAPIQueueDataSet = mSerialQueue.poll();

                        if (webAPIQueueDataSet != null) {
                            Log.d(TAG, String.format("%s SerialThread Queue Receive.", String.valueOf(webAPIQueueDataSet.getRequestCode())));

                            // Wait the next request until current request is completed.
                            executeRequest(webAPIQueueDataSet);
                        }
                    }
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }

            Log.d(TAG, "SerialThread Stop.");
        }

        void addQueue(WebAPIQueueDataSet webAPIQueueDataSet){
            mSerialQueue.add(webAPIQueueDataSet);
            if(!isWait) {
                synchronized (this) {
                    this.notifyAll();
                }
            }
        }

        void waitThread(){
            isWait = true;
        }

        void notifyThread(){
            if(isWait) {
                isWait = false;
                synchronized (this) {
                    this.notifyAll();
                }
            }
        }

        void clearQueue(){
            mSerialQueue.clear();
        }
    }

    private class ParallelThread extends Thread{
        private ConcurrentLinkedQueue<WebAPIQueueDataSet> mParallelQueue = new ConcurrentLinkedQueue<>();
        private Boolean isWait = false;
        @Override
        public void run() {
            Log.d(TAG, "ParallelThread Start.");

            while(!mParallelThread.isInterrupted()){
                try {
                    if(mParallelQueue.isEmpty() || isWait) {
                        // Wait for the enqueueRequest to be called.
                        synchronized(this) {
                            this.wait();
                        }
                    }
                    else {
                        // Receive a queue.
                        final WebAPIQueueDataSet webAPIQueueDataSet = mParallelQueue.poll();

                        if (webAPIQueueDataSet != null) {
                            Log.d(TAG, String.format("%s ParallelThread Queue Receive.", String.valueOf(webAPIQueueDataSet.getRequestCode())));

                            // The request execute in other thread.
                            // And accept the next request immediately.
                            mTaskThreadPool.execute(
                                    new Thread() {
                                        @Override
                                        public void run() {
                                            Log.d(TAG, String.format("%s TaskThreadPool Begin.", String.valueOf(webAPIQueueDataSet.getRequestCode())));
                                            executeRequest(webAPIQueueDataSet);
                                            Log.d(TAG, String.format("%s TaskThreadPool End.", String.valueOf(webAPIQueueDataSet.getRequestCode())));
                                        }
                                    }
                            );
                        }
                    }
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }

            Log.d(TAG, "ParallelThread Stop.");
        }

        void addQueue(WebAPIQueueDataSet webAPIQueueDataSet){
            mParallelQueue.add(webAPIQueueDataSet);
            if(!isWait) {
                synchronized (this) {
                    this.notifyAll();
                }
            }
        }

        void waitThread(){
            isWait = true;
        }

        void notifyThread(){
            if(isWait) {
                isWait = false;
                synchronized (this) {
                    this.notifyAll();
                }
            }
        }
    }

    public static WebAPI getInstance(){
        if(sWebAPI == null) {
            sWebAPI = new WebAPI();
        }
        return sWebAPI;
    }

    private WebAPI(){
        BlockingQueue<Runnable> poolWorkQueue = new LinkedBlockingQueue<>();

        // Generate ThreadPool.
        mTaskThreadPool = new ThreadPoolExecutor(
                NUMBER_OF_CORES,
                NUMBER_OF_CORES,
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                poolWorkQueue);
    }

    String getUrl(){
        return mUrl;
    }

    String[] getDigestAuthInfo(){
        String[] strings = null;

        synchronized (mSyncAuthObj){
            if(mHttpDigestAuth != null){
                strings = new String[2];
                strings[0] = mHttpDigestAuth.getUserName();
                strings[1] = mHttpDigestAuth.getPassword();
            }
        }

        return strings;
    }

    String getDigestAuthHeader(String method, String url, byte[] body){
        String header = null;
        synchronized (mSyncAuthObj){
            if(mHttpDigestAuth != null){
                header = mHttpDigestAuth.getDigestAuthHeader(method, url, body, null);
            }
        }
        return header;
    }

    void clearDigestAuthInfo(){
        synchronized (mSyncAuthObj){
            mHttpDigestAuth = null;
        }
    }

    APIDataSet getAPIData(String key){
        if(mAPIDataMap.get(key) == null){
            Log.d(TAG, key + " is not supported.");
        }
        return mAPIDataMap.get(key);
    }

    void setAPIDataList(String body){
        mAPIDataMap.clear();
        try {
            JSONObject jsonObject = new JSONObject(body);
            for(int j = 0; j < SUPPORTED_API_VERSION.length; j++){
                Log.d(TAG, SUPPORTED_API_VERSION[j]);

                if(!jsonObject.isNull(SUPPORTED_API_VERSION[j])){
                    JSONArray jsonArray = jsonObject.getJSONArray(SUPPORTED_API_VERSION[j]);
                    for(int i = 0; i < jsonArray.length(); i++){
                        APIDataSet apiDataSet = new APIDataSet(mUrl, SUPPORTED_API_VERSION[j], jsonArray.getJSONObject(i));
                        if(!apiDataSet.getKey().isEmpty()) {
                            mAPIDataMap.put(apiDataSet.getKey(), apiDataSet);
                            Log.d(TAG, apiDataSet.getUrl());
                        }
                    }
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void setListener(AuthenticateListener authenticateListener, DisconnectListener disconnectListener){
        mAuthenticateListener = authenticateListener;
        mDisconnectListener = disconnectListener;
    }

    void start(String url){
        // Start the WebAPI Thread.
        if (mSerialThread == null && mParallelThread == null) {
            mSerialThread = new SerialThread();
            mParallelThread = new ParallelThread();
            mSerialThread.start();
            mParallelThread.start();
        }
        mUrl = url;
    }

    void waitThread(){
        mSerialThread.waitThread();
        mParallelThread.waitThread();
    }

    void notifyThread(){
        mSerialThread.notifyThread();
        mParallelThread.notifyThread();
    }

    void cancelFileDownload(String url){
        synchronized (mSyncCurrentComObj) {
            if (mCurrentSerialCommunication != null && mCurrentSerialCommunication.getRequestUrl().equals(url)) {
                mCurrentSerialCommunication.cancelReadResponse();
            }
            else{
                Log.d(TAG, "not cancel");
            }
        }
    }

    void cancelAllSerialRequest(){
        // Cancel all requests when it is started recording in ContentsViewer.
        synchronized (mSyncCurrentComObj) {
            mSerialThread.clearQueue();
            if (mCurrentSerialCommunication != null) {
                mCurrentSerialCommunication.cancelReadResponse();
            }
        }
    }

    void enqueueRequest(WebAPIQueueDataSet webAPIQueueDataSet){
        Log.d(TAG, "TaskThreadPool ActiveCount / NumberOfCores :" + String.valueOf(mTaskThreadPool.getActiveCount()) + "/" + String.valueOf(NUMBER_OF_CORES));
        if (webAPIQueueDataSet.isParallel()){
            mParallelThread.addQueue(webAPIQueueDataSet);
        }
        else{
            mSerialThread.addQueue(webAPIQueueDataSet);
        }
    }

    private void executeRequest(WebAPIQueueDataSet webAPIQueueDataSet){
        switch (webAPIQueueDataSet.getRequestCode()){
            case ACT_WEB_API:
                actWebAPI(webAPIQueueDataSet);
                break;
            case GET_DEVICESTATUS_STORAGE:
                sendDeviceStatusStorage(webAPIQueueDataSet, GET);
                break;
            case GET_FUNCTIONS_DATETIME:
                sendFunctionsDatetime(webAPIQueueDataSet, GET);
                break;
            case PUT_FUNCTIONS_DATETIME:
                sendFunctionsDatetime(webAPIQueueDataSet, PUT);
                break;
            case GET_FUNCTIONS_WIFISETTINGINFORMATION:
                sendFunctionsWifisetting(webAPIQueueDataSet, GET);
                break;
            case PUT_FUNCTIONS_WIFISETTINGINFORMATION:
                sendFunctionsWifisetting(webAPIQueueDataSet, PUT);
                break;
            case DELETE_FUNCTIONS_WIFISETTINGINFORMATION:
                sendFunctionsWifisetting(webAPIQueueDataSet, DELETE);
                break;
            case POST_FUNCTIONS_CARDFORMAT:
                sendFunctionsFormat(webAPIQueueDataSet, POST);
                break;
            case POST_FUNCTIONS_WIFICONNECTION:
                sendFunctionsWificonnection(webAPIQueueDataSet, POST);
                break;
            case POST_FUNCTIONS_NETWORKCONNECTION:
                sendFunctionsNetworkconnection(webAPIQueueDataSet, POST);
                break;
            case GET_SHOOTING_CONTROL_SHOOTINGMODE:
                sendShootingControlShootingmode(webAPIQueueDataSet, GET);
                break;
            case POST_SHOOTING_CONTROL_SHOOTINGMODE:
                sendShootingControlShootingmode(webAPIQueueDataSet, POST);
                break;
            case POST_SHOOTING_CONTROL_SHUTTERBUTTON:
                sendShootingControlShutterbutton(webAPIQueueDataSet, POST);
                break;
            case POST_SHOOTING_CONTROL_SHUTTERBUTTON_MANUAL:
                sendShootingControlShutterbuttonManual(webAPIQueueDataSet, POST);
                break;
            case POST_SHOOTING_CONTROL_RECBUTTON:
                sendShootingControlRecbutton(webAPIQueueDataSet, POST);
                break;
            case GET_SHOOTING_CONTROL_MOVIEMODE:
                sendShootingControlMoviecontrol(webAPIQueueDataSet, GET);
                break;
            case POST_SHOOTING_CONTROL_MOVIEMODE:
                sendShootingControlMoviecontrol(webAPIQueueDataSet, POST);
                break;
            case GET_SHOOTING_CONTROL_ZOOM:
                sendShootingControlZoom(webAPIQueueDataSet, GET);
                break;
            case POST_SHOOTING_CONTROL_ZOOM:
                sendShootingControlZoom(webAPIQueueDataSet, POST);
                break;
            case POST_SHOOTING_CONTROL_DRIVEFOCUS:
                sendShootingControlDrivefocus(webAPIQueueDataSet, POST);
                break;
            case POST_SHOOTING_CONTROL_AF:
                sendShootingControlAf(webAPIQueueDataSet, POST);
                break;
            case POST_SHOOTING_LIVEVIEW:
                sendShootingLiveview(webAPIQueueDataSet, POST);
                break;
            case GET_SHOOTING_LIVEVIEW_FLIP:
                sendShootingLiveviewFlip(webAPIQueueDataSet, GET);
                break;
            case GET_SHOOTING_LIVEVIEW_FLIPDETAIL:
                sendShootingLiveviewFlipOrg(webAPIQueueDataSet, GET);
                break;
            case GET_SHOOTING_LIVEVIEW_SCROLL:
                sendShootingLiveviewScroll(webAPIQueueDataSet, GET);
                break;
            case DELETE_SHOOTING_LIVEVIEW_SCROLL:
                sendShootingLiveviewScroll(webAPIQueueDataSet, DELETE);
                break;
            case GET_SHOOTING_LIVEVIEW_SCROLLDETAIL:
                sendShootingLiveviewScrollOrg(webAPIQueueDataSet, GET);
                break;
            case DELETE_SHOOTING_LIVEVIEW_SCROLLDETEIL:
                sendShootingLiveviewScrollOrg(webAPIQueueDataSet, DELETE);
                break;
            case GET_SHOOTING_LIVEVIEW_RTPSESSIONDESC:
                sendShootingLiveviewRtpSessionDesc(webAPIQueueDataSet, GET);
                break;
            case POST_SHOOTING_LIVEVIEW_RTP:
                sendShootingLiveviewRtp(webAPIQueueDataSet, POST);
                break;
            case POST_SHOOTING_LIVEVIEW_ANGLEINFORMATION:
                sendShootingLiveviewAngleinformation(webAPIQueueDataSet, POST);
                break;
            case PUT_SHOOTING_LIVEVIEW_AFFRAMEPOSITION:
                sendShootingLiveviewAfframeposition(webAPIQueueDataSet, PUT);
                break;
            case GET_EVENT_POLLING:
                sendEventPolling(webAPIQueueDataSet, GET);
                break;
            case DELETE_EVENT_POLLING:
                sendEventPolling(webAPIQueueDataSet, DELETE);
                break;
            case GET_EVENT_MONITORING:
                sendEventMonitoring(webAPIQueueDataSet, GET);
                break;
            case DELETE_EVENT_MONITORING:
                sendEventMonitoring(webAPIQueueDataSet, DELETE);
                break;
            default:
                break;
        }
    }

    private void sendRequest(final String method, final String url, final byte[] body, File uploadFile, final WebAPIQueueDataSet webAPIQueueDataSet, int timeout){
        String authHeader = null;
        HttpResultDataSet httpResultDataSet;
        WebAPIResultDataSet result;
        HttpDigestAuth httpDigestAuth = null;
        HttpCommunication httpCommunication = new HttpCommunication(method, url, body, uploadFile, timeout);

        if (!webAPIQueueDataSet.isParallel()) {
            // Save the request which is executing if the serial processing.
            synchronized (mSyncCurrentComObj) {
                mCurrentSerialCommunication = httpCommunication;
            }
        }

        // In the case of first authentication, a header is generated using the previous HttpDigestAuth object.
        synchronized (mSyncAuthObj) {
            if (mHttpDigestAuth != null) {
                authHeader = mHttpDigestAuth.getDigestAuthHeader(method, url, body, null);
            }
        }

        while(true) {
            if (!webAPIQueueDataSet.isParallel()) {
                synchronized (mSyncCurrentComObj) {
                    mCurrentSerialCommunication = httpCommunication;
                }
            }

            if (webAPIQueueDataSet.getChunkResultListener() == null) {
                httpResultDataSet = httpCommunication.sendRequest(authHeader, webAPIQueueDataSet.getHttpProgressListener());
            }
            else {
                httpResultDataSet = httpCommunication.getChunkResponse(authHeader, webAPIQueueDataSet.getChunkResultListener());
            }

            result = new WebAPIResultDataSet(webAPIQueueDataSet.getRequestCode(), method, url, body, httpResultDataSet);

            if (httpResultDataSet.getResponseCode() == 401 && httpResultDataSet.getResponseHeaderMap().containsKey("WWW-Authenticate")) {
                synchronized (mSyncAuthObj) {
                    mAuthErrorCount++;
                    Log.d(TAG, String.format("%s : Authenticate Error.(%d)", url, mAuthErrorCount));

                    // When a first authentication error occurred.
                    if (mHttpDigestAuth == null) {
                        final CountDownLatch signal = new CountDownLatch(1);
                        final AuthenticateDialogFragment dialogFragment = AuthenticateDialogFragment.newInstance();
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    mAuthenticateListener.showAuthDialog(dialogFragment, new DialogInterface.OnDismissListener() {
                                        @Override
                                        public void onDismiss(DialogInterface dialog) {
                                            signal.countDown();
                                        }
                                    });
                                }
                                catch (IllegalStateException e) {
                                    e.printStackTrace();
                                    signal.countDown();
                                }
                            }
                        });

                        // Wait until the dialog is closed.
                        try {
                            signal.await();
                        }
                        catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        // Get the authentication information.
                        String userName = dialogFragment.getUserName();
                        String password = dialogFragment.getPassword();

                        if (userName == null && password == null) {
                            // Do nothing, if the dialog is canceled.
                            Log.d(TAG, "Auth Dialog Cancel.");
                            break;
                        }
                        else {
                            // Generate a authentication header and send a request again.
                            Log.d(TAG, "Auth Dialog OK.");
                            httpDigestAuth = new HttpDigestAuth(userName, password);
                            authHeader = httpDigestAuth.getDigestAuthHeader(method, url, body, httpResultDataSet.getResponseHeaderMap().get("WWW-Authenticate"));
                            Log.d(TAG, authHeader);
                        }
                    }
                    else {
                        if(mAuthErrorCount < 10) {
                            // In case of other than the first authentication error,
                            // Generate a authentication header and send a request again.
                            Log.d(TAG, url + " : Auth Request resend.");
                            if (mHttpDigestAuth != null) {
                                authHeader = mHttpDigestAuth.getDigestAuthHeader(method, url, body, null);
                            }
                            else{
                                break;
                            }
                        }
                        else{
                            // Reset the application if authentication fail continuously.
                            Log.d(TAG, url + " : Auth Reset.");
                            mDisconnectListener.onNotifyDisconnect("Authenticate Info Reset.", true);
                            break;
                        }
                    }
                }
            }
            else{
                if(mAuthErrorCount != 0) {
                    synchronized (mSyncAuthObj) {
                        mAuthErrorCount = 0;
                    }
                    Log.d(TAG, String.format("%s : Authenticate Error Count Clear.", url));
                }

                if(httpResultDataSet.getResponseCode() == 200 || httpResultDataSet.getResponseCode() == 202) {
                    if (mHttpDigestAuth == null) {
                        synchronized (mSyncAuthObj) {
                            mHttpDigestAuth = httpDigestAuth;
                        }
                    }
                }
                break;
            }
        }

        if (!webAPIQueueDataSet.isParallel()) {
            synchronized (mSyncCurrentComObj) {
                mCurrentSerialCommunication = null;
            }
        }

        callbackResult(webAPIQueueDataSet, result);
    }

    private void sendRequest(String method, String url, byte[] body, WebAPIQueueDataSet webAPIQueueDataSet){
        sendRequest(method, url, body, null, webAPIQueueDataSet, DEFAULT_TIMEOUT);
    }

    private void sendRequest(String method, String url, String body, WebAPIQueueDataSet webAPIQueueDataSet, int timeout){
        if(body != null) {
            try {
                byte[] bytes = body.getBytes("UTF-8");
                sendRequest(method, url, bytes, null, webAPIQueueDataSet, timeout);
            }
            catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        else{
            sendRequest(method, url, null, null, webAPIQueueDataSet, timeout);
        }
    }

    private void sendRequest(String method, String url, String body, WebAPIQueueDataSet webAPIQueueDataSet){
        sendRequest(method, url, body, webAPIQueueDataSet, DEFAULT_TIMEOUT);
    }

    private void callbackResult(final WebAPIQueueDataSet webAPIQueueDataSet, final WebAPIResultDataSet result){
        if(webAPIQueueDataSet.getWebAPIResultListener() != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    webAPIQueueDataSet.getWebAPIResultListener().onWebAPIResult(result);
                }
            });
        }
    }

    private void actWebAPI(WebAPIQueueDataSet webAPIQueueDataSet){
        String method;
        String uri;
        String body;
        Bundle args = webAPIQueueDataSet.getArguments();

        if(args != null) {
            String[] params = args.getStringArray(webAPIQueueDataSet.getRequestCode().name());
            if(params != null) {
                method = params[0];
                uri = params[1];
                body = params[2];
                sendRequest(method, uri, body, webAPIQueueDataSet);
            }
        }
    }

    private void sendDeviceStatusStorage(WebAPIQueueDataSet webAPIQueueDataSet, String method){
        APIDataSet api = mAPIDataMap.get(DEVICESTATUS_STORAGE);
        if(api != null){
            sendRequest(method, api.getUrl(), (byte[]) null, webAPIQueueDataSet);
        }
    }

    private void sendFunctionsDatetime(WebAPIQueueDataSet webAPIQueueDataSet, String method){
        APIDataSet api = mAPIDataMap.get(FUNCTIONS_DATETIME);
        if(api != null) {
            String body = null;
            if (method.equals(PUT)) {
                Bundle args = webAPIQueueDataSet.getArguments();
                if (args != null) {
                    String[] params = args.getStringArray(webAPIQueueDataSet.getRequestCode().name());
                    if (params != null) {
                        body = String.format("{ \"%s\": \"%s\", \"%s\": %s }", DATETIME, params[0], DST, params[1]);
                    }
                }
            }
            sendRequest(method, api.getUrl(), body, webAPIQueueDataSet);
        }
    }

    private void sendFunctionsWifisetting(WebAPIQueueDataSet webAPIQueueDataSet, String method){
        APIDataSet api = mAPIDataMap.get(FUNCTIONS_WIFISETTING);
        if(api != null) {
            String body = null;
            if (method.equals(PUT)) {
                Bundle args = webAPIQueueDataSet.getArguments();
                if (args != null) {
                    body = args.getString(webAPIQueueDataSet.getRequestCode().name());
                }
            }
            sendRequest(method, api.getUrl(), body, webAPIQueueDataSet);
        }
    }

    private void sendFunctionsFormat(WebAPIQueueDataSet webAPIQueueDataSet, String method){
        APIDataSet api = mAPIDataMap.get(FUNCTIONS_CARDFORMAT);
        if(api != null) {
            String body = null;
            if (method.equals(POST)) {
                Bundle args = webAPIQueueDataSet.getArguments();
                if (args != null) {
                    body = String.format("{\"%s\": \"%s\"}", STORAGE_NAME, args.getString(webAPIQueueDataSet.getRequestCode().name()));
                }
            }
            sendRequest(method, api.getUrl(), body, webAPIQueueDataSet);
        }
    }

    private void sendFunctionsWificonnection(WebAPIQueueDataSet webAPIQueueDataSet, String method){
        APIDataSet api = mAPIDataMap.get(FUNCTIONS_WIFICONNECTION);
        if(api != null) {
            String body = "{\"" + ACTION + "\": \"disconnect\"}";
            sendRequest(method, api.getUrl(), body, webAPIQueueDataSet);
        }
    }

    private void sendFunctionsNetworkconnection(WebAPIQueueDataSet webAPIQueueDataSet, String method){
        APIDataSet api = mAPIDataMap.get(FUNCTIONS_NETWORKCONNECTION);
        if(api != null) {
            String body = "{\"" + ACTION + "\": \"disconnect\"}";
            sendRequest(method, api.getUrl(), body, webAPIQueueDataSet);
        }
    }

    private void sendShootingControlShootingmode(WebAPIQueueDataSet webAPIQueueDataSet, String method){
        APIDataSet api = mAPIDataMap.get(SHOOTING_CONTROL_SHOOTINGMODE);
        if(api != null) {
            String body = null;
            if (method.equals(POST)) {
                Bundle args = webAPIQueueDataSet.getArguments();
                if (args != null) {
                    String param = args.getString(webAPIQueueDataSet.getRequestCode().name());
                    body = String.format("{ \"%s\": \"%s\" }", STATUS, param);
                }
            }

            sendRequest(method, api.getUrl(), body, webAPIQueueDataSet);
        }
    }

    private void sendShootingControlShutterbutton(WebAPIQueueDataSet webAPIQueueDataSet, String method){
        APIDataSet api = mAPIDataMap.get(SHOOTING_CONTROL_SHUTTERBUTTON);
        if(api != null) {
            String body = null;
            if (method.equals(POST)) {
                Bundle args = webAPIQueueDataSet.getArguments();
                if (args != null) {
                    String param = args.getString(webAPIQueueDataSet.getRequestCode().name());
                    JSONObject json = new JSONObject();
                    try {
                        json.put("af", Boolean.valueOf(param));
                    }
                    catch (JSONException | NumberFormatException e) {
                        e.printStackTrace();
                    }
                    body = json.toString();
                    Log.d(TAG, body);
                }
            }
            sendRequest(method, api.getUrl(), body, webAPIQueueDataSet);
        }
    }

    private void sendShootingControlShutterbuttonManual(WebAPIQueueDataSet webAPIQueueDataSet, String method){
        APIDataSet api = mAPIDataMap.get(SHOOTING_CONTROL_SHUTTERBUTTON_MANUAL);
        if(api != null) {
            String body = null;
            if (method.equals(POST)) {
                Bundle args = webAPIQueueDataSet.getArguments();
                if (args != null) {
                    String[] param = args.getStringArray(webAPIQueueDataSet.getRequestCode().name());
                    if(param != null && param.length == 2) {
                        JSONObject json = new JSONObject();
                        try {
                            json.put(ACTION, param[0]);
                            json.put("af", Boolean.valueOf(param[1]));
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }
                        body = json.toString();
                        Log.d(TAG, body);
                    }
                }
            }
            sendRequest(method, api.getUrl(), body, webAPIQueueDataSet);
        }
    }

    private void sendShootingControlRecbutton(WebAPIQueueDataSet webAPIQueueDataSet, String method){
        APIDataSet api = mAPIDataMap.get(SHOOTING_CONTROL_RECBUTTON);
        if(api != null) {
            String body = null;
            if (method.equals(POST)) {
                Bundle args = webAPIQueueDataSet.getArguments();
                if (args != null) {
                    String param = args.getString(webAPIQueueDataSet.getRequestCode().name());
                    body = String.format("{\"%s\": \"%s\"}", ACTION, param);
                }
            }
            sendRequest(method, api.getUrl(), body, webAPIQueueDataSet);
        }
    }

    private void sendShootingControlMoviecontrol(WebAPIQueueDataSet webAPIQueueDataSet, String method){
        APIDataSet api = mAPIDataMap.get(SHOOTING_CONTROL_MOVIEMODE);
        if(api != null) {
            String body = null;
            if (method.equals(POST)) {
                Bundle args = webAPIQueueDataSet.getArguments();
                if (args != null) {
                    String param = args.getString(webAPIQueueDataSet.getRequestCode().name());
                    body = String.format("{\"%s\": \"%s\"}", ACTION, param);
                }
            }
            sendRequest(method, api.getUrl(), body, webAPIQueueDataSet);
        }
    }

    private void sendShootingControlZoom(WebAPIQueueDataSet webAPIQueueDataSet, String method){
        APIDataSet api = mAPIDataMap.get(SHOOTING_CONTROL_ZOOM);
        if(api != null) {
            String body = null;
            if (method.equals(POST)) {
                Bundle args = webAPIQueueDataSet.getArguments();
                if (args != null) {
                    int param = args.getInt(webAPIQueueDataSet.getRequestCode().name());
                    body = String.format(Locale.US, "{\"%s\": %d}", VALUE, param);
                }
            }
            sendRequest(method, api.getUrl(), body, webAPIQueueDataSet);
        }
    }

    private void sendShootingControlDrivefocus(WebAPIQueueDataSet webAPIQueueDataSet, String method){
        APIDataSet api = mAPIDataMap.get(SHOOTING_CONTROL_DRIVEFOCUS);
        if(api != null) {
            String body = null;
            if (method.equals(POST)) {
                Bundle args = webAPIQueueDataSet.getArguments();
                if (args != null) {
                    String param = args.getString(webAPIQueueDataSet.getRequestCode().name());
                    body = String.format("{\"%s\": \"%s\"}", VALUE, param);
                }
            }
            sendRequest(method, api.getUrl(), body, webAPIQueueDataSet);
        }
    }

    private void sendShootingControlAf(WebAPIQueueDataSet webAPIQueueDataSet, String method){
        APIDataSet api = mAPIDataMap.get(SHOOTING_CONTROL_AF);
        if(api != null) {
            String body = null;
            if (method.equals(POST)) {
                Bundle args = webAPIQueueDataSet.getArguments();
                if (args != null) {
                    String param = args.getString(webAPIQueueDataSet.getRequestCode().name());
                    body = String.format("{\"%s\": \"%s\"}", ACTION, param);
                }
            }
            sendRequest(method, api.getUrl(), body, webAPIQueueDataSet);
        }
    }

    private void sendShootingLiveviewAfframeposition(WebAPIQueueDataSet webAPIQueueDataSet, String method){
        APIDataSet api = mAPIDataMap.get(SHOOTING_LIVEVIEW_AFFRAMEPOSITION);
        if(api != null) {
            String body = null;
            if (method.equals(PUT)) {
                Bundle args = webAPIQueueDataSet.getArguments();
                if (args != null) {
                    int[] param = args.getIntArray(webAPIQueueDataSet.getRequestCode().name());
                    if(param != null && param.length == 2) {
                        JSONObject json = new JSONObject();
                        try {
                            json.put(POSITION_X, param[0]);
                            json.put(POSITION_Y, param[1]);
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }
                        body = json.toString();
                        Log.d(TAG, body);
                    }
                }
            }
            sendRequest(method, api.getUrl(), body, webAPIQueueDataSet);
        }
    }

    private void sendShootingLiveview(WebAPIQueueDataSet webAPIQueueDataSet, String method){
        APIDataSet api = mAPIDataMap.get(SHOOTING_LIVEVIEW);
        if(api != null) {
            String body = null;
            if (method.equals(POST)) {
                Bundle args = webAPIQueueDataSet.getArguments();
                if (args != null) {
                    String[] param = args.getStringArray(webAPIQueueDataSet.getRequestCode().name());
                    if(param != null && param.length == 2) {
                        JSONObject json = new JSONObject();
                        try {
                            json.put(LIVEVIEWSIZE, param[0]);
                            json.put(CAMERADISPLAY, param[1]);
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }
                        body = json.toString();
                        Log.d(TAG, body);
                    }
                }
            }
            sendRequest(method, api.getUrl(), body, webAPIQueueDataSet);
        }
    }

    private void sendShootingLiveviewFlip(WebAPIQueueDataSet webAPIQueueDataSet, String method){
        APIDataSet api = mAPIDataMap.get(SHOOTING_LIVEVIEW_FLIP);
        if(api != null) {
            sendRequest(method, api.getUrl(), (byte[]) null, webAPIQueueDataSet);
        }
    }

    private void sendShootingLiveviewFlipOrg(WebAPIQueueDataSet webAPIQueueDataSet, String method){
        APIDataSet api = mAPIDataMap.get(SHOOTING_LIVEVIEW_FLIPDETAIL);
        if(api != null) {
            String url = api.getUrl();
            Bundle args = webAPIQueueDataSet.getArguments();
            if (args != null) {
                String kind = args.getString(webAPIQueueDataSet.getRequestCode().name());
                url += "?kind=" + kind;
            }
            sendRequest(method, url, (byte[]) null, webAPIQueueDataSet);
        }
    }

    private void sendShootingLiveviewScroll(WebAPIQueueDataSet webAPIQueueDataSet, String method){
        APIDataSet api = mAPIDataMap.get(SHOOTING_LIVEVIEW_SCROLL);
        if(api != null) {
            sendRequest(method, api.getUrl(), (byte[]) null, webAPIQueueDataSet);
        }
    }

    private void sendShootingLiveviewScrollOrg(WebAPIQueueDataSet webAPIQueueDataSet, String method){
        APIDataSet api = mAPIDataMap.get(SHOOTING_LIVEVIEW_SCROLLDETAIL);
        if(api != null) {
            String url = api.getUrl();
            Bundle args = webAPIQueueDataSet.getArguments();
            if (args != null) {
                String kind = args.getString(webAPIQueueDataSet.getRequestCode().name());
                url += "?kind=" + kind;
            }
            sendRequest(method, url, (byte[]) null, webAPIQueueDataSet);
        }
    }

    private void sendShootingLiveviewRtpSessionDesc(WebAPIQueueDataSet webAPIQueueDataSet, String method){
        APIDataSet api = mAPIDataMap.get(SHOOTING_LIVEVIEW_RTPSESSIONDESC);
        if(api != null) {
            sendRequest(method, api.getUrl(), (byte[]) null, webAPIQueueDataSet);
        }
    }

    private void sendShootingLiveviewRtp(WebAPIQueueDataSet webAPIQueueDataSet, String method){
        APIDataSet api = mAPIDataMap.get(SHOOTING_LIVEVIEW_RTP);
        if(api != null) {
            String body = null;
            if (method.equals(POST)) {
                Bundle args = webAPIQueueDataSet.getArguments();
                if (args != null) {
                    String[] param = args.getStringArray(webAPIQueueDataSet.getRequestCode().name());
                    if(param != null && param.length == 2) {
                        JSONObject json = new JSONObject();
                        try {
                            json.put(ACTION, param[0]);
                            json.put("ipaddress", param[1]);
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }
                        body = json.toString();
                        Log.d(TAG, body);
                    }
                }
            }
            sendRequest(method, api.getUrl(), body, webAPIQueueDataSet);
        }
    }

    private void sendShootingLiveviewAngleinformation(WebAPIQueueDataSet webAPIQueueDataSet, String method){
        APIDataSet api = mAPIDataMap.get(SHOOTING_LIVEVIEW_ANGLEINFORMATION);
        if(api != null) {
            String body = null;
            if (method.equals(POST)) {
                Bundle args = webAPIQueueDataSet.getArguments();
                if (args != null) {
                    String param = args.getString(webAPIQueueDataSet.getRequestCode().name());
                    body = String.format("{\"%s\": \"%s\"}", ACTION, param);
                }
            }
            sendRequest(method, api.getUrl(), body, webAPIQueueDataSet);
        }
    }

    private void sendEventPolling(WebAPIQueueDataSet webAPIQueueDataSet, String method){
        APIDataSet api = mAPIDataMap.get(EVENT_POLLING);
        if(api != null) {
            String url = api.getUrl();

            Bundle args = webAPIQueueDataSet.getArguments();
            if (args != null) {
                Boolean isContinue = args.getBoolean(webAPIQueueDataSet.getRequestCode().name());
                if(isContinue){
                    url += "?continue=on";
                }
            }

            sendRequest(method, url, null, null, webAPIQueueDataSet, DEFAULT_TIMEOUT);
        }
    }

    private void sendEventMonitoring(WebAPIQueueDataSet webAPIQueueDataSet, String method){
        APIDataSet api = mAPIDataMap.get(EVENT_MONITORING);
        if(api != null) {
            sendRequest(method, api.getUrl(), (byte[]) null, webAPIQueueDataSet);
        }
    }
}
