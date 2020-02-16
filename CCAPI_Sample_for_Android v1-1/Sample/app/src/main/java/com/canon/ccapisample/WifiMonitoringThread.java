package com.canon.ccapisample;

import android.util.Log;

import java.io.IOException;

class WifiMonitoringThread extends Thread {
    private static final String TAG = WifiMonitoringThread.class.getSimpleName();
    private String mAddress;
    private DisconnectListener mDisconnectListener;

    WifiMonitoringThread(String address, DisconnectListener disconnectListener){
        mAddress = address;
        mDisconnectListener = disconnectListener;
    }

    @Override
    public void run() {
        Log.d(TAG, String.format("WifiMonitoringThread(%d) begin.", this.getId()));

        while(!isInterrupted()) {
            Log.d(TAG, String.format("WifiMonitoringThread(%d) : Ping send.", this.getId()));
            Constants.WifiMonitoringResult ret = confirmationByPing(mAddress);

            if(ret == Constants.WifiMonitoringResult.DISCONNECTION){
                Log.d(TAG, String.format("WifiMonitoringThread(%d) : Ping cannot receive.", this.getId()));
                mDisconnectListener.onNotifyDisconnect("Wifi Disconnected.", true);
                break;
            }
            else if(ret == Constants.WifiMonitoringResult.INTERRUPT){
                Log.d(TAG, String.format("WifiMonitoringThread(%d) : Ping receiving is interrupted.", this.getId()));
                break;
            }

            try {
                synchronized (this) {
                    this.wait(3000);
                }
            }
            catch (InterruptedException e) {
                e.printStackTrace();
                Log.d(TAG, String.format("WifiMonitoringThread(%d) : waiting interrupted.", this.getId()));
                break;
            }
        }

        Log.d(TAG, String.format("WifiMonitoringThread(%d) end.", this.getId()));
    }

    private Constants.WifiMonitoringResult confirmationByPing(String address){
        Runtime runtime = Runtime.getRuntime();
        Process process = null;
        int exitVal = 0;
        Constants.WifiMonitoringResult ret = Constants.WifiMonitoringResult.DISCONNECTION;

        try {
            process = runtime.exec("ping -c 5 " + address);
            process.waitFor();

            exitVal = process.exitValue();
            if(exitVal == 0){
                ret = Constants.WifiMonitoringResult.CONNECTION;
            }
        }
        catch (IOException | IllegalThreadStateException e) {
            e.printStackTrace();
        }
        catch (InterruptedException e){
            e.printStackTrace();
            ret = Constants.WifiMonitoringResult.INTERRUPT;
        }

        return ret;
    }
}
