package com.canon.ccapisample;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.canon.ccapisample.Constants.CCAPI.Field.ABILITY;
import static com.canon.ccapisample.Constants.CCAPI.Field.MAX;
import static com.canon.ccapisample.Constants.CCAPI.Field.MIN;
import static com.canon.ccapisample.Constants.CCAPI.Field.STEP;
import static com.canon.ccapisample.Constants.CCAPI.Field.VALUE;

class ListViewDataSet implements Serializable{
    enum DataType {
        STRING,
        ENUM,
        RANGE,
        NONE
    }

    private static final String TAG = ListViewDataSet.class.getSimpleName();
    private String mName = "";
    private Map<String, String> mValue = new HashMap<>();
    private Map<String, List<String>> mEnumAbility = new HashMap<>();
    private Map<String, Map<String, Integer>> mRangeAbility = new HashMap<>();
    private List<String> mItems = new ArrayList<>();
    private Map<String, DataType> mDataType = new HashMap<>();

    ListViewDataSet(){}

    ListViewDataSet(String name, JSONObject response){
        Log.d(TAG, "createListViewData: " + name);
        mName = name;
        try {
            if (!response.isNull(VALUE)){
                Object value = response.get(VALUE);
                parseJsonValue(name, value);

                if(!response.isNull(ABILITY)){
                    Object ability = response.get(ABILITY);

                    if(ability instanceof JSONArray){
                        parseArrayAbility(name, (JSONArray)ability);
                    }
                    else if(ability instanceof JSONObject){
                        parseJsonAbility(name, (JSONObject)ability);
                    }
                    else{
                        Log.d(TAG, "Unknown ability format. name = " + name);
                    }
                }
            }
            else if(!response.isNull(ABILITY)){
                Log.d(TAG, "Ability only. name = " + name);
            }
            else {
                parseJsonOther(response);
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        Collections.sort(mItems);
    }

    private void parseJsonValue(String name, Object value) throws JSONException{
        if (value instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) value;
            Iterator<String> keys = jsonObject.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                Object object = jsonObject.get(key);

                mValue.put(key, object.toString());
                mItems.add(key);
            }
        }
        else {
            mValue.put(name, value.toString());
            mItems.add(name);
        }
    }

    private void parseArrayAbility(String name, JSONArray jsonArrayAbility)  throws JSONException{
        if (jsonArrayAbility.length() > 0) {
            if (jsonArrayAbility.get(0) instanceof String) {
                List<String> list = new ArrayList<>();
                for (int i = 0; i < jsonArrayAbility.length(); i++) {
                    list.add(jsonArrayAbility.getString(i));
                }
                mEnumAbility.put(name, list);
                mDataType.put(name, DataType.ENUM);
            }
            else if (jsonArrayAbility.get(0) instanceof JSONObject) {
                for(String item : mItems) {
                    mEnumAbility.put(item, new ArrayList<String>());
                    mDataType.put(item, DataType.ENUM);

                    for (int i = 0; i < jsonArrayAbility.length(); i++) {
                        Object object = jsonArrayAbility.getJSONObject(i).get(item);
                        List<String> list = mEnumAbility.get(item);
                        String str = object.toString();
                        if (!list.contains(str)) {
                            list.add(str);
                        }
                    }
                }
            }
            else {
                Log.d(TAG, "ability : Unknown Format.");
                mDataType.put(name, DataType.NONE);
            }
        }
        else{
            Log.d(TAG, "ability : Array is void. " + name);
            mDataType.put(name, DataType.NONE);
        }
    }

    private void parseJsonAbility(String name, JSONObject ability) throws JSONException{
        if(!ability.isNull(MIN) && !ability.isNull(MAX) && !ability.isNull(STEP) ) {
            Map<String, Integer> range = new HashMap<>();
            range.put(MIN, ability.getInt(MIN));
            range.put(MAX, ability.getInt(MAX));
            range.put(STEP, ability.getInt(STEP));

            mRangeAbility.put(name, range);
            mDataType.put(name, DataType.RANGE);
        }
        else{
            for(String item : mItems) {
                if(!ability.isNull(item)) {
                    Object object = ability.get(item);
                    if (object instanceof JSONObject) {
                        parseJsonAbility(item, (JSONObject) object);
                    }
                    else if (object instanceof JSONArray) {
                        parseArrayAbility(item, (JSONArray) object);
                    }
                    else {
                        Log.d(TAG, String.format("ability : Unknown Format. %s / %s", name, item));
                        mDataType.put(name, DataType.NONE);
                    }
                }
                else{
                    Log.d(TAG, String.format("ability : Unknown Format. %s / %s", name, item));
                    mDataType.put(name, DataType.NONE);
                    break;
                }
            }
        }
    }

    private void parseJsonOther(JSONObject response) throws JSONException{
        Iterator<String> iterator = response.keys();

        while (iterator.hasNext()) {
            String key = iterator.next();
            Object param = response.get(key);

            mValue.put(key, param.toString());
            mItems.add(key);
            mDataType.put(key, DataType.STRING);
        }
    }

    String getName() {
        return mName;
    }

    void setName(String name) {
        mName = name;
    }

    Map<String, String> getValue() {
        return mValue;
    }

    void setValue(Map<String, String> value) {
        mValue = value;
    }

    Map<String, List<String>> getEnumAbility() {
        return mEnumAbility;
    }

    Map<String, Map<String, Integer>> getRangeAbility() {
        return mRangeAbility;
    }

    List<String> getItems() {
        return mItems;
    }

    void setItems(List<String> items) {
        mItems = items;
    }

    Map<String, DataType> getDataType() {
        return mDataType;
    }

    boolean isSettable(){
        boolean ret = false;

        if(mItems.size() != 0){
            // When setting values have been got.
            if(mEnumAbility.size() != 0 || mRangeAbility.size() != 0) {
                // It can be changed, if abilities have been got.
                ret = true;
            }
            else{
                // When abilities have not been got.
                for (String item : mItems) {
                    if(mDataType.containsKey(item) && mDataType.get(item) == DataType.STRING){
                        // String type can be changed without getting abilities.
                        ret = true;
                    }
                    else{
                        // Except for string type can not be changed without getting abilities.
                        ret = false;
                        break;
                    }
                }
            }
        }
        else{
            // It can not be changed, when setting values have not been got.
            ret = false;
        }

        return ret;
    }
}
