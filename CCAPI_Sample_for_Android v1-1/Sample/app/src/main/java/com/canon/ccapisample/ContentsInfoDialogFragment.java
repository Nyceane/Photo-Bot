package com.canon.ccapisample;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.canon.ccapisample.Constants.CCAPI.Method.GET;
import static com.canon.ccapisample.Constants.CCAPI.UNIT_MAP;

public class ContentsInfoDialogFragment extends DialogFragment implements WebAPIResultListener{
    enum ShowType{
        INFO,
    }

    private static final String TAG = ContentsInfoDialogFragment.class.getSimpleName();
    private static final String SHOW_TYPE = "ShowType";
    private static final String CONTENTS_DATA_SET = "ContentsDataSet";
    private static final String LIST_VIEW_KEY_NAME = "name";
    private static final String LIST_VIEW_KEY_VALUE = "value";
    private SimpleAdapter mAdapter;
    private List<Map<String, String>> mAdapterDataList = new ArrayList<>();

    public static ContentsInfoDialogFragment newInstance(Fragment target, ShowType type, ContentsDataSet contentsDataSet){
        ContentsInfoDialogFragment instance = new ContentsInfoDialogFragment();
        instance.setTargetFragment(target, 0);
        Bundle arguments = new Bundle();
        arguments.putSerializable(SHOW_TYPE, type);
        arguments.putSerializable(CONTENTS_DATA_SET, contentsDataSet);
        instance.setArguments(arguments);
        return instance;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle arguments) {
        Log.d(TAG, "onCreateDialog");

        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());

        if (getArguments() != null) {
            ShowType showType = (ShowType) getArguments().getSerializable(SHOW_TYPE);
            ContentsDataSet contentsDataSet = (ContentsDataSet) getArguments().getSerializable(CONTENTS_DATA_SET);

            if(showType != null && contentsDataSet != null) {
                dialog.setTitle(contentsDataSet.getName());

                LinearLayout parentLayout = new LinearLayout(getActivity());
                parentLayout.setOrientation(LinearLayout.VERTICAL);
                parentLayout.setPadding(10, 10, 10, 10);

                ListView listView = new ListView(getActivity());
                mAdapter = new CustomSimpleAdapter(
                        getActivity(),
                        mAdapterDataList,
                        R.layout.list_view_small_item_layout,
                        new String[]{LIST_VIEW_KEY_NAME, LIST_VIEW_KEY_VALUE},
                        new int[]{android.R.id.text1, android.R.id.text2},
                        null,
                        null);
                listView.setAdapter(mAdapter);
                parentLayout.addView(listView);

                dialog.setView(parentLayout);

                if(showType == ShowType.INFO) {
                    Bundle args = new Bundle();
                    String[] params = new String[]{GET, contentsDataSet.getUrl() + "?kind=info", null};
                    args.putStringArray(Constants.RequestCode.ACT_WEB_API.name(), params);
                    WebAPI.getInstance().enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.ACT_WEB_API, args, this));
                }
            }
        }
        return dialog.create();
    }

    @Override
    public void onWebAPIResult(WebAPIResultDataSet result) {
        Log.d(TAG, String.format("[DEBUG] %s onWebAPIResult", String.valueOf(result.getRequestCode())));
        Context context = getActivity();
        if(context != null) {
            if (result.isError()) {
                Toast.makeText(context, result.getErrorMsg(), Toast.LENGTH_SHORT).show();
            }
            else {
                switch (result.getRequestCode()) {
                    case ACT_WEB_API:
                        Map<String, String> query = result.getQueryMap();
                        if (query != null && query.containsKey("kind") && query.get("kind").equals("info")) {
                            createFileInfo(result.getResponseBody());
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void createFileInfo(String result){
        try {
            JSONObject response = new JSONObject(result);
            Iterator<String> iterator = response.keys();

            while (iterator.hasNext()) {
                StringBuilder stringBuilder = new StringBuilder();
                Map<String, String> map = new HashMap<>();
                String key = iterator.next();
                Object param = response.get(key);

                map.put(LIST_VIEW_KEY_NAME, key);

                if(!String.valueOf(param).equals("null")) {
                    stringBuilder.append(String.valueOf(param));
                    if (UNIT_MAP.containsKey(key)) {
                        stringBuilder.append(" ").append(UNIT_MAP.get(key));
                    }
                }

                map.put(LIST_VIEW_KEY_VALUE, stringBuilder.toString());

                if(map.size() != 0) {
                    mAdapterDataList.add(map);
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        mAdapter.notifyDataSetChanged();
    }
}
