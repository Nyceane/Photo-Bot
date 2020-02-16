package com.canon.ccapisample;

interface HttpProgressListener {
    void onHttpProgressing(int max, int progress, byte[] progressBytes);
}
