package com.canon.ccapisample;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.HttpAuthHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.canon.ccapisample.Constants.CCAPI.Field.STATUS;
import static com.canon.ccapisample.Constants.CCAPI.Field.RECBUTTON;
import static com.canon.ccapisample.Constants.CCAPI.Method.GET;

public class ContentsShowFragment extends Fragment implements EventListener{
    private static final String TAG = ContentsShowFragment.class.getSimpleName();
    private static final String STORAGE_CONTENTS_DATA_SET = "ContentsDataSet";
    private static final String QUERY = "Query";
    private ContentsDataSet mContentsDataSet;
    private String mQuery = null;
    private WebAPI mWebAPI;
    private WebView mWebView = null;
    private ProgressBar mProgressBar = null;
    private Point mDisplaySize = new Point();

    public ContentsShowFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ContentsShowFragment.
     */
    public static ContentsShowFragment newInstance(ContentsDataSet contentsDataSet, String query) {
        ContentsShowFragment fragment = new ContentsShowFragment();
        Bundle arguments = new Bundle();
        arguments.putSerializable(STORAGE_CONTENTS_DATA_SET, contentsDataSet);
        arguments.putString(QUERY, query);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWebAPI = WebAPI.getInstance();

        // Hand over a digest authentication header to the WebView.
        // And pause the WebAPI thread to prevent mismatch of the nonce count.
        mWebAPI.waitThread();

        if (getArguments() != null) {
            mContentsDataSet = (ContentsDataSet) getArguments().getSerializable(STORAGE_CONTENTS_DATA_SET);
            mQuery = getArguments().getString(QUERY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        Display display = ( (WindowManager)getContext().getSystemService( Context.WINDOW_SERVICE ) ).getDefaultDisplay();
        display.getSize(mDisplaySize);

        View view = inflater.inflate(R.layout.fragment_contents_viewer_image, container, false);

        if(mContentsDataSet.getKind() == ContentsDataSet.Kind.IMAGE || (mQuery != null)) {
            view = createViewForImage(mQuery);
        }

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        // Cancel the loading of images.
        if(mWebView != null){
            mWebView.stopLoading();
        }

        // Restart the WebAPI thread.
        mWebAPI.notifyThread();
    }

    private View createViewForImage(String query){
        String url;
        String authHeader;
        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_contents_viewer_image, null);

        if (query == null) {
            url = mContentsDataSet.getUrl();
        }
        else {
            url = mContentsDataSet.getUrl() + "?" + query;
        }

        mProgressBar = view.findViewById(R.id.StillProgressBar);
        mWebView = view.findViewById(R.id.StillWebView);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);

        // Update the progress bar.
        mWebView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                mProgressBar.setProgress(progress);
            }
        });

        // Control of the view display at starting/ending of the loading.
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.d(TAG, "onPageStarted");
                mProgressBar.setVisibility(View.VISIBLE);
                mWebView.setVisibility(View.GONE);
                mWebAPI.notifyThread();
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "onPageFinished");
                mProgressBar.setVisibility(View.GONE);
                mWebView.setVisibility(View.VISIBLE);
            }
            @Override
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
                Log.d(TAG, "onReceivedHttpAuthRequest");
                String[] strings = mWebAPI.getDigestAuthInfo();
                if(strings != null) {
                    handler.proceed(strings[0], strings[1]);
                }
            }
        });

        // Generate a authorization header.
        authHeader = mWebAPI.getDigestAuthHeader(GET, mContentsDataSet.getUrl(), null);
        if(authHeader != null) {
            Map<String, String> headerMap = new HashMap<>();
            headerMap.put("Authorization", authHeader);
            mWebView.loadUrl(url, headerMap);
        }
        else {
            mWebView.loadUrl(url);
        }

        Log.d(TAG, url);

        return view;
    }

    @Override
    public void onNotifyEvent(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            if(jsonObject.length() != 0) {
                Iterator<String> iterator = jsonObject.keys();
                while(iterator.hasNext()) {
                    String key = iterator.next();
                    String param = jsonObject.getString(key);
                    Log.d(TAG, String.format("onNotifyEvent : %s : %s", key, param));

                    switch (key) {
                        case RECBUTTON:
                            JSONObject rec = new JSONObject(param);

                            if(!rec.isNull(STATUS)) {
                                if (rec.getString(STATUS).equals("start")) {
                                    mWebAPI.cancelAllSerialRequest();

                                    // Return to the MainActivity's display by the DisconnectListener.
                                    Activity activity = getActivity();
                                    if (activity != null && activity instanceof DisconnectListener) {
                                        ((DisconnectListener) activity).onNotifyDisconnect("Rec started.", false);
                                    }
                                }
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
