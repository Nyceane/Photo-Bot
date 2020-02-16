package com.canon.ccapisample;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;

interface AuthenticateListener {
    void showAuthDialog(DialogFragment dialogFragment, DialogInterface.OnDismissListener onDismissListener);
}
