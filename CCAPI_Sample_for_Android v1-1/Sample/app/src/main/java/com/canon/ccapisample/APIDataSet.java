package com.canon.ccapisample;

import org.json.JSONException;
import org.json.JSONObject;

class APIDataSet {
    private String mPath = "";
    private String mUrl = "";
    private String mKey = "";
    private boolean mGetable = false;
    private boolean mPostable = false;
    private boolean mPutable = false;
    private boolean mDeletable = false;

    APIDataSet(String baseUrl, String version, JSONObject api){
        try {
            if(api.has("path")){
                this.mPath = api.getString("path");
                this.mUrl = baseUrl + this.mPath.replace("/ccapi","");
            }else{
                this.mUrl = api.getString("url");
            }
            this.mKey = this.mUrl.replace(baseUrl + "/" + version + "/", "");
            this.mGetable = api.getBoolean("get");
            this.mPostable = api.getBoolean("post");
            this.mPutable = api.getBoolean("put");
            this.mDeletable = api.getBoolean("delete");
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getUrl() {
        return mUrl;
    }

    public String getKey() {
        return mKey;
    }

    public boolean isGetable() {
        return mGetable;
    }

    public boolean isPostable() {
        return mPostable;
    }

    public boolean isPutable() {
        return mPutable;
    }

    public boolean isDeletable() {
        return mDeletable;
    }
}
