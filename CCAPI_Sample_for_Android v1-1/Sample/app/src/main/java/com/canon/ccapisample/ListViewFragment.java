package com.canon.ccapisample;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.canon.ccapisample.Constants.CCAPI.Field.CARD_FORMAT;
import static com.canon.ccapisample.Constants.CCAPI.Field.DATETIME;
import static com.canon.ccapisample.Constants.CCAPI.Field.EXPOSURE_INCREMENTS;
import static com.canon.ccapisample.Constants.CCAPI.Field.MESSAGE;
import static com.canon.ccapisample.Constants.CCAPI.Field.STORAGE;
import static com.canon.ccapisample.Constants.CCAPI.Field.STORAGE_LIST;
import static com.canon.ccapisample.Constants.CCAPI.Field.STORAGE_NAME;
import static com.canon.ccapisample.Constants.CCAPI.Field.WIFI_SETTINGS;
import static com.canon.ccapisample.Constants.CCAPI.Field.WIFI_SETTINGS_SET_1;
import static com.canon.ccapisample.Constants.CCAPI.Field.WIFI_SETTINGS_SET_2;
import static com.canon.ccapisample.Constants.CCAPI.Field.WIFI_SETTINGS_SET_3;
import static com.canon.ccapisample.Constants.CCAPI.Key.DEVICESTATUS_STORAGE;
import static com.canon.ccapisample.Constants.CCAPI.Method.DELETE;
import static com.canon.ccapisample.Constants.CCAPI.Method.GET;
import static com.canon.ccapisample.Constants.CCAPI.Method.PUT;
import static com.canon.ccapisample.Constants.CCAPI.Value.MODE_NOT_SUPPORTED;
import static com.canon.ccapisample.Constants.Settings.Key.KEY;
import static com.canon.ccapisample.Constants.Settings.Key.NAME;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ListViewFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ListViewFragment extends Fragment implements WebAPIResultListener, EventListener, ExpandableListView.OnChildClickListener, ExpandableListView.OnGroupClickListener {
    private static final String TAG = ListViewFragment.class.getSimpleName();
    private static final String SETTINGS_INFO = "SettingsInfo";
    private static final String LIST_VIEW_KEY_GROUP = "group";
    private static final String LIST_VIEW_KEY_NAME = "name";
    private static final String LIST_VIEW_KEY_VALUE = "value";
    private WebAPI mWebAPI;
    private JSONArray mSettingsInfo;
    private List<ListViewDataSet> mSettingsDataSetList = new ArrayList<>();
    private SimpleExpandableListAdapter mAdapter;
    private ExpandableListView mExpandableListView;
    private List<Map<String, String>> mParentList = new ArrayList<>();
    private List<List<Map<String, String>>> mChildList = new ArrayList<>();
    private List<Boolean> mEditableList = new ArrayList<>();
    private ProgressDialogFragment mProgressDialog = null;

    public ListViewFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param settings Parameter 1.
     * @return A new instance of fragment ListViewFragment.
     */
    public static ListViewFragment newInstance(String settings) {
        ListViewFragment fragment = new ListViewFragment();
        Bundle args = new Bundle();
        args.putString(SETTINGS_INFO, settings);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWebAPI = WebAPI.getInstance();

        try {
            mSettingsInfo = new JSONArray(getArguments().getString(SETTINGS_INFO));
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        View view = inflater.inflate(R.layout.fragment_list_view, container, false);

        mSettingsDataSetList.clear();
        mParentList.clear();
        mChildList.clear();
        mEditableList.clear();

        // Set the List View.
        mExpandableListView = view.findViewById(R.id.ListView);
        mAdapter = new CustomSimpleExpandableListAdapter(
                getActivity(),
                mParentList,
                R.layout.list_view_parent_item_layout,
                new String[] {LIST_VIEW_KEY_GROUP},
                new int[] { R.id.ParentItemText },
                mChildList,
                R.layout.list_view_child_item_layout,
                new String[] {LIST_VIEW_KEY_NAME, LIST_VIEW_KEY_VALUE },
                new int[] { android.R.id.text1, android.R.id.text2 },
                mEditableList
        );
        mExpandableListView.setAdapter(mAdapter);
        mExpandableListView.setGroupIndicator(null);
        mExpandableListView.setOnGroupClickListener(this);
        mExpandableListView.setOnChildClickListener(this);

        getListViewSettings();

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState");
    }

    @Override
    public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        return true;
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        Log.d(TAG, String.format("onChildClick groupPosition[%d], childPosition[%d], id[%d]", groupPosition, childPosition, id));

        ExpandableListAdapter adapter = parent.getExpandableListAdapter();
        Map<String, String> titleMap = (Map<String, String>)adapter.getGroup(groupPosition);
        Map<String, String> itemsMap = (Map<String, String>)adapter.getChild(groupPosition, childPosition);
        String name = "";
        Boolean isEditable = mEditableList.get(groupPosition);

        if (titleMap.get(LIST_VIEW_KEY_GROUP).isEmpty()){
            // Get a name from the item name, if there is no title.
            name = itemsMap.get(LIST_VIEW_KEY_NAME);
        }
        else{
            name = titleMap.get(LIST_VIEW_KEY_GROUP);
        }

        JSONObject settings = null;
        ListViewDataSet settingsDataSet = null;

        for(ListViewDataSet listViewDataSet : mSettingsDataSetList){
            if (name.equals(listViewDataSet.getName())) {
                settingsDataSet = listViewDataSet;
                break;
            }
        }

        if( settingsDataSet != null && isEditable ) {
            switch (name) {
                case WIFI_SETTINGS_SET_1:
                case WIFI_SETTINGS_SET_2:
                case WIFI_SETTINGS_SET_3:{
                    try {
                        for (int i = 0; i < mSettingsInfo.length(); i++) {
                            if (name.equals(mSettingsInfo.getJSONObject(i).getString(NAME))) {
                                settings = mSettingsInfo.getJSONObject(i);
                                break;
                            }
                        }

                        if (settings != null) {
                            // Display the Wi-Fi setting dialog.
                            WifiSettingDialogFragment dialog = WifiSettingDialogFragment.newInstance(this, settings.getString(KEY));
                            dialog.show(getActivity().getSupportFragmentManager(), name);
                        }
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case DATETIME: {
                    // Display the time setting dialog.
                    DateTimeSettingDialogFragment dialog = DateTimeSettingDialogFragment.newInstance(this);
                    dialog.show(getActivity().getSupportFragmentManager(), name);
                    break;
                }
                case CARD_FORMAT: {
                    // Display the check dialog before executing card format.
                    // The callback of pressing the OK button receives with the onActivityResult().
                    String storageName = itemsMap.get(LIST_VIEW_KEY_NAME);
                    Bundle args = new Bundle();
                    args.putString(Constants.RequestCode.POST_FUNCTIONS_CARDFORMAT.name(), storageName);
                    MessageDialogFragment dialog = MessageDialogFragment.newInstance(CARD_FORMAT, "Execute?", args, this);
                    dialog.show(getActivity().getSupportFragmentManager(), CARD_FORMAT);
                    break;
                }
                default:
                    try {
                        for (int i = 0; i < mSettingsInfo.length(); i++) {
                            if (name.equals(mSettingsInfo.getJSONObject(i).getString(NAME))) {
                                settings = mSettingsInfo.getJSONObject(i);
                                break;
                            }
                        }

                        if (settings != null) {
                            ListViewDialogFragment dialog = ListViewDialogFragment.newInstance(this, settings, settingsDataSet);
                            dialog.show(getActivity().getSupportFragmentManager(), name);
                        }
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }

        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            // Execution processing about card format.
            mProgressDialog = ProgressDialogFragment.newInstance(ProgressDialogFragment.Type.Circular, CARD_FORMAT);
            mProgressDialog.show(getActivity().getSupportFragmentManager(), CARD_FORMAT);
            Bundle args = data.getBundleExtra(Intent.EXTRA_RESTRICTIONS_BUNDLE);
            mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.POST_FUNCTIONS_CARDFORMAT, args, this));
        }
    }

    /**
     * Callback from execute WebAPI
     * @param result HTTP Request result
     */
    @Override
    public void onWebAPIResult(WebAPIResultDataSet result){
        Log.d(TAG, String.format("%s onWebAPIResult", String.valueOf(result.getRequestCode())));
        Context context = getActivity();

        // Do nothing, if the life cycle of the fragment is finished.
        if(context != null) {
            if(result.getRequestCode() == Constants.RequestCode.POST_FUNCTIONS_CARDFORMAT) {
                // Close the ProgressDialog, if it is displayed regardless of the execution result.
                if (mProgressDialog != null) {
                    mProgressDialog.dismissAllowingStateLoss();
                    mProgressDialog = null;
                }
            }

            if (result.isError()) {
                if(result.getMethod().equals(GET) && result.getResponseCode() == 503
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
                    case ACT_WEB_API: {

                        // Get a name of setting items.
                        String url = result.getUrl();
                        String name = "";
                        if(url.contains(WIFI_SETTINGS)){
                            String[] split = url.split(String.format("(?=%s)", WIFI_SETTINGS));
                            name = split[split.length - 1];
                        }
                        else if(url.contains(EXPOSURE_INCREMENTS)){
                            String[] split = url.split(String.format("(?=%s)", EXPOSURE_INCREMENTS));
                            name = split[split.length - 1];
                        }
                        else{
                            name = result.getRequestName();
                        }

                        // The callback processing after completion of acquisition of setting values when a Fragment is displayed.
                        if (result.getMethod().equals(GET)) {
                            updateListView(name, result.getResponseBody());
                        }

                        // The callback processing after completion of modification of the dialog setting value.
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

                                    // Reflect the values of generated data in current data.
                                    currentData.setValue(responseData.getValue());

                                    updateListViewData(name, currentData);

                                    mAdapter.notifyDataSetChanged();
                                }
                                catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        break;
                    }
                    case GET_DEVICESTATUS_STORAGE: {
                        // Generate data for format.
                        updateListViewCardFormat(result.getResponseBody());
                        break;
                    }
                    case POST_FUNCTIONS_CARDFORMAT: {
                        // The callback from the format execution.
                        Toast.makeText(context, "Format finished.", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    default:
                        break;
                }
            }
        }
        else{
            Log.d(TAG, String.format("[DEBUG] %s Activity is Null.", String.valueOf(result.getRequestCode())));
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
                    // Conversion from /exposureincrements/xxx to exposureincrements_xxx.
                    if(key.contains(EXPOSURE_INCREMENTS + "_")){
                        key = key.replace("_","/");
                        Log.d(TAG, "EXPOSURE_INCREMENTS: " + key);
                    }
                    // The name is displayed as the item, because the cardformat API is executed by sending the storage's name when using the POST method.
                    // Reflect the ListView when the storage is updated.
                    if(key.equals(STORAGE)){
                        updateListViewCardFormat(param);
                    }
                    updateListView(key, param);
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void getListViewSettings(){
        for (int i = 0; i < mSettingsInfo.length(); i++) {
            try {
                JSONObject object = mSettingsInfo.getJSONObject(i);
                String name = object.getString(NAME);
                String key = object.getString(KEY);

                switch (name) {
                    case CARD_FORMAT: {
                        APIDataSet api = mWebAPI.getAPIData(DEVICESTATUS_STORAGE);
                        if (api != null) {

                            createListView(name);

                            // Get storage information, because the format execution needs it.
                            mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.GET_DEVICESTATUS_STORAGE, null, this));
                        }
                        break;
                    }
                    default: {
                        APIDataSet api = mWebAPI.getAPIData(key);
                        if (api != null && api.isGetable()) {

                            createListView(name);

                            Bundle args = new Bundle();
                            String[] params = new String[]{GET, api.getUrl(), null};
                            args.putStringArray(Constants.RequestCode.ACT_WEB_API.name(), params);
                            mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.ACT_WEB_API, args, this));
                        }
                        break;
                    }
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void addListViewData(ListViewDataSet dataSet){
        Map<String, String> parent = new HashMap<>();
        List<Map<String, String>> childList = new ArrayList<>();

        parent.put(LIST_VIEW_KEY_GROUP, dataSet.getName());

        Map<String, String> child = new HashMap<>();
        child.put(LIST_VIEW_KEY_NAME, "");
        child.put(LIST_VIEW_KEY_VALUE, "");
        childList.add(child);

        mEditableList.add(false);
        mSettingsDataSetList.add(dataSet);
        mParentList.add(parent);
        mChildList.add(childList);
        mExpandableListView.expandGroup(mParentList.size() - 1);
    }

    private void updateListViewData(String name, ListViewDataSet dataSet){
        boolean isExist = false;
        int index = 0;
        List<Map<String, String>> childList = new ArrayList<>();

        if(dataSet.getItems().size() != 0) {
            for (String item : dataSet.getItems()) {
                Map<String, String> child = new HashMap<>();
                child.put(LIST_VIEW_KEY_NAME, item);
                child.put(LIST_VIEW_KEY_VALUE, dataSet.getValue().get(item));
                childList.add(child);
            }
        }

        Boolean editable = false;
        try {
            for(int i = 0; i < mSettingsInfo.length(); i++) {
                if (mSettingsInfo.getJSONObject(i).getString(NAME).equals(dataSet.getName())) {
                    APIDataSet api = mWebAPI.getAPIData(mSettingsInfo.getJSONObject(i).getString(KEY));
                    if( api != null ) {
                        if (api.isPutable() || api.isPostable()) {
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

        // Calculate an index.
        for (int i = 0; i < mSettingsDataSetList.size(); i++) {
            if (mSettingsDataSetList.get(i).getName().equals(dataSet.getName())) {
                isExist = true;
                index = i;
                break;
            }
        }

        if (isExist){
            mEditableList.set(index, editable);
            mSettingsDataSetList.set(index, dataSet);
            mChildList.set(index, childList);
        }
        else{
            if(name.equals(STORAGE)) {
                for (int i = 0; i < mSettingsDataSetList.size(); i++) {
                    if (mSettingsDataSetList.get(i).getName().equals(STORAGE)) {
                        isExist = true;
                        index = i;
                        break;
                    }
                }

                Map<String, String> parent = new HashMap<>();
                parent.put(LIST_VIEW_KEY_GROUP, dataSet.getName());

                if(isExist){
                    mEditableList.set(index, editable);
                    mSettingsDataSetList.set(index, dataSet);
                    mParentList.set(index, parent);
                    mChildList.set(index, childList);
                }
                else{
                    mEditableList.add(editable);
                    mSettingsDataSetList.add(dataSet);
                    mParentList.add(parent);
                    mChildList.add(childList);
                    mExpandableListView.expandGroup(mParentList.size() - 1);
                }
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
            if (name.equals(STORAGE)) {
                JSONArray jsonArray = response.getJSONArray(STORAGE_LIST);
                if(jsonArray.length() == 1) {
                    ListViewDataSet listViewDataSet = new ListViewDataSet(name, jsonArray.getJSONObject(0));
                    updateListViewData(name, listViewDataSet);
                }
                else{
                    for (int i = 0; i < jsonArray.length(); i++) {
                        ListViewDataSet listViewDataSet = new ListViewDataSet(STORAGE + (i + 1), jsonArray.getJSONObject(i));
                        updateListViewData(name, listViewDataSet);
                    }
                }
            }
            else {
                ListViewDataSet listViewDataSet = new ListViewDataSet(name, response);
                updateListViewData(name, listViewDataSet);
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        mAdapter.notifyDataSetChanged();
    }

    private void updateListViewCardFormat(String responseBody){
        ListViewDataSet dataSet = new ListViewDataSet();
        dataSet.setName(CARD_FORMAT);
        try {
            Map<String, String> valueMap = new HashMap<>();
            List<String> itemList = new ArrayList<>();

            JSONObject jsonObject = new JSONObject(responseBody);
            JSONArray jsonArray = jsonObject.getJSONArray(STORAGE_LIST);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject storage = jsonArray.getJSONObject(i);
                if (!storage.isNull(STORAGE_NAME)){
                    itemList.add(storage.getString(STORAGE_NAME));
                    valueMap.put(storage.getString(STORAGE_NAME), "Tap to execute.");
                }
            }
            dataSet.setValue(valueMap);
            dataSet.setItems(itemList);

            if(itemList.size() > 0) {
                updateListViewData(CARD_FORMAT, dataSet);
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
