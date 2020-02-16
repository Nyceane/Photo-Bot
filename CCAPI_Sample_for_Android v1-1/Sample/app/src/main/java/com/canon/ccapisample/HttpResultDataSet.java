package com.canon.ccapisample;

import java.io.UnsupportedEncodingException;
import java.util.Map;

class HttpResultDataSet {
    private int mResponseCode;
    private String mResponseMessage;
    private Map<String, String> mResponseHeaderMap;
    private byte[] mBytesResponseBody;
    private Boolean mIsCancel;

    HttpResultDataSet(int responseCode, String responseMessage, Map<String, String> responseHeaderMap, byte[] bytesResponseBody, Boolean isCancel){
        this.mResponseCode = responseCode;
        this.mResponseMessage = responseMessage;
        this.mResponseHeaderMap = responseHeaderMap;
        this.mBytesResponseBody = bytesResponseBody;
        this.mIsCancel = isCancel;
    }

    HttpResultDataSet(int responseCode, String responseMessage, Map<String, String> responseHeaderMap, byte[] bytesResponseBody){
        this.mResponseCode = responseCode;
        this.mResponseMessage = responseMessage;
        this.mResponseHeaderMap = responseHeaderMap;
        this.mBytesResponseBody = bytesResponseBody;
        this.mIsCancel = false;
    }

    int getResponseCode() {
        return mResponseCode;
    }

    String getStringResponseBody() {
        String body = "";
        if(mBytesResponseBody != null) {
            try {
                body = new String(mBytesResponseBody, "UTF-8");
            }
            catch (UnsupportedEncodingException | OutOfMemoryError e) {
                e.printStackTrace();
            }
        }
        return body;
    }

    byte[] getBytesResponseBody() {
        return mBytesResponseBody;
    }

    Map<String, String> getResponseHeaderMap() {
        return mResponseHeaderMap;
    }

    Boolean getCancel() {
        return mIsCancel;
    }
}
