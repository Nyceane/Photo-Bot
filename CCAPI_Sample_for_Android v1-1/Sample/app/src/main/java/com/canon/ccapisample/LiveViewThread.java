package com.canon.ccapisample;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.canon.ccapisample.Constants.CCAPI.Field.AF_FRAME;
import static com.canon.ccapisample.Constants.CCAPI.Field.HEIGHT;
import static com.canon.ccapisample.Constants.CCAPI.Field.HISTOGRAM;
import static com.canon.ccapisample.Constants.CCAPI.Field.IMAGE;
import static com.canon.ccapisample.Constants.CCAPI.Field.MAGNIFICATION;
import static com.canon.ccapisample.Constants.CCAPI.Field.POSITION_HEIGHT;
import static com.canon.ccapisample.Constants.CCAPI.Field.POSITION_WIDTH;
import static com.canon.ccapisample.Constants.CCAPI.Field.POSITION_X;
import static com.canon.ccapisample.Constants.CCAPI.Field.POSITION_Y;
import static com.canon.ccapisample.Constants.CCAPI.Field.SELECT;
import static com.canon.ccapisample.Constants.CCAPI.Field.STATUS;
import static com.canon.ccapisample.Constants.CCAPI.Field.VISIBLE;
import static com.canon.ccapisample.Constants.CCAPI.Field.WIDTH;
import static com.canon.ccapisample.Constants.CCAPI.Field.X;
import static com.canon.ccapisample.Constants.CCAPI.Field.Y;
import static com.canon.ccapisample.Constants.CCAPI.Field.ZOOM;

class LiveViewThread extends Thread{
    interface Callback{
        void onComplete();
    }

    private static final String TAG = LiveViewThread.class.getSimpleName();
    private static final String LIST_VIEW_KEY_GROUP = "group";
    private static final String LIST_VIEW_KEY_NAME = "name";
    private static final String LIST_VIEW_KEY_VALUE = "value";
    private static final int HISTOGRAM_LENGTH = 256;
    private static final int HISTOGRAM_LIMIT = 256;
    private static final int HISTOGRAM_SCALE = 100;
    private static final int HISTOGRAM_VIEW_WIDTH = HISTOGRAM_LENGTH + 2; /* number of the histogram elements         +  frame */
    private static final int HISTOGRAM_VIEW_HEIGHT = HISTOGRAM_LIMIT + 2; /* maximum number of the histogram display  +  frame */

    private final WeakReference<ImageView> mImageViewReference;
    private final WeakReference<TextView> mLvSizeTextViewReference;
    private final WeakReference<ExpandableListView> mInfoListViewReference;
    private final WeakReference<ImageView> mHistogramReference;
    private Handler mHandler;
    private Constants.LiveViewMethod mMethod;
    private Constants.LiveViewKind mKind;
    private WebAPI mWebAPI;
    private Bitmap mBitmap;

    private SimpleExpandableListAdapter mAdapter;
    private List<Map<String, String>> mAdapterParentList = new ArrayList<>();
    private List<List<Map<String, String>>> mAdapterChildList = new ArrayList<>();
    private Map<String, Integer> mListViewIndexMap = new HashMap<>();

    private JSONArray mAfFrameArray = null;
    private Map<String, Integer> mLvImageInfoMap = new HashMap<>();
    private Map<String, Integer> mLvVisibleInfoMap = new HashMap<>();
    private Map<String, Integer> mLvZoomInfoMap = new HashMap<>();

    private ByteArrayOutputStream mChunkByteArrayOutputStream = new ByteArrayOutputStream();
    private int mChunkDataSize = 0;

    private int mFrameCount = 0;
    private long mStartTime = 0;
    private long mElapsedTime = 0;

    LiveViewThread(Context context, ImageView imageView, TextView lvSizeText, ImageView histogram, ExpandableListView infoListView, Constants.LiveViewMethod method, Constants.LiveViewKind kind){
        this.mHandler = new Handler();
        this.mImageViewReference = new WeakReference<>(imageView);
        this.mLvSizeTextViewReference = new WeakReference<>(lvSizeText);
        this.mInfoListViewReference = new WeakReference<>(infoListView);
        this.mHistogramReference = new WeakReference<>(histogram);
        this.mMethod = method;
        this.mKind = kind;
        this.mWebAPI = WebAPI.getInstance();

        this.mAdapter = new SimpleExpandableListAdapter(
                context,
                mAdapterParentList,
                R.layout.expandable_list_view_small_parent_item_layout,
                new String[] { LIST_VIEW_KEY_GROUP },
                new int[] { R.id.ParentItemText },
                mAdapterChildList,
                R.layout.expandable_list_view_small_child_item_layout,
                new String[] { LIST_VIEW_KEY_NAME, LIST_VIEW_KEY_VALUE },
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        infoListView.setAdapter(mAdapter);
    }

    @Override
    public void run() {
        Log.d(TAG, String.format("LiveViewThread Start. %s / %s", mMethod.toString(), mKind.toString()));

        while(!isInterrupted()) {
            try {
                synchronized (this) {
                    switch(mMethod){
                        case FLIP:
                            flipLiveView(false);
                            break;
                        case FLIPDETAIL:
                            flipLiveView(true);
                            break;
                        case SCROLL:
                            scrollLiveView(false);
                            break;
                        case SCROLLDETAIL:
                            scrollLiveView(true);
                            break;
                        default:
                            break;
                    }

                    this.wait();
                }
            }
            catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }

        Log.d(TAG, String.format("LiveViewThread End. %s / %s", mMethod.toString(), mKind.toString()));
    }

    void stopThread(final Callback callback){
        if(isAlive()) {
            WebAPIResultListener webAPIResultListener = new WebAPIResultListener() {
                @Override
                public void onWebAPIResult(WebAPIResultDataSet result) {
                    interrupt();
                    Log.d(TAG, String.format("stopThread : onWebAPIResult. %s / %s", mMethod.toString(), mKind.toString()));
                    callback.onComplete();
                }
            };

            switch (mMethod) {
                case FLIP:
                case FLIPDETAIL:
                    interrupt();
                    callback.onComplete();
                    break;
                case SCROLL:
                    mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.DELETE_SHOOTING_LIVEVIEW_SCROLL, null, webAPIResultListener));
                    break;
                case SCROLLDETAIL:
                    mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.DELETE_SHOOTING_LIVEVIEW_SCROLLDETEIL, null, webAPIResultListener));
                    break;
                default:
                    break;
            }
        }
        else{
            callback.onComplete();
        }
    }

    private void notifyThread(){
        synchronized (this) {
            this.notifyAll();
        }
    }

    private void flipLiveView(Boolean isOrg){
        if(isOrg){
            Bundle args = new Bundle();

            switch (mKind){
                case INFO:
                    args.putString(Constants.RequestCode.GET_SHOOTING_LIVEVIEW_FLIPDETAIL.name(), "info");
                    break;
                case IMAGE_AND_INFO:
                    args.putString(Constants.RequestCode.GET_SHOOTING_LIVEVIEW_FLIPDETAIL.name(), "both");
                    break;
                case IMAGE:
                default:
                    args.putString(Constants.RequestCode.GET_SHOOTING_LIVEVIEW_FLIPDETAIL.name(), "image");
                    break;
            }

            mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.GET_SHOOTING_LIVEVIEW_FLIPDETAIL, args, true, new WebAPIResultListener() {
                @Override
                public void onWebAPIResult(WebAPIResultDataSet result) {
                    Log.d(TAG, "flipLiveView(org) onWebAPIResult begin.");
                    if (result.isError()) {
                        Log.d(TAG, "flipLiveView(org) onWebAPIResult Error.");
                    }
                    else {
                        if(result.getBytesResponseBody() != null) {
                            OrgFormatDataSet orgFormatDataSet = new OrgFormatDataSet(result.getBytesResponseBody());
                            parseOrgFormatData(orgFormatDataSet);
                        }
                    }
                    notifyThread();
                    Log.d(TAG, "flipLiveView(org) onWebAPIResult end.");
                }
            }));
        }
        else{
            mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.GET_SHOOTING_LIVEVIEW_FLIP, null, true, new WebAPIResultListener() {
                @Override
                public void onWebAPIResult(WebAPIResultDataSet result) {
                    Log.d(TAG, "flipLiveView onWebAPIResult begin.");
                    if (result.isError()) {
                        Log.d(TAG, "flipLiveView onWebAPIResult Error.");
                    }
                    else {
                        Bitmap bitmap = result.getImageResponseBody();
                        if (bitmap != null) {
                            updateLiveView(bitmap);
                        }
                    }
                    notifyThread();
                    Log.d(TAG, "flipLiveView onWebAPIResult end.");
                }
            }));
        }
    }

    private void scrollLiveView(Boolean isOrg){
        if(isOrg){
            Bundle args = new Bundle();

            switch (mKind) {
                case IMAGE_AND_INFO:
                    args.putString(Constants.RequestCode.GET_SHOOTING_LIVEVIEW_SCROLLDETAIL.name(), "both");
                    break;
                case INFO:
                    args.putString(Constants.RequestCode.GET_SHOOTING_LIVEVIEW_SCROLLDETAIL.name(), "info");
                    break;
                case IMAGE:
                default:
                    args.putString(Constants.RequestCode.GET_SHOOTING_LIVEVIEW_SCROLLDETAIL.name(), "image");
                    break;
            }

            mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.GET_SHOOTING_LIVEVIEW_SCROLLDETAIL, args, true, new ChunkResultListener() {
                @Override
                public boolean onChunkResult(byte[] bytes) {
                    // Send data in units of chunk in the following order.
                    // 1 : Start Byte/Data Type/Data Size (7 byte)
                    // 2 : Data
                    // 3 : End Byte (2 byte)
                    mChunkByteArrayOutputStream.write(bytes, 0, bytes.length);
                    int chunkLength = mChunkByteArrayOutputStream.size();

                    if(mChunkDataSize == 0 && chunkLength >= 7) {
                        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
                        byteBuffer.order(ByteOrder.BIG_ENDIAN);
                        byteBuffer.getShort();  // Start Byte (2bytes)
                        byteBuffer.get();       // Data Type (1byte)
                        mChunkDataSize = byteBuffer.getInt(); // Data Size (4bytes)
                    }
                    else if(mChunkDataSize != 0 && chunkLength >= 7 + mChunkDataSize + 2) {
                        OrgFormatDataSet orgFormatDataSet = new OrgFormatDataSet(mChunkByteArrayOutputStream.toByteArray());
                        parseOrgFormatData(orgFormatDataSet);
                        try {
                            mChunkByteArrayOutputStream.close();
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                        mChunkByteArrayOutputStream = new ByteArrayOutputStream();
                        mChunkDataSize = 0;
                    }
                    return isAlive();
                }
            }, new WebAPIResultListener() {
                @Override
                public void onWebAPIResult(WebAPIResultDataSet result) {
                    Log.d(TAG, "scrollLiveView(org) onWebAPIResult. " + mKind.toString());
                    if (result.isError()) {
                        Log.d(TAG, "scrollLiveView(org) onWebAPIResult Error.");
                    }
                    notifyThread();
                }
            }));
        }
        else{
            mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.GET_SHOOTING_LIVEVIEW_SCROLL, null, true, new ChunkResultListener() {
                @Override
                public boolean onChunkResult(byte[] bytes) {
                    if (bytes == null) {
                        Log.d(TAG, "scrollLiveView onChunkResult Error.");
                    }
                    else {
                        BitmapFactory.Options options = new  BitmapFactory.Options();
                        options.inMutable = true;
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                        if (bitmap != null) {
                            updateLiveView(bitmap);
                        }
                    }

                    return isAlive();
                }
            }, new WebAPIResultListener() {
                @Override
                public void onWebAPIResult(WebAPIResultDataSet result) {
                    Log.d(TAG, "scrollLiveView onWebAPIResult.");
                    if (result.isError()) {
                        Log.d(TAG, "scrollLiveView onWebAPIResult Error.");
                    }
                    notifyThread();
                }
            }));
        }
    }

    private void updateLiveView(Bitmap bitmap){
        Log.d(TAG, "updateLiveView called.");

        if(bitmap.getWidth() == 160 && bitmap.getHeight() == 120) {
            // Do not draw 160 x 120 black image on the application screen.
            Log.d(TAG, "updateLiveView : Black image");
        }
        else{
            final ImageView imageView = mImageViewReference.get();
            if (imageView != null) {
                final Bitmap liveViewBitmap = createLiveViewBitmap(bitmap);
                final TextView lvSizeText = mLvSizeTextViewReference.get();

                mHandler.post(new Runnable() {
                    public void run() {
                        if (mBitmap != null) {
                            imageView.setImageDrawable(null);
                            imageView.setImageBitmap(null);
                            mBitmap.recycle();
                            mBitmap = null;
                        }
                        mBitmap = liveViewBitmap;
                        imageView.setImageBitmap(mBitmap);
                        Log.d(TAG, "updateLiveView setImageBitmap");

                        if (lvSizeText != null) {
                            try {
                                String width = String.valueOf(mBitmap.getWidth());
                                String height = String.valueOf(mBitmap.getHeight());
                                lvSizeText.setText(width + " x " + height);
                            }
                            catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }

                        calcFPS();
                    }
                });
            }
            else {
                interrupt();
                Log.d(TAG, "updateLiveView ImageView not enabled.");
            }
        }
    }

    private Bitmap createLiveViewBitmap(Bitmap bitmap){
        // Determine whether incidental information has been got.
        if(mAfFrameArray != null && mLvImageInfoMap.size() != 0 && mLvVisibleInfoMap.size() != 0 && mLvZoomInfoMap.size() != 0){
            if(mLvZoomInfoMap.get(MAGNIFICATION) == 0x01) {
                bitmap = createVisiblePosition(bitmap);
                bitmap = createAfFrame(bitmap);
            }
        }
        return bitmap;
    }

    private Bitmap createVisiblePosition(Bitmap bitmap){

        // Paint the outside of visible range with black.
        int posX = mLvVisibleInfoMap.get(POSITION_X) - mLvImageInfoMap.get(POSITION_X);
        int posY = mLvVisibleInfoMap.get(POSITION_Y) - mLvImageInfoMap.get(POSITION_Y);
        float posWidth = mLvVisibleInfoMap.get(POSITION_WIDTH);
        float posHeight = mLvVisibleInfoMap.get(POSITION_HEIGHT);
        float widthScale = mLvImageInfoMap.get(POSITION_WIDTH) / (float) bitmap.getWidth();
        float heightScale = mLvImageInfoMap.get(POSITION_HEIGHT) / (float) bitmap.getHeight();
        int x = Math.round( (float) posX / widthScale );
        int y = Math.round( (float) posY / heightScale );
        int width = Math.round( posWidth / widthScale );
        int height = Math.round( posHeight / heightScale );

        Paint paint = new Paint();
        Canvas canvas = new Canvas(bitmap);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setColor(Color.BLACK);

        canvas.drawRect(0, 0, bitmap.getWidth(), y, paint);
        canvas.drawRect(0, y + height, bitmap.getWidth(), bitmap.getHeight(), paint);

        canvas.drawRect(0, 0, x, bitmap.getHeight(), paint);
        canvas.drawRect(x + width, 0, bitmap.getWidth(), bitmap.getHeight(), paint);

        return bitmap;
    }

    private Bitmap createAfFrame(Bitmap bitmap){
        Paint paint = new Paint();
        Canvas canvas = new Canvas(bitmap);
        int mainColor = 0;
        int subColor = 0;
        paint.setStrokeWidth(2);
        paint.setStyle(Paint.Style.STROKE);

        try {
            List<JSONObject>  afFrameList = new ArrayList<>();
            for (int i = 0; i < mAfFrameArray.length(); i++) {
                JSONObject frame = mAfFrameArray.getJSONObject(i);

                if (frame.getInt(SELECT) != 0x02) {
                    switch (frame.getInt(STATUS) & 0x0F) {
                        case 0x00:
                            afFrameList.add(0, frame);
                            break;
                        default:
                            afFrameList.add(frame);
                            break;
                    }
                }
            }

            // The processing of the AF frame drawing.
            for (JSONObject frame: afFrameList){

                switch(frame.getInt(SELECT)) {
                    case 0x00:
                        mainColor = Color.GRAY;
                        subColor = Color.BLACK;
                        break;
                    case 0x01:
                        mainColor = Color.WHITE;
                        subColor = Color.GRAY;
                        break;
                    default:
                        break;
                }

                switch(frame.getInt(STATUS) & 0x0F){
                    case 0x01:
                        mainColor = Color.GREEN;
                        break;
                    case 0x02:
                        mainColor = Color.YELLOW;
                        break;
                    case 0x04:
                        mainColor = 0xFF00AFFF;
                        break;
                    default:
                        break;
                }

                int posX = mLvImageInfoMap.get(POSITION_X);
                int posY = mLvImageInfoMap.get(POSITION_Y);
                float widthScale = mLvImageInfoMap.get(POSITION_WIDTH) / (float) bitmap.getWidth();
                float heightScale = mLvImageInfoMap.get(POSITION_HEIGHT) / (float) bitmap.getHeight();
                int x = Math.round((float) (frame.getInt(X) - posX) / widthScale );
                int y = Math.round((float) (frame.getInt(Y) - posY) / heightScale );
                int width = Math.round((float) frame.getInt(WIDTH) / widthScale );
                int height = Math.round((float) frame.getInt(HEIGHT) / heightScale );

                switch (frame.getInt(STATUS) & 0xF0){
                    case 0x20: {

                        int widthLen = width / 4;
                        int heightLen = height / 4;

                        paint.setColor(subColor);

                        canvas.drawLine(x + 1, y + 1, x + 1 + widthLen, y + 1, paint);
                        canvas.drawLine(x + 1, y + 1, x + 1, y + 1 + heightLen, paint);

                        canvas.drawLine(x + width + 1, y + 1, x + width + 1 - widthLen, y + 1, paint);
                        canvas.drawLine(x + width + 1, y + 1, x + width + 1, y + 1 + heightLen, paint);

                        canvas.drawLine(x + 1, y + height + 1, x + 1 + widthLen, y + height + 1, paint);
                        canvas.drawLine(x + 1, y + height + 1, x + 1, y + height + 1 - heightLen, paint);

                        canvas.drawLine(x + width + 1, y + height + 1, x + width + 1 - widthLen, y + height + 1, paint);
                        canvas.drawLine(x + width + 1, y + height + 1, x + width + 1, y + height + 1 - heightLen, paint);

                        paint.setColor(mainColor);

                        canvas.drawLine(x, y, x + widthLen, y, paint);
                        canvas.drawLine(x, y, x, y + heightLen, paint);

                        canvas.drawLine(x + width, y, x + width - widthLen, y, paint);
                        canvas.drawLine(x + width, y, x + width, y + heightLen, paint);

                        canvas.drawLine(x, y + height, x + widthLen, y + height, paint);
                        canvas.drawLine(x, y + height, x, y + height - heightLen, paint);

                        canvas.drawLine(x + width, y + height, x + width - widthLen, y + height, paint);
                        canvas.drawLine(x + width, y + height, x + width, y + height - heightLen, paint);
                        break;
                    }
                    case 0x30: {

                        paint.setStrokeWidth(1);
                        int widthLen = width / 4;
                        int heightLen = height / 4;

                        paint.setColor(subColor);

                        canvas.drawLine(x, y, x + widthLen, y, paint);
                        canvas.drawLine(x, y, x, y + heightLen, paint);

                        canvas.drawLine(x + width, y, (x + width) - widthLen, y, paint);
                        canvas.drawLine(x + width, y, x + width, y + heightLen, paint);

                        canvas.drawLine(x, y + height, x + widthLen, y + height, paint);
                        canvas.drawLine(x, y + height, x, (y + height) - heightLen, paint);

                        canvas.drawLine(x + width, y + height, (x + width) - widthLen, y + height, paint);
                        canvas.drawLine(x + width, y + height, x + width, (y + height) - heightLen, paint);

                        canvas.drawLine(x + 2, y + 2, (x + 2) + (widthLen - 2), y + 2, paint);
                        canvas.drawLine(x + 2, y + 2, x + 2, (y + 2) + (heightLen - 2), paint);

                        canvas.drawLine(x + width - 2, y + 2, (x + width - 2) - (widthLen - 2), y + 2, paint);
                        canvas.drawLine(x + width - 2, y + 2, x + width - 2, (y + 2) + (heightLen - 2), paint);

                        canvas.drawLine(x + 2, y + height - 2, (x + 2) + (widthLen - 2), y + height - 2, paint);
                        canvas.drawLine(x + 2, y + height - 2, x + 2, (y + height - 2) - (heightLen - 2), paint);

                        canvas.drawLine(x + width - 2, y + height - 2, (x + width - 2) - (widthLen - 2), y + height - 2, paint);
                        canvas.drawLine(x + width - 2, y + height - 2, x + width - 2, (y + height - 2) - (heightLen - 2), paint);

                        paint.setColor(mainColor);

                        canvas.drawLine(x - 1, y - 1, (x - 1) + (widthLen + 1), y - 1, paint);
                        canvas.drawLine(x - 1, y - 1, x - 1, (y - 1) + (heightLen + 1), paint);

                        canvas.drawLine(x + width + 1, y - 1, (x + width + 1) - (widthLen + 1), y - 1, paint);
                        canvas.drawLine(x + width + 1, y - 1, x + width + 1, (y - 1) + (heightLen + 1), paint);

                        canvas.drawLine(x - 1, y + height + 1, (x - 1) + (widthLen + 1), y + height + 1, paint);
                        canvas.drawLine(x - 1, y + height + 1, x - 1, (y + height + 1) - (heightLen + 1), paint);

                        canvas.drawLine(x + width + 1, y + height + 1, (x + width + 1) - (widthLen + 1), y + height + 1, paint);
                        canvas.drawLine(x + width + 1, y + height + 1, x + width + 1, (y + height + 1) - (heightLen + 1), paint);

                        canvas.drawLine(x + 1, y + 1, (x + 1) + (widthLen - 1), y + 1, paint);
                        canvas.drawLine(x + 1, y + 1, x + 1, (y + 1) + (heightLen - 1), paint);

                        canvas.drawLine(x + width - 1, y + 1, (x + width - 1) - (widthLen - 1), y + 1, paint);
                        canvas.drawLine(x + width - 1, y + 1, x + width - 1, (y + 1) + (heightLen - 1), paint);

                        canvas.drawLine(x + 1, y + height - 1, (x + 1) + (widthLen - 1), y + height - 1, paint);
                        canvas.drawLine(x + 1, y + height - 1, x + 1, (y + height - 1) - (heightLen - 1), paint);

                        canvas.drawLine(x + width - 1, y + height - 1, (x + width - 1) - (widthLen - 1), y + height - 1, paint);
                        canvas.drawLine(x + width - 1, y + height - 1, x + width - 1, (y + height - 1) - (heightLen - 1), paint);
                        break;
                    }
                    default: {

                        paint.setColor(subColor);
                        canvas.drawRect(x + 1, y + 1, x + width + 1, y + height + 1, paint);

                        paint.setColor(mainColor);
                        canvas.drawRect(x, y, x + width, y + height, paint);
                        break;
                    }
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    private Bitmap createHistogram(JSONArray histogramArray){
        List<Integer> colorList = Arrays.asList(Color.WHITE, Color.RED, Color.GREEN, Color.CYAN);
        Matrix matrix = new Matrix();
        Bitmap bitmap = Bitmap.createBitmap( HISTOGRAM_VIEW_WIDTH * 4, HISTOGRAM_VIEW_HEIGHT, Bitmap.Config.RGB_565 );
        Paint paint = new Paint();
        Canvas canvas = new Canvas(bitmap);

        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setColor(Color.BLACK);
        canvas.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);

        for (int i = 0; i < histogramArray.length(); i++) {

            paint.setColor(Color.GRAY);
            canvas.drawRect(HISTOGRAM_VIEW_WIDTH * i, 0, HISTOGRAM_VIEW_WIDTH * i + HISTOGRAM_LENGTH, HISTOGRAM_VIEW_HEIGHT, paint);

            Integer color = Color.WHITE;
            if(colorList.size() > i){
                color = colorList.get(i);
            }

            try {
                JSONArray valueArray = histogramArray.getJSONArray(i);
                for (int j = 0; j < valueArray.length(); j++) {
                    int value = valueArray.getInt(j) / HISTOGRAM_SCALE;
                    if(value > HISTOGRAM_LIMIT){
                        value = HISTOGRAM_LIMIT;
                    }
                    paint.setColor(color);
                    canvas.drawRect(HISTOGRAM_VIEW_WIDTH * i + j + 1, 1, HISTOGRAM_VIEW_WIDTH * i + j + 1, value + 1, paint);
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // Flip upside down.
        matrix.preScale(1, -1);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
        return bitmap;
    }

    private void updateLiveViewInfoList(final String info){
        final ExpandableListView infoListView = mInfoListViewReference.get();
        if (infoListView != null) {
            try {
                JSONObject lvInfoObject = new JSONObject(info);

                if(lvInfoObject.length() != 0) {
                    Iterator<String> parentIterator = lvInfoObject.keys();

                    while (parentIterator.hasNext()) {
                        final String parentKey = parentIterator.next();
                        Object param = lvInfoObject.get(parentKey);

                        if (param instanceof JSONObject) {
                            TreeMap<String,String> treeMap = new TreeMap<>();
                            JSONObject jsonObject = (JSONObject) param;
                            Iterator<String> childIterator = jsonObject.keys();
                            int index = 0;

                            if (!mListViewIndexMap.containsKey(parentKey)) {

                                mAdapterParentList.add(new HashMap<String, String>() {
                                    { put(LIST_VIEW_KEY_GROUP, parentKey); }
                                });

                                mAdapterChildList.add(new ArrayList<Map<String, String>>());
                                mListViewIndexMap.put(parentKey, mListViewIndexMap.size());
                            }

                            index = mListViewIndexMap.get(parentKey);

                            List<Map<String, String>> childMapList = new ArrayList<>();

                            while (childIterator.hasNext()) {
                                String childKey = childIterator.next();
                                Object childValue = jsonObject.get(childKey);

                                if (childValue instanceof JSONObject) {
                                    Iterator<String> grandchildIterator = ((JSONObject)childValue).keys();

                                    while (grandchildIterator.hasNext()) {
                                        String grandchildKey = grandchildIterator.next();
                                        Object grandchildValue = ((JSONObject)childValue).get(grandchildKey);

                                        switch (childKey) {
                                            case IMAGE:
                                                mLvImageInfoMap.put(grandchildKey, (Integer) grandchildValue);
                                                break;
                                            case VISIBLE:
                                                mLvVisibleInfoMap.put(grandchildKey, (Integer) grandchildValue);
                                                break;
                                            case ZOOM:
                                                mLvZoomInfoMap.put(grandchildKey, (Integer) grandchildValue);
                                                break;
                                            default:
                                                break;
                                        }
                                        treeMap.put(childKey + "/" + grandchildKey, String.valueOf(grandchildValue));
                                    }
                                }
                                else if(childValue instanceof JSONArray){
                                    switch (childKey) {
                                        case HISTOGRAM:
                                            final ImageView imageView = mHistogramReference.get();
                                            if (imageView != null) {
                                                final Bitmap bitmap = createHistogram((JSONArray) childValue);
                                                mHandler.post(new Runnable() {
                                                    public void run() {
                                                        imageView.setImageBitmap(bitmap);
                                                    }
                                                });
                                            }
                                            break;
                                        case AF_FRAME:
                                            // Coordinate position of the AF frame.
                                            mAfFrameArray = (JSONArray) childValue;
                                            int afCount = 0;

                                            // Display the number of effective frameworks.
                                            for (int i = 0; i < mAfFrameArray.length(); i++) {
                                                JSONObject frame = mAfFrameArray.getJSONObject(i);
                                                if (frame.getInt(SELECT) != 0x02) {
                                                    afCount++;
                                                }
                                            }

                                            treeMap.put(childKey, String.valueOf(afCount));
                                            break;
                                        default:
                                            break;
                                    }
                                }
                                else{
                                    treeMap.put(childKey, String.valueOf(childValue));
                                }
                            }

                            for (String name : treeMap.keySet()) {
                                String value = treeMap.get(name);
                                Map<String, String> childMap = new HashMap<>();
                                childMap.put(LIST_VIEW_KEY_NAME, name);
                                childMap.put(LIST_VIEW_KEY_VALUE, value);
                                childMapList.add(childMap);
                            }
                            mAdapterChildList.set(index, childMapList);
                        }
                        else {
                            Log.d(TAG, String.format("%s : Unknown Format.", parentKey));
                        }
                    }
                }
                else{
                    clearLvInfo();
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mAdapter.notifyDataSetChanged();
                }
            });
        }
        else {
            interrupt();
            Log.d(TAG, "updateLiveViewInfoList ListView not enabled.");
        }
    }

    private void parseOrgFormatData(OrgFormatDataSet orgFormatDataSet){
        if ( !orgFormatDataSet.isUnknownFormat() ) {
            byte[] imageData = orgFormatDataSet.getLvImageData();
            byte[] infoData = orgFormatDataSet.getLvInfoData();

            if (infoData != null) {
                Log.d(TAG, "parseOrgFormatData(info)");
                try {
                    String data = new String(infoData, "UTF-8");
                    updateLiveViewInfoList(data);
                }
                catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            if (imageData != null) {
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageData);
                BitmapFactory.Options options = new  BitmapFactory.Options();
                options.inMutable = true;
                Bitmap bitmap = BitmapFactory.decodeStream(byteArrayInputStream, null, options);
                if (bitmap != null) {
                    Log.d(TAG, String.format("parseOrgFormatData(image) : w(%d) x h(%d)", bitmap.getWidth(), bitmap.getHeight()));
                    updateLiveView(bitmap);
                }
            }
        }
        else {
            Log.d(TAG, "parseOrgFormatData Error.");
        }
    }

    private void clearLvInfo(){
        mAfFrameArray = null;
        mLvImageInfoMap.clear();
        mLvVisibleInfoMap.clear();
        mLvZoomInfoMap.clear();
    }

    private long calcFPS(){
        long frameRate = -1;
        long now = System.currentTimeMillis();
        mFrameCount++;

        if(mStartTime != 0){
            mElapsedTime += now - mStartTime;

            if(mElapsedTime > 1000){
                frameRate = (mFrameCount / (mElapsedTime / 1000));
                mElapsedTime = 0;
                mFrameCount = 0;
                Log.d(TAG, "FPS : " + String.valueOf(frameRate));
            }
        }

        mStartTime = now;
        return frameRate;
    }

    int getImagePositionX(){
        int x = 0;
        if(mLvImageInfoMap.containsKey(POSITION_X)){
            x = mLvImageInfoMap.get(POSITION_X);
        }
        return x;
    }

    int getImagePositionY(){
        int y = 0;
        if(mLvImageInfoMap.containsKey(POSITION_Y)){
            y = mLvImageInfoMap.get(POSITION_Y);
        }
        return y;
    }

    int getImagePositionWidth(){
        int x = 0;
        if(mLvImageInfoMap.containsKey(POSITION_WIDTH)){
            x = mLvImageInfoMap.get(POSITION_WIDTH);
        }
        return x;
    }

    int getImagePositionHeight(){
        int y = 0;
        if(mLvImageInfoMap.containsKey(POSITION_HEIGHT)){
            y = mLvImageInfoMap.get(POSITION_HEIGHT);
        }
        return y;
    }
}
